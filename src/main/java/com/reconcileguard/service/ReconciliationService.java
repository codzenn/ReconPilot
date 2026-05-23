package com.reconcileguard.service;

import com.reconcileguard.domain.AuditEvent;
import com.reconcileguard.domain.CaseStatus;
import com.reconcileguard.domain.IssueType;
import com.reconcileguard.domain.PaymentChannel;
import com.reconcileguard.domain.PaymentStatus;
import com.reconcileguard.domain.PaymentTransaction;
import com.reconcileguard.domain.ReconciliationCase;
import com.reconcileguard.domain.Severity;
import com.reconcileguard.dto.AuditEventResponse;
import com.reconcileguard.dto.PaymentTransactionResponse;
import com.reconcileguard.dto.ReconcileRunResponse;
import com.reconcileguard.dto.ReconciliationCaseResponse;
import com.reconcileguard.dto.ResolveCaseRequest;
import com.reconcileguard.dto.SummaryResponse;
import com.reconcileguard.exception.NotFoundException;
import com.reconcileguard.repository.AuditEventRepository;
import com.reconcileguard.repository.PaymentTransactionRepository;
import com.reconcileguard.repository.ReconciliationCaseRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReconciliationService {
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000");

    private final PaymentTransactionRepository transactionRepository;
    private final ReconciliationCaseRepository caseRepository;
    private final AuditEventRepository auditEventRepository;

    public ReconciliationService(
            PaymentTransactionRepository transactionRepository,
            ReconciliationCaseRepository caseRepository,
            AuditEventRepository auditEventRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.caseRepository = caseRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary() {
        List<ReconciliationCase> openCases = caseRepository.findByStatus(CaseStatus.OPEN);

        Map<String, BigDecimal> exposureByTransaction = new LinkedHashMap<>();
        Map<String, Long> channels = new LinkedHashMap<>();
        long criticalCases = 0;

        for (ReconciliationCase reconCase : openCases) {
            PaymentTransaction transaction = reconCase.getTransaction();
            exposureByTransaction.putIfAbsent(transaction.getId(), transaction.getAmount());
            channels.merge(transaction.getChannel().name(), 1L, Long::sum);
            if (reconCase.getSeverity() == Severity.CRITICAL) {
                criticalCases++;
            }
        }

        BigDecimal valueAtRisk = exposureByTransaction.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SummaryResponse(
                transactionRepository.count(),
                openCases.size(),
                criticalCases,
                valueAtRisk,
                channels
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> transactions(String q, String channel, String risk) {
        String search = normalize(q);
        PaymentChannel channelFilter = enumOrNull(PaymentChannel.class, channel);
        String riskFilter = normalize(risk);

        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "initiatedAt")).stream()
                .filter(transaction -> channelFilter == null || transaction.getChannel() == channelFilter)
                .filter(transaction -> !StringUtils.hasText(riskFilter) || "ALL".equals(riskFilter) || riskBand(transaction.getRiskScore()).equals(riskFilter))
                .filter(transaction -> matches(transaction, search))
                .map(PaymentTransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReconciliationCaseResponse> cases(String status) {
        CaseStatus statusFilter = enumOrNull(CaseStatus.class, status);
        return caseRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(reconCase -> statusFilter == null || reconCase.getStatus() == statusFilter)
                .map(ReconciliationCaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationCaseResponse caseById(String caseId) {
        return caseRepository.findById(caseId)
                .map(ReconciliationCaseResponse::from)
                .orElseThrow(() -> new NotFoundException("RCA case not found: " + caseId));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> auditTrail() {
        return auditEventRepository.findTop100ByOrderByEventTimeDesc().stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    @Transactional
    public ReconcileRunResponse runReconciliation(String actor) {
        List<PaymentTransaction> transactions = transactionRepository.findAll();
        Set<String> duplicateUtrs = Set.copyOf(transactionRepository.findDuplicateUtrs());
        LocalDateTime now = LocalDateTime.now();

        int opened = 0;
        int refreshed = 0;

        for (PaymentTransaction transaction : transactions) {
            for (CaseDraft draft : detect(transaction, duplicateUtrs, now)) {
                ReconciliationCase reconCase = caseRepository
                        .findByTransactionAndIssueType(transaction, draft.issueType())
                        .orElse(null);

                if (reconCase == null) {
                    reconCase = new ReconciliationCase(
                            caseId(transaction, draft.issueType()),
                            transaction,
                            draft.issueType(),
                            draft.severity(),
                            draft.slaHours(),
                            draft.rootCause(),
                            draft.recommendedAction(),
                            draft.ownerQueue()
                    );
                    caseRepository.save(reconCase);
                    opened++;
                } else if (reconCase.getStatus() == CaseStatus.OPEN) {
                    reconCase.refresh(
                            draft.severity(),
                            draft.slaHours(),
                            draft.rootCause(),
                            draft.recommendedAction(),
                            draft.ownerQueue()
                    );
                    refreshed++;
                }
            }
        }

        auditEventRepository.save(new AuditEvent(
                cleanActor(actor),
                "RECONCILIATION_RUN",
                "BATCH",
                opened + " opened, " + refreshed + " refreshed across " + transactions.size() + " transactions"
        ));

        long openCases = caseRepository.countByStatus(CaseStatus.OPEN);
        return new ReconcileRunResponse(
                transactions.size(),
                opened,
                refreshed,
                openCases,
                "Reconciliation completed: " + opened + " opened, " + refreshed + " refreshed."
        );
    }

    @Transactional
    public ReconciliationCaseResponse resolveCase(String caseId, ResolveCaseRequest request, String actor) {
        ReconciliationCase reconCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NotFoundException("RCA case not found: " + caseId));

        reconCase.resolve(request.resolutionNote());
        auditEventRepository.save(new AuditEvent(
                cleanActor(actor),
                "CASE_RESOLVED",
                caseId,
                request.resolutionNote()
        ));

        return ReconciliationCaseResponse.from(reconCase);
    }

    private List<CaseDraft> detect(PaymentTransaction transaction, Set<String> duplicateUtrs, LocalDateTime now) {
        List<CaseDraft> issues = new ArrayList<>();
        long ageHours = Math.max(0, Duration.between(transaction.getInitiatedAt(), now).toHours());

        if (transaction.getCbsStatus() == PaymentStatus.SUCCESS
                && (transaction.getSwitchStatus() == PaymentStatus.FAILED || transaction.getSwitchStatus() == PaymentStatus.REVERSED)) {
            issues.add(new CaseDraft(
                    IssueType.CUSTOMER_DEBITED_PAYMENT_FAILED,
                    transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0 || transaction.isCustomerComplaint() ? Severity.CRITICAL : Severity.HIGH,
                    4,
                    "CBS debit is successful, but switch settlement did not complete.",
                    "Trigger reversal validation, hold customer communication, and confirm switch settlement file.",
                    "Digital Payments Ops"
            ));
        }

        if ((transaction.getCbsStatus() == PaymentStatus.PENDING || transaction.getCbsStatus() == PaymentStatus.TIMEOUT)
                && transaction.getSwitchStatus() == PaymentStatus.SUCCESS) {
            issues.add(new CaseDraft(
                    IssueType.BENEFICIARY_CREDIT_PENDING,
                    transaction.getAmount().compareTo(HIGH_VALUE_THRESHOLD) >= 0 ? Severity.CRITICAL : Severity.HIGH,
                    8,
                    "Switch reports success, but CBS posting is still pending or timed out.",
                    "Reconcile CBS posting batch, verify beneficiary credit, and prepare customer status update.",
                    "CBS Reconciliation"
            ));
        }

        if (transaction.getGatewayStatus() == PaymentStatus.TIMEOUT && ageHours >= 2) {
            issues.add(new CaseDraft(
                    IssueType.GATEWAY_TIMEOUT_WITHOUT_FINAL_STATUS,
                    transaction.getRiskScore() >= 70 ? Severity.HIGH : Severity.MEDIUM,
                    12,
                    "Gateway timed out and no final callback was received within the monitoring window.",
                    "Fetch gateway final status, replay callback safely, and update payment status evidence.",
                    "Gateway Integrations"
            ));
        }

        if (duplicateUtrs.contains(transaction.getUtr())) {
            issues.add(new CaseDraft(
                    IssueType.DUPLICATE_UTR_REFERENCE,
                    Severity.HIGH,
                    6,
                    "Same UTR/reference appears on multiple payment records.",
                    "Freeze duplicate posting path, compare source feed checksums, and mark the valid transaction.",
                    "Fraud and Ops Review"
            ));
        }

        if (transaction.isCustomerComplaint()
                && ageHours > 24
                && transaction.getCbsStatus() != PaymentStatus.SUCCESS) {
            issues.add(new CaseDraft(
                    IssueType.SLA_BREACH_RISK,
                    Severity.CRITICAL,
                    2,
                    "Customer complaint remains unresolved beyond the operational SLA window.",
                    "Escalate to branch and payment operations manager with transaction evidence pack.",
                    "Customer Escalation Desk"
            ));
        }

        return issues;
    }

    private static boolean matches(PaymentTransaction transaction, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String haystack = String.join(" ",
                transaction.getId(),
                transaction.getUtr(),
                transaction.getCustomerName(),
                transaction.getBranch(),
                transaction.getChannel().name()
        ).toUpperCase(Locale.ROOT);
        return haystack.contains(search);
    }

    private static String caseId(PaymentTransaction transaction, IssueType issueType) {
        String raw = "RC-" + transaction.getId() + "-" + String.format("%02d", issueType.ordinal() + 1);
        return raw.length() <= 40 ? raw : raw.substring(0, 40);
    }

    private static String cleanActor(String actor) {
        return StringUtils.hasText(actor) ? actor.trim() : "system";
    }

    private static String riskBand(int score) {
        if (score >= 85) return "CRITICAL";
        if (score >= 70) return "HIGH";
        if (score >= 50) return "MEDIUM";
        return "LOW";
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized) || "ALL".equals(normalized)) {
            return null;
        }
        return Enum.valueOf(type, normalized);
    }

    private record CaseDraft(
            IssueType issueType,
            Severity severity,
            int slaHours,
            String rootCause,
            String recommendedAction,
            String ownerQueue
    ) {
    }
}
