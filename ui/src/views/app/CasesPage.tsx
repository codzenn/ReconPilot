import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiFetch } from "../../lib/api";

type ReconciliationCaseResponse = {
  id: string;
  transactionId: string;
  issueType: string;
  severity: string;
  status: string;
  slaHours: number;
  rootCause: string;
  recommendedAction: string;
  owner: string;
  createdAt: string;
  updatedAt: string;
  resolutionNote: string | null;
};

export function CasesPage() {
  const [status, setStatus] = useState("");
  const [rows, setRows] = useState<ReconciliationCaseResponse[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const url = useMemo(() => {
    const params = new URLSearchParams();
    if (status) params.set("status", status);
    const suffix = params.toString();
    return suffix ? `/api/cases?${suffix}` : "/api/cases";
  }, [status]);

  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    apiFetch<ReconciliationCaseResponse[]>(url)
      .then((data) => {
        if (!cancelled) setRows(data);
      })
      .catch(() => {
        if (!cancelled) setError("Unable to load cases.");
      })
      .finally(() => {
        if (!cancelled) setBusy(false);
      });
    return () => {
      cancelled = true;
    };
  }, [url]);

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div className="card" style={{ padding: 22, display: "grid", gap: 14 }}>
        <div style={{ display: "grid", gap: 8 }}>
          <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
            RCA queue
          </div>
          <h2 style={{ margin: 0, fontSize: 26, letterSpacing: "-0.05em" }}>Cases</h2>
        </div>
        <div className="field" style={{ maxWidth: 260 }}>
          <label htmlFor="status">Status</label>
          <select id="status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All</option>
            <option value="OPEN">Open</option>
            <option value="RESOLVED">Resolved</option>
          </select>
        </div>
        {error ? <div className="error">{error}</div> : null}
      </div>

      <div className="card" style={{ padding: 18, display: "grid", gap: 12 }}>
        {busy ? (
          <div className="help">Loading…</div>
        ) : rows.length === 0 ? (
          <div className="help">No cases found.</div>
        ) : (
          rows.map((c) => (
            <Link
              key={c.id}
              to={`/app/cases/${encodeURIComponent(c.id)}`}
              className="card card-soft"
              style={{
                padding: 18,
                display: "grid",
                gap: 10,
                textDecoration: "none",
                borderRadius: 24
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>{c.issueType}</strong>
                <span className="help">
                  {c.severity} · {c.status}
                </span>
              </div>
              <div className="help">
                Txn {c.transactionId} · Owner {c.owner} · SLA {c.slaHours}h
              </div>
              <div style={{ display: "grid", gap: 6 }}>
                <div className="help">
                  <strong style={{ color: "var(--text)" }}>Root cause:</strong> {c.rootCause}
                </div>
                <div className="help">
                  <strong style={{ color: "var(--text)" }}>Recommended:</strong> {c.recommendedAction}
                </div>
              </div>
            </Link>
          ))
        )}
      </div>
    </div>
  );
}
