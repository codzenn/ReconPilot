package com.reconcileguard;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public class Main {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ERROR_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Logger LOG = Logger.getLogger("reconcileguard");

    private static AppConfig config;
    private static DataStore store;
    private static RateLimiter rateLimiter;

    public static void main(String[] args) throws Exception {
        config = AppConfig.load();
        configureLogging(config);
        store = DataStore.open(config.dataDir);
        rateLimiter = new RateLimiter(config.rateLimitPerMinute);

        ReconService reconService = new ReconService(store);
        if (store.cases().isEmpty()) {
            reconService.run("system");
        }

        var pool = Executors.newFixedThreadPool(config.workerThreads);
        try (ServerSocket server = new ServerSocket()) {
            server.bind(new InetSocketAddress(config.host, config.port));
            LOG.info("ReconcileGuard running at http://" + config.host + ":" + config.port);
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> handle(socket, reconService));
            }
        }
    }

    private static void handle(Socket socket, ReconService reconService) {
        String requestId = UUID.randomUUID().toString();
        try (socket) {
            Request request = parse(socket.getInputStream(), requestId);
            Response response = route(request, reconService);
            write(socket.getOutputStream(), response);
            LOG.info(request.method + " " + request.path + " " + response.status + " requestId=" + requestId);
        } catch (AppException ex) {
            writeQuietly(socket, errorResponse(ex.status, ex.message, ex.path, requestId));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Unhandled request failure requestId=" + requestId, ex);
            writeQuietly(socket, errorResponse(500, "Internal server error", "", requestId));
        }
    }

    private static Response route(Request request, ReconService reconService) throws IOException {
        if (request.path.startsWith("/api/")) {
            return routeApi(request, reconService);
        }
        return serveStatic(request.path, request.requestId);
    }

    private static Response routeApi(Request request, ReconService reconService) {
        if ("GET".equals(request.method) && "/api/health".equals(request.path)) {
            return jsonResponse(200, request.requestId, healthJson());
        }

        AuthResult auth = authenticate(request);
        if (!auth.allowed) {
            return errorResponse(401, "Missing or invalid bearer token", request.path, request.requestId);
        }
        if (!rateLimiter.allow(auth.key)) {
            return errorResponse(429, "Rate limit exceeded", request.path, request.requestId);
        }

        String method = request.method;
        String path = request.path;

        if ("GET".equals(method) && "/api/summary".equals(path)) {
            return jsonResponse(200, request.requestId, summaryJson());
        }
        if ("GET".equals(method) && "/api/transactions".equals(path)) {
            return jsonResponse(200, request.requestId, transactionsJson(query(request)));
        }
        if ("GET".equals(method) && "/api/cases".equals(path)) {
            return jsonResponse(200, request.requestId, casesJson(store.cases()));
        }
        if ("GET".equals(method) && path.startsWith("/api/cases/")) {
            String id = path.substring("/api/cases/".length());
            if (!id.matches("RC-[A-Za-z0-9-]+")) {
                return errorResponse(400, "Invalid case id", path, request.requestId);
            }
            Optional<ReconCase> found = store.findCase(id);
            return found.map(reconCase -> jsonResponse(200, request.requestId, reconCase.toJson(store)))
                    .orElseGet(() -> errorResponse(404, "Case not found", path, request.requestId));
        }
        if ("POST".equals(method) && "/api/reconcile/run".equals(path)) {
            Optional<String> operator = requireOperator(request);
            if (operator.isEmpty()) {
                return errorResponse(400, "Missing X-Operator-Id header", path, request.requestId);
            }
            int before = store.cases().size();
            reconService.run(operator.get());
            return jsonResponse(200, request.requestId, "{\"message\":\"Reconciliation completed\",\"openCases\":"
                    + store.openCaseCount() + ",\"newOrUpdatedCases\":" + Math.max(0, store.cases().size() - before) + "}");
        }
        if ("POST".equals(method) && path.startsWith("/api/cases/") && path.endsWith("/resolve")) {
            Optional<String> operator = requireOperator(request);
            if (operator.isEmpty()) {
                return errorResponse(400, "Missing X-Operator-Id header", path, request.requestId);
            }
            String id = path.substring("/api/cases/".length(), path.length() - "/resolve".length());
            if (!id.matches("RC-[A-Za-z0-9-]+")) {
                return errorResponse(400, "Invalid case id", path, request.requestId);
            }
            String note = field(request.body, "resolutionNote").orElse("Closed after branch confirmation").trim();
            if (note.length() < 5 || note.length() > 300) {
                return errorResponse(400, "Resolution note must be between 5 and 300 characters", path, request.requestId);
            }
            Optional<ReconCase> resolved = reconService.resolve(id, operator.get(), note);
            return resolved.map(reconCase -> jsonResponse(200, request.requestId, reconCase.toJson(store)))
                    .orElseGet(() -> errorResponse(404, "Case not found", path, request.requestId));
        }
        if ("GET".equals(method) && "/api/audit".equals(path)) {
            return jsonResponse(200, request.requestId, auditJson());
        }
        return errorResponse(404, "Unknown endpoint", path, request.requestId);
    }

    private static AuthResult authenticate(Request request) {
        String token = request.headers.getOrDefault("authorization", "");
        String expected = "Bearer " + config.apiToken;
        return new AuthResult(token.equals(expected), token.isBlank() ? request.remoteAddress : token);
    }

    private static Optional<String> requireOperator(Request request) {
        String operator = request.headers.get("x-operator-id");
        if (operator == null || operator.isBlank()) return Optional.empty();
        operator = operator.trim();
        if (!operator.matches("[A-Za-z0-9._@-]{3,80}")) return Optional.empty();
        return Optional.of(operator);
    }

    private static String healthJson() {
        return "{"
                + "\"status\":\"UP\","
                + "\"storage\":\"" + json(config.dataDir.toString()) + "\","
                + "\"transactions\":" + store.transactions().size() + ","
                + "\"cases\":" + store.cases().size() + ","
                + "\"auditEvents\":" + store.audit().size()
                + "}";
    }

    private static String summaryJson() {
        List<ReconCase> cases = store.cases();
        long open = cases.stream().filter(c -> "OPEN".equals(c.status)).count();
        long critical = cases.stream().filter(c -> "CRITICAL".equals(c.severity) && "OPEN".equals(c.status)).count();
        long resolved = cases.stream().filter(c -> "RESOLVED".equals(c.status)).count();
        double valueAtRisk = cases.stream()
                .filter(c -> !"RESOLVED".equals(c.status))
                .map(store::txnForCase)
                .flatMap(Optional::stream)
                .mapToDouble(t -> t.amount)
                .sum();
        Map<String, Long> channelCounts = cases.stream()
                .filter(c -> !"RESOLVED".equals(c.status))
                .map(store::txnForCase)
                .flatMap(Optional::stream)
                .collect(Collectors.groupingBy(t -> t.channel, LinkedHashMap::new, Collectors.counting()));
        return "{"
                + "\"totalTransactions\":" + store.transactions().size() + ","
                + "\"openCases\":" + open + ","
                + "\"criticalCases\":" + critical + ","
                + "\"resolvedCases\":" + resolved + ","
                + "\"valueAtRisk\":" + String.format(Locale.US, "%.2f", valueAtRisk) + ","
                + "\"channels\":" + mapJson(channelCounts)
                + "}";
    }

    private static String transactionsJson(Map<String, String> query) {
        String q = query.getOrDefault("q", "").toLowerCase(Locale.ROOT);
        String channel = query.getOrDefault("channel", "ALL");
        String risk = query.getOrDefault("risk", "ALL");

        List<PaymentTxn> filtered = store.transactions().stream()
                .filter(t -> "ALL".equals(channel) || t.channel.equals(channel))
                .filter(t -> "ALL".equals(risk) || riskBand(t.riskScore).equals(risk))
                .filter(t -> q.isBlank() || (t.utr + t.customerName + t.branch + t.channel).toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparing((PaymentTxn t) -> t.riskScore).reversed())
                .toList();
        return "[" + filtered.stream().map(PaymentTxn::toJson).collect(Collectors.joining(",")) + "]";
    }

    private static String casesJson(List<ReconCase> values) {
        return "[" + values.stream()
                .sorted(Comparator.comparing((ReconCase c) -> severityRank(c.severity)).reversed()
                        .thenComparing(c -> c.createdAt))
                .map(c -> c.toJson(store))
                .collect(Collectors.joining(",")) + "]";
    }

    private static String auditJson() {
        return "[" + store.audit().stream()
                .sorted(Comparator.comparing((AuditEvent e) -> e.timestamp).reversed())
                .map(AuditEvent::toJson)
                .collect(Collectors.joining(",")) + "]";
    }

    private static Response serveStatic(String path, String requestId) throws IOException {
        if (path.equals("/")) path = "/index.html";
        Path webRoot = Path.of("web").toAbsolutePath().normalize();
        Path target = webRoot.resolve(path.substring(1)).normalize();
        if (!target.startsWith(webRoot) || !Files.exists(target) || Files.isDirectory(target)) {
            return errorResponse(404, "Static asset not found", path, requestId);
        }
        return new Response(200, contentType(target), Files.readAllBytes(target), requestId);
    }

    private static String contentType(Path target) {
        String name = target.getFileName().toString();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        return "application/octet-stream";
    }

    private static Request parse(InputStream raw, String requestId) throws IOException {
        BufferedInputStream in = new BufferedInputStream(raw);
        String requestLine = readLine(in);
        if (requestLine == null || requestLine.isBlank()) {
            throw new AppException(400, "Empty request", "");
        }
        String[] start = requestLine.split(" ");
        if (start.length < 2) {
            throw new AppException(400, "Malformed request", "");
        }
        String method = start[0];
        String target = start[1];
        String path = target;
        String rawQuery = "";
        int queryStart = target.indexOf('?');
        if (queryStart >= 0) {
            path = target.substring(0, queryStart);
            rawQuery = target.substring(queryStart + 1);
        }
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = readLine(in)) != null && !line.isBlank()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                headers.put(line.substring(0, colon).trim().toLowerCase(Locale.ROOT),
                        line.substring(colon + 1).trim());
            }
        }
        int length = Integer.parseInt(headers.getOrDefault("content-length", "0"));
        if (length > config.maxRequestBytes) {
            throw new AppException(413, "Request body too large", decode(path));
        }
        byte[] body = in.readNBytes(length);
        return new Request(method, decode(path), rawQuery, headers, new String(body, StandardCharsets.UTF_8),
                requestId, headers.getOrDefault("x-forwarded-for", "direct"));
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = in.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                byte[] bytes = line.toByteArray();
                return new String(bytes, 0, Math.max(0, bytes.length - 1), StandardCharsets.UTF_8);
            }
            line.write(current);
            previous = current;
        }
        if (line.size() == 0) return null;
        return line.toString(StandardCharsets.UTF_8);
    }

    private static Map<String, String> query(Request request) {
        Map<String, String> map = new HashMap<>();
        if (request.rawQuery == null || request.rawQuery.isBlank()) return map;
        for (String part : request.rawQuery.split("&")) {
            String[] pair = part.split("=", 2);
            String key = decode(pair[0]);
            String value = pair.length > 1 ? decode(pair[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static Optional<String> field(String json, String name) {
        String needle = "\"" + name + "\"";
        int key = json.indexOf(needle);
        if (key < 0) return Optional.empty();
        int colon = json.indexOf(':', key);
        int start = json.indexOf('"', colon + 1);
        int end = json.indexOf('"', start + 1);
        if (colon < 0 || start < 0 || end < 0) return Optional.empty();
        return Optional.of(json.substring(start + 1, end));
    }

    private static Response jsonResponse(int status, String requestId, String body) {
        return textResponse(status, requestId, body, "application/json; charset=utf-8");
    }

    private static Response errorResponse(int status, String message, String path, String requestId) {
        String statusText = statusText(status);
        String body = "{"
                + "\"timestamp\":\"" + ERROR_TIME.format(LocalDateTime.now()) + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + json(statusText) + "\","
                + "\"message\":\"" + json(message) + "\","
                + "\"path\":\"" + json(path == null ? "" : path) + "\","
                + "\"requestId\":\"" + json(requestId) + "\""
                + "}";
        return jsonResponse(status, requestId, body);
    }

    private static Response textResponse(int status, String requestId, String body, String contentType) {
        return new Response(status, contentType, body.getBytes(StandardCharsets.UTF_8), requestId);
    }

    private static void writeQuietly(Socket socket, Response response) {
        try {
            write(socket.getOutputStream(), response);
        } catch (IOException ignored) {
        }
    }

    private static void write(OutputStream out, Response response) throws IOException {
        String headers = "HTTP/1.1 " + response.status + " " + statusText(response.status) + "\r\n"
                + "Content-Type: " + response.contentType + "\r\n"
                + "Content-Length: " + response.body.length + "\r\n"
                + "X-Request-Id: " + response.requestId + "\r\n"
                + "X-Content-Type-Options: nosniff\r\n"
                + "X-Frame-Options: DENY\r\n"
                + "Referrer-Policy: no-referrer\r\n"
                + "Cache-Control: no-store\r\n"
                + "Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:\r\n"
                + "Connection: close\r\n\r\n";
        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(response.body);
        out.flush();
    }

    private static String statusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 404 -> "Not Found";
            case 413 -> "Payload Too Large";
            case 429 -> "Too Many Requests";
            default -> "Internal Server Error";
        };
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private static String ownerFor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "Digital Payments Ops Lead";
            case "HIGH" -> "Reconciliation Analyst";
            default -> "Branch Support Queue";
        };
    }

    private static String riskBand(int score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 70) return "HIGH";
        if (score >= 50) return "MEDIUM";
        return "LOW";
    }

    private static String mapJson(Map<String, Long> map) {
        return "{" + map.entrySet().stream()
                .map(e -> "\"" + json(e.getKey()) + "\":" + e.getValue())
                .collect(Collectors.joining(",")) + "}";
    }

    private static String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\t", " ")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private static String now() {
        return LocalDateTime.now().format(TIME);
    }

    private static void configureLogging(AppConfig config) throws IOException {
        Files.createDirectories(config.logDir);
        LOG.setUseParentHandlers(true);
        FileHandler fileHandler = new FileHandler(config.logDir.resolve("reconcileguard.log").toString(), true);
        fileHandler.setFormatter(new SimpleFormatter());
        LOG.addHandler(fileHandler);
        LOG.setLevel(Level.INFO);
    }

    private record Request(String method, String path, String rawQuery, Map<String, String> headers, String body,
                           String requestId, String remoteAddress) {
    }

    private record Response(int status, String contentType, byte[] body, String requestId) {
    }

    private record AuthResult(boolean allowed, String key) {
    }

    private static final class AppException extends RuntimeException {
        private final int status;
        private final String message;
        private final String path;

        private AppException(int status, String message, String path) {
            super(message);
            this.status = status;
            this.message = message;
            this.path = path;
        }
    }

    private static final class AppConfig {
        private final String host;
        private final int port;
        private final int workerThreads;
        private final int maxRequestBytes;
        private final int rateLimitPerMinute;
        private final String apiToken;
        private final Path dataDir;
        private final Path logDir;

        private AppConfig(String host, int port, int workerThreads, int maxRequestBytes, int rateLimitPerMinute,
                          String apiToken, Path dataDir, Path logDir) {
            this.host = host;
            this.port = port;
            this.workerThreads = workerThreads;
            this.maxRequestBytes = maxRequestBytes;
            this.rateLimitPerMinute = rateLimitPerMinute;
            this.apiToken = apiToken;
            this.dataDir = dataDir;
            this.logDir = logDir;
        }

        private static AppConfig load() {
            Path data = Path.of(get("RG_DATA_DIR", "data"));
            return new AppConfig(
                    get("RG_HOST", "127.0.0.1"),
                    integer("RG_PORT", 8080),
                    integer("RG_WORKERS", 8),
                    integer("RG_MAX_REQUEST_BYTES", 16_384),
                    integer("RG_RATE_LIMIT_PER_MINUTE", 120),
                    get("RG_API_TOKEN", "dev-token-change-me"),
                    data,
                    Path.of(get("RG_LOG_DIR", data.resolve("logs").toString()))
            );
        }

        private static String get(String key, String fallback) {
            String value = System.getenv(key);
            return value == null || value.isBlank() ? fallback : value.trim();
        }

        private static int integer(String key, int fallback) {
            try {
                return Integer.parseInt(get(key, String.valueOf(fallback)));
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }
    }

    private static final class RateLimiter {
        private final int limit;
        private final Map<String, ArrayDeque<Long>> buckets = new ConcurrentHashMap<>();

        private RateLimiter(int limit) {
            this.limit = limit;
        }

        private boolean allow(String key) {
            long now = System.currentTimeMillis();
            long floor = now - 60_000;
            ArrayDeque<Long> bucket = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
            synchronized (bucket) {
                while (!bucket.isEmpty() && bucket.peekFirst() < floor) {
                    bucket.removeFirst();
                }
                if (bucket.size() >= limit) return false;
                bucket.addLast(now);
                return true;
            }
        }
    }

    private static final class ReconService {
        private final DataStore store;

        private ReconService(DataStore store) {
            this.store = store;
        }

        private synchronized void run(String operator) {
            Map<String, ReconCase> existing = store.cases().stream()
                    .collect(Collectors.toMap(c -> c.transactionId + "|" + c.issueType, c -> c, (a, b) -> a));
            Set<String> duplicateUtrs = duplicateUtrs(store.transactions());

            for (PaymentTxn txn : store.transactions()) {
                for (DetectedIssue issue : detect(txn, duplicateUtrs)) {
                    String key = txn.id + "|" + issue.type;
                    ReconCase reconCase = existing.get(key);
                    if (reconCase == null) {
                        reconCase = new ReconCase("RC-" + (1000 + store.cases().size() + 1), txn.id, issue.type,
                                issue.severity, "OPEN", issue.slaHours, issue.rootCause, issue.action,
                                ownerFor(issue.severity), now(), now(), "");
                        store.upsertCase(reconCase);
                        store.addAudit(new AuditEvent(now(), operator, "CASE_CREATED", reconCase.id,
                                issue.type + " detected for " + txn.utr));
                    } else if (!"RESOLVED".equals(reconCase.status)) {
                        reconCase.rootCause = issue.rootCause;
                        reconCase.recommendedAction = issue.action;
                        reconCase.updatedAt = now();
                        store.upsertCase(reconCase);
                        store.addAudit(new AuditEvent(now(), operator, "CASE_REFRESHED", reconCase.id,
                                "Issue still open after reconciliation run"));
                    }
                }
            }
            store.persist();
        }

        private synchronized Optional<ReconCase> resolve(String id, String operator, String note) {
            Optional<ReconCase> found = store.findCase(id);
            found.ifPresent(c -> {
                c.status = "RESOLVED";
                c.resolutionNote = note;
                c.updatedAt = now();
                store.upsertCase(c);
                store.addAudit(new AuditEvent(now(), operator, "CASE_RESOLVED", id, note));
                store.persist();
            });
            return found;
        }

        private static List<DetectedIssue> detect(PaymentTxn txn, Set<String> duplicateUtrs) {
            List<DetectedIssue> issues = new ArrayList<>();
            if ("SUCCESS".equals(txn.cbsStatus) && List.of("FAILED", "REVERSED").contains(txn.switchStatus)) {
                issues.add(new DetectedIssue("CUSTOMER_DEBITED_PAYMENT_FAILED", "CRITICAL", 4,
                        "Customer debit completed in CBS but switch did not settle successfully.",
                        "Trigger auto-reversal, notify branch, and send customer-safe status update."));
            }
            if (List.of("PENDING", "TIMEOUT").contains(txn.cbsStatus) && "SUCCESS".equals(txn.switchStatus)) {
                issues.add(new DetectedIssue("BENEFICIARY_CREDIT_PENDING", "HIGH", 8,
                        "Switch shows success but CBS posting is not complete.",
                        "Verify ledger posting, replay callback, and mark beneficiary credit confirmation."));
            }
            if ("TIMEOUT".equals(txn.gatewayStatus) && txn.ageHours > 2) {
                issues.add(new DetectedIssue("GATEWAY_TIMEOUT_WITHOUT_FINAL_STATUS", "MEDIUM", 12,
                        "Payment gateway timed out and final status callback is missing.",
                        "Pull latest gateway status, retry reconciliation, and keep complaint watch active."));
            }
            if (duplicateUtrs.contains(txn.utr)) {
                issues.add(new DetectedIssue("DUPLICATE_UTR_REFERENCE", "HIGH", 6,
                        "Same UTR/reference appears on multiple transactions.",
                        "Block duplicate settlement posting and route to maker-checker verification."));
            }
            if (txn.customerComplaint && txn.ageHours > 24 && !"SUCCESS".equals(txn.cbsStatus)) {
                issues.add(new DetectedIssue("SLA_BREACH_RISK", "CRITICAL", 2,
                        "Customer complaint is open beyond operational SLA and transaction is unresolved.",
                        "Escalate to operations lead and branch support with RCA summary."));
            }
            return issues;
        }

        private static Set<String> duplicateUtrs(List<PaymentTxn> txns) {
            Set<String> seen = new HashSet<>();
            Set<String> duplicate = new HashSet<>();
            for (PaymentTxn txn : txns) {
                if (!seen.add(txn.utr)) duplicate.add(txn.utr);
            }
            return duplicate;
        }
    }

    private static final class DataStore {
        private final Path dataDir;
        private final List<PaymentTxn> transactions = new ArrayList<>();
        private final List<ReconCase> cases = new ArrayList<>();
        private final List<AuditEvent> audit = new ArrayList<>();

        private DataStore(Path dataDir) {
            this.dataDir = dataDir;
        }

        private static DataStore open(Path dataDir) throws IOException {
            Files.createDirectories(dataDir);
            DataStore store = new DataStore(dataDir);
            store.load();
            if (store.transactions.isEmpty()) {
                store.transactions.addAll(seedTransactions());
                store.persistTransactions();
            }
            return store;
        }

        private synchronized List<PaymentTxn> transactions() {
            return new ArrayList<>(transactions);
        }

        private synchronized List<ReconCase> cases() {
            return new ArrayList<>(cases);
        }

        private synchronized List<AuditEvent> audit() {
            return new ArrayList<>(audit);
        }

        private synchronized Optional<ReconCase> findCase(String id) {
            return cases.stream().filter(c -> c.id.equals(id)).findFirst();
        }

        private synchronized Optional<PaymentTxn> txnForCase(ReconCase reconCase) {
            return transactions.stream().filter(t -> t.id.equals(reconCase.transactionId)).findFirst();
        }

        private synchronized long openCaseCount() {
            return cases.stream().filter(c -> "OPEN".equals(c.status)).count();
        }

        private synchronized void upsertCase(ReconCase reconCase) {
            cases.removeIf(c -> c.id.equals(reconCase.id));
            cases.add(reconCase);
        }

        private synchronized void addAudit(AuditEvent event) {
            audit.add(event);
        }

        private synchronized void load() throws IOException {
            loadTransactions();
            loadCases();
            loadAudit();
        }

        private synchronized void persist() {
            try {
                persistCases();
                persistAudit();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Failed to persist data store", ex);
                throw new AppException(500, "Failed to persist data store", "");
            }
        }

        private void loadTransactions() throws IOException {
            Path file = dataDir.resolve("transactions.tsv");
            if (!Files.exists(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                transactions.add(PaymentTxn.fromTsv(line));
            }
        }

        private void loadCases() throws IOException {
            Path file = dataDir.resolve("cases.tsv");
            if (!Files.exists(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                cases.add(ReconCase.fromTsv(line));
            }
        }

        private void loadAudit() throws IOException {
            Path file = dataDir.resolve("audit.tsv");
            if (!Files.exists(file)) return;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                audit.add(AuditEvent.fromTsv(line));
            }
        }

        private void persistTransactions() throws IOException {
            write(dataDir.resolve("transactions.tsv"), transactions.stream().map(PaymentTxn::toTsv).toList());
        }

        private void persistCases() throws IOException {
            write(dataDir.resolve("cases.tsv"), cases.stream().map(ReconCase::toTsv).toList());
        }

        private void persistAudit() throws IOException {
            write(dataDir.resolve("audit.tsv"), audit.stream().map(AuditEvent::toTsv).toList());
        }

        private void write(Path file, List<String> lines) throws IOException {
            Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private record PaymentTxn(String id, String channel, String utr, String customerName, String branch,
                              double amount, String initiatedAt, String cbsStatus, String switchStatus,
                              String gatewayStatus, boolean customerComplaint, int ageHours, int riskScore) {
        private String toJson() {
            return "{"
                    + "\"id\":\"" + json(id) + "\","
                    + "\"channel\":\"" + json(channel) + "\","
                    + "\"utr\":\"" + json(utr) + "\","
                    + "\"customerName\":\"" + json(customerName) + "\","
                    + "\"branch\":\"" + json(branch) + "\","
                    + "\"amount\":" + String.format(Locale.US, "%.2f", amount) + ","
                    + "\"initiatedAt\":\"" + json(initiatedAt) + "\","
                    + "\"cbsStatus\":\"" + json(cbsStatus) + "\","
                    + "\"switchStatus\":\"" + json(switchStatus) + "\","
                    + "\"gatewayStatus\":\"" + json(gatewayStatus) + "\","
                    + "\"customerComplaint\":" + customerComplaint + ","
                    + "\"ageHours\":" + ageHours + ","
                    + "\"riskScore\":" + riskScore + ","
                    + "\"riskBand\":\"" + riskBand(riskScore) + "\""
                    + "}";
        }

        private String toTsv() {
            return String.join("\t", id, channel, utr, clean(customerName), clean(branch), String.valueOf(amount),
                    initiatedAt, cbsStatus, switchStatus, gatewayStatus, String.valueOf(customerComplaint),
                    String.valueOf(ageHours), String.valueOf(riskScore));
        }

        private static PaymentTxn fromTsv(String line) {
            String[] p = line.split("\t", -1);
            return new PaymentTxn(p[0], p[1], p[2], p[3], p[4], Double.parseDouble(p[5]), p[6], p[7], p[8], p[9],
                    Boolean.parseBoolean(p[10]), Integer.parseInt(p[11]), Integer.parseInt(p[12]));
        }
    }

    private static final class ReconCase {
        private final String id;
        private final String transactionId;
        private final String issueType;
        private final String severity;
        private String status;
        private final int slaHours;
        private String rootCause;
        private String recommendedAction;
        private final String owner;
        private final String createdAt;
        private String updatedAt;
        private String resolutionNote;

        private ReconCase(String id, String transactionId, String issueType, String severity, String status,
                          int slaHours, String rootCause, String recommendedAction, String owner,
                          String createdAt, String updatedAt, String resolutionNote) {
            this.id = id;
            this.transactionId = transactionId;
            this.issueType = issueType;
            this.severity = severity;
            this.status = status;
            this.slaHours = slaHours;
            this.rootCause = rootCause;
            this.recommendedAction = recommendedAction;
            this.owner = owner;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.resolutionNote = resolutionNote;
        }

        private String toJson(DataStore store) {
            Optional<PaymentTxn> txn = store.txnForCase(this);
            return "{"
                    + "\"id\":\"" + json(id) + "\","
                    + "\"transactionId\":\"" + json(transactionId) + "\","
                    + "\"issueType\":\"" + json(issueType) + "\","
                    + "\"severity\":\"" + json(severity) + "\","
                    + "\"status\":\"" + json(status) + "\","
                    + "\"slaHours\":" + slaHours + ","
                    + "\"rootCause\":\"" + json(rootCause) + "\","
                    + "\"recommendedAction\":\"" + json(recommendedAction) + "\","
                    + "\"owner\":\"" + json(owner) + "\","
                    + "\"createdAt\":\"" + json(createdAt) + "\","
                    + "\"updatedAt\":\"" + json(updatedAt) + "\","
                    + "\"resolutionNote\":\"" + json(resolutionNote) + "\","
                    + "\"transaction\":" + txn.map(PaymentTxn::toJson).orElse("null")
                    + "}";
        }

        private String toTsv() {
            return String.join("\t", id, transactionId, issueType, severity, status, String.valueOf(slaHours),
                    clean(rootCause), clean(recommendedAction), clean(owner), createdAt, updatedAt, clean(resolutionNote));
        }

        private static ReconCase fromTsv(String line) {
            String[] p = line.split("\t", -1);
            return new ReconCase(p[0], p[1], p[2], p[3], p[4], Integer.parseInt(p[5]), p[6], p[7], p[8], p[9], p[10], p[11]);
        }
    }

    private record DetectedIssue(String type, String severity, int slaHours, String rootCause, String action) {
    }

    private record AuditEvent(String timestamp, String actor, String action, String reference, String details) {
        private String toJson() {
            return "{"
                    + "\"timestamp\":\"" + json(timestamp) + "\","
                    + "\"actor\":\"" + json(actor) + "\","
                    + "\"action\":\"" + json(action) + "\","
                    + "\"reference\":\"" + json(reference) + "\","
                    + "\"details\":\"" + json(details) + "\""
                    + "}";
        }

        private String toTsv() {
            return String.join("\t", timestamp, clean(actor), action, reference, clean(details));
        }

        private static AuditEvent fromTsv(String line) {
            String[] p = line.split("\t", -1);
            return new AuditEvent(p[0], p[1], p[2], p[3], p[4]);
        }
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.replace("\t", " ").replace("\r", " ").replace("\n", " ");
    }

    private static List<PaymentTxn> seedTransactions() {
        return List.of(
                new PaymentTxn("TXN-10001", "UPI", "SIBUPI260501", "Aditi Rao", "Ernakulam", 14250.00, "2026-05-23 09:12", "SUCCESS", "FAILED", "SUCCESS", true, 31, 94),
                new PaymentTxn("TXN-10002", "IMPS", "SIBIMP260502", "Manoj Nair", "Bengaluru", 72000.00, "2026-05-23 09:18", "PENDING", "SUCCESS", "SUCCESS", true, 18, 88),
                new PaymentTxn("TXN-10003", "NEFT", "SIBNFT260503", "Neha Thomas", "Thrissur", 185000.00, "2026-05-23 10:05", "SUCCESS", "SUCCESS", "SUCCESS", false, 4, 31),
                new PaymentTxn("TXN-10004", "UPI", "SIBUPI260504", "Rahul Menon", "Kochi", 9999.00, "2026-05-23 10:22", "TIMEOUT", "PENDING", "TIMEOUT", true, 7, 77),
                new PaymentTxn("TXN-10005", "CARD", "SIBCRD260505", "Farah Khan", "Chennai", 42500.00, "2026-05-23 11:04", "SUCCESS", "REVERSED", "SUCCESS", true, 28, 91),
                new PaymentTxn("TXN-10006", "IMPS", "SIBIMP260506", "Kiran Das", "Kozhikode", 6500.00, "2026-05-23 11:48", "SUCCESS", "SUCCESS", "SUCCESS", false, 2, 21),
                new PaymentTxn("TXN-10007", "NEFT", "SIBNFT260507", "Meera Iyer", "Mumbai", 252000.00, "2026-05-23 12:15", "PENDING", "SUCCESS", "SUCCESS", false, 14, 81),
                new PaymentTxn("TXN-10008", "UPI", "SIBUPI260508", "Anil George", "Ernakulam", 1250.00, "2026-05-23 12:43", "TIMEOUT", "PENDING", "TIMEOUT", false, 5, 64),
                new PaymentTxn("TXN-10009", "IMPS", "SIBIMP260502", "Pooja Shah", "Bengaluru", 72000.00, "2026-05-23 13:01", "SUCCESS", "SUCCESS", "SUCCESS", false, 1, 79),
                new PaymentTxn("TXN-10010", "UPI", "SIBUPI260510", "Vivek S", "Delhi", 5600.00, "2026-05-23 13:27", "SUCCESS", "FAILED", "SUCCESS", true, 30, 96),
                new PaymentTxn("TXN-10011", "NEFT", "SIBNFT260511", "Sneha Joseph", "Coimbatore", 84500.00, "2026-05-23 14:06", "SUCCESS", "SUCCESS", "SUCCESS", false, 1, 19),
                new PaymentTxn("TXN-10012", "CARD", "SIBCRD260512", "Ramesh Pillai", "Trivandrum", 22900.00, "2026-05-23 14:28", "PENDING", "SUCCESS", "SUCCESS", true, 26, 89)
        );
    }
}
