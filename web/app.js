const state = {
  summary: null,
  cases: [],
  transactions: [],
  audit: [],
  activeView: "dashboard"
};

const money = new Intl.NumberFormat("en-IN", {
  style: "currency",
  currency: "INR",
  maximumFractionDigits: 0
});

const els = {
  metrics: document.querySelector("#metrics"),
  chart: document.querySelector("#channelChart"),
  chartTotal: document.querySelector("#chartTotal"),
  criticalCases: document.querySelector("#criticalCases"),
  caseQueue: document.querySelector("#caseQueue"),
  txnRows: document.querySelector("#txnRows"),
  auditList: document.querySelector("#auditList"),
  toast: document.querySelector("#toast"),
  authStatus: document.querySelector("#authStatus")
};

const savedToken = localStorage.getItem("rgApiToken");
if (savedToken) {
  document.querySelector("#apiToken").value = savedToken;
  setAuthStatus("Saved token loaded");
}
document.querySelector("#apiToken").addEventListener("change", () => {
  localStorage.setItem("rgApiToken", document.querySelector("#apiToken").value.trim());
  setAuthStatus("Token updated");
});

document.querySelectorAll(".nav-item").forEach((button) => {
  button.addEventListener("click", () => switchView(button.dataset.view));
});

document.querySelector("#loginButton").addEventListener("click", issueJwt);
document.querySelector("#runRecon").addEventListener("click", runReconciliation);
document.querySelector("#refreshCases").addEventListener("click", loadAll);
document.querySelector("#txnSearch").addEventListener("input", debounce(loadTransactions, 250));
document.querySelector("#channelFilter").addEventListener("change", loadTransactions);
document.querySelector("#riskFilter").addEventListener("change", loadTransactions);
document.querySelector("#caseSearch").addEventListener("input", renderCases);

loadAll().catch((error) => showToast(error.message));

async function issueJwt() {
  const operatorId = document.querySelector("#operatorId").value.trim() || "ops.peeyush";
  const password = document.querySelector("#adminPassword").value;
  const response = await fetch("/api/auth/token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ operatorId, password })
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message = data.message || data.error || "JWT endpoint unavailable";
    setAuthStatus("JWT not issued");
    showToast(message);
    throw new Error(message);
  }
  document.querySelector("#apiToken").value = data.accessToken;
  localStorage.setItem("rgApiToken", data.accessToken);
  setAuthStatus("JWT issued");
  showToast(`JWT issued for ${data.operatorId}`);
  await loadAll();
}

async function loadAll() {
  const [summary, cases, audit] = await Promise.all([
    api("/api/summary"),
    api("/api/cases"),
    api("/api/audit")
  ]);
  state.summary = summary;
  state.cases = cases;
  state.audit = audit;
  await loadTransactions();
  renderAll();
}

async function loadTransactions() {
  const params = new URLSearchParams({
    q: document.querySelector("#txnSearch").value.trim(),
    channel: document.querySelector("#channelFilter").value,
    risk: document.querySelector("#riskFilter").value
  });
  state.transactions = await api(`/api/transactions?${params}`);
  renderTransactions();
}

async function runReconciliation() {
  const result = await api("/api/reconcile/run", {
    method: "POST",
    headers: operatorHeaders()
  });
  showToast(result.message);
  await loadAll();
}

async function resolveCase(caseId) {
  const note = `Resolved through ReconcileGuard maker-checker review at ${new Date().toLocaleString()}`;
  await api(`/api/cases/${caseId}/resolve`, {
    method: "POST",
    headers: {
      ...operatorHeaders(),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ resolutionNote: note })
  });
  showToast(`${caseId} resolved`);
  await loadAll();
}

async function api(path, options = {}) {
  options.headers = {
    ...authHeaders(),
    ...(options.headers || {})
  };
  const response = await fetch(path, options);
  const data = await response.json();
  if (!response.ok) {
    const message = data.message || data.error || "Request failed";
    showToast(message);
    throw new Error(message);
  }
  return data;
}

function operatorHeaders() {
  return {
    ...authHeaders(),
    "X-Operator-Id": document.querySelector("#operatorId").value.trim() || "ops.peeyush"
  };
}

function authHeaders() {
  return {
    Authorization: `Bearer ${document.querySelector("#apiToken").value.trim() || "dev-token-change-me"}`
  };
}

function renderAll() {
  renderSummary();
  renderCases();
  renderTransactions();
  renderAudit();
}

function renderSummary() {
  const s = state.summary;
  els.metrics.innerHTML = [
    metric("Transactions Scanned", s.totalTransactions, "Loaded from CBS/switch/gateway feeds"),
    metric("Open RCA Cases", s.openCases, "Pending operations action"),
    metric("Critical Cases", s.criticalCases, "SLA or customer-impact risk"),
    metric("Value at Risk", money.format(s.valueAtRisk), "Unresolved payment exposure")
  ].join("");

  const channels = Object.entries(s.channels || {});
  const total = channels.reduce((sum, [, count]) => sum + count, 0);
  els.chartTotal.textContent = `${total} open`;
  els.chart.innerHTML = channels.length
    ? channels.map(([name, count]) => {
        const pct = Math.max(6, Math.round((count / Math.max(total, 1)) * 100));
        return `
          <div class="bar-row">
            <strong>${escapeHtml(name)}</strong>
            <div class="bar-track"><div class="bar-fill" style="width:${pct}%"></div></div>
            <span>${count}</span>
          </div>
        `;
      }).join("")
    : `<p>No open channel exposure.</p>`;

  const critical = state.cases
    .filter((item) => item.status === "OPEN")
    .sort((a, b) => severityScore(b.severity) - severityScore(a.severity))
    .slice(0, 4);
  els.criticalCases.innerHTML = critical.length ? critical.map(caseCard).join("") : empty("No critical queue.");
}

function renderCases() {
  const q = document.querySelector("#caseSearch").value.toLowerCase().trim();
  const list = state.cases.filter((item) => {
    const tx = item.transaction || {};
    const blob = `${item.id} ${item.issueType} ${item.severity} ${tx.utr || ""} ${tx.customerName || ""}`.toLowerCase();
    return !q || blob.includes(q);
  });
  els.caseQueue.innerHTML = list.length ? list.map(caseCard).join("") : empty("No matching RCA cases.");
  els.caseQueue.querySelectorAll("[data-resolve]").forEach((button) => {
    button.addEventListener("click", () => resolveCase(button.dataset.resolve));
  });
}

function renderTransactions() {
  els.txnRows.innerHTML = state.transactions.map((txn) => `
    <tr>
      <td>
        <strong>${escapeHtml(txn.id)}</strong>
        <span>${escapeHtml(txn.utr)} &middot; ${escapeHtml(txn.customerName)} &middot; ${escapeHtml(txn.branch)}</span>
      </td>
      <td>${escapeHtml(txn.channel)}</td>
      <td>${money.format(txn.amount)}</td>
      <td>${statusPill(txn.cbsStatus)}</td>
      <td>${statusPill(txn.switchStatus)}</td>
      <td>${statusPill(txn.gatewayStatus)}</td>
      <td>${badge(txn.riskBand)}<span>${txn.riskScore}/100</span></td>
    </tr>
  `).join("");
}

function renderAudit() {
  els.auditList.innerHTML = state.audit.length
    ? state.audit.map((item) => `
      <div class="audit-item">
        <div class="case-title">
          <strong>${escapeHtml(item.action)}</strong>
          <span class="badge status">${escapeHtml(item.reference)}</span>
        </div>
        <div class="audit-meta">
          <span>${escapeHtml(item.timestamp)}</span>
          <span>${escapeHtml(item.actor)}</span>
        </div>
        <p>${escapeHtml(item.details)}</p>
      </div>
    `).join("")
    : empty("No audit events.");
}

function caseCard(item) {
  const tx = item.transaction || {};
  const resolved = item.status === "RESOLVED";
  return `
    <article class="case-item">
      <div>
        <div class="case-title">
          ${badge(item.severity)}
          <strong>${escapeHtml(item.id)} &middot; ${formatIssue(item.issueType)}</strong>
          <span class="badge status">${escapeHtml(item.status)}</span>
        </div>
        <div class="case-meta">
          <span>${escapeHtml(tx.channel || "")}</span>
          <span>${escapeHtml(tx.utr || "")}</span>
          <span>${escapeHtml(tx.customerName || "")}</span>
          <span>${money.format(tx.amount || 0)}</span>
          <span>SLA ${item.slaHours}h</span>
        </div>
        <p>${escapeHtml(item.rootCause)}</p>
        <p><strong>Action:</strong> ${escapeHtml(item.recommendedAction)}</p>
      </div>
      <div>
        ${resolved ? "" : `<button class="resolve-btn" data-resolve="${escapeHtml(item.id)}" type="button">Resolve</button>`}
      </div>
    </article>
  `;
}

function metric(label, value, subtext) {
  return `
    <div class="metric">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(String(value))}</strong>
      <small>${escapeHtml(subtext)}</small>
    </div>
  `;
}

function badge(value) {
  return `<span class="badge ${escapeHtml(value)}">${escapeHtml(value)}</span>`;
}

function statusPill(value) {
  const severity = value === "SUCCESS" ? "LOW" : value === "PENDING" ? "MEDIUM" : "HIGH";
  return `<span class="badge ${severity}">${escapeHtml(value)}</span>`;
}

function empty(text) {
  return `<div class="case-item"><p>${escapeHtml(text)}</p></div>`;
}

function switchView(view) {
  state.activeView = view;
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.classList.toggle("active", button.dataset.view === view);
  });
  document.querySelectorAll(".view").forEach((section) => section.classList.remove("active-view"));
  document.querySelector(`#${view}View`).classList.add("active-view");
}

function showToast(message) {
  els.toast.textContent = message;
  els.toast.classList.add("show");
  setTimeout(() => els.toast.classList.remove("show"), 2500);
}

function setAuthStatus(message) {
  if (els.authStatus) {
    els.authStatus.textContent = message;
  }
}

function severityScore(value) {
  return { CRITICAL: 4, HIGH: 3, MEDIUM: 2, LOW: 1 }[value] || 0;
}

function formatIssue(value) {
  return value.toLowerCase().split("_").map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join(" ");
}

function debounce(fn, wait) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
