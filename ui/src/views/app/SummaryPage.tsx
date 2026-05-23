import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "../../lib/api";

type SummaryResponse = {
  totalTransactions: number;
  openCases: number;
  criticalCases: number;
  valueAtRisk: string;
  channels: Record<string, number>;
};

type ReconcileRunResponse = {
  scannedTransactions: number;
  openedCases: number;
  refreshedCases: number;
  openCases: number;
  message: string;
};

export function SummaryPage() {
  const [summary, setSummary] = useState<SummaryResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    apiFetch<SummaryResponse>("/api/summary")
      .then((data) => {
        if (!cancelled) setSummary(data);
      })
      .catch(() => {
        if (!cancelled) setError("Unable to load summary.");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function runReconciliation() {
    setError(null);
    setNote(null);
    setBusy(true);
    try {
      const response = await apiFetch<ReconcileRunResponse>("/api/reconcile/run", { method: "POST" });
      setNote(response.message || `Reconciliation completed: ${response.openedCases} opened, ${response.refreshedCases} refreshed.`);
      const refreshed = await apiFetch<SummaryResponse>("/api/summary");
      setSummary(refreshed);
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) setError("You do not have permission to run reconciliation.");
      else setError("Unable to run reconciliation.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div className="card" style={{ padding: 26, display: "grid", gap: 10 }}>
        <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
          Overview
        </div>
        <h2 style={{ margin: 0, fontSize: 26, letterSpacing: "-0.05em" }}>Summary</h2>
        <p className="help" style={{ margin: 0 }}>
          High-level operational posture for the current dataset.
        </p>
        <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginTop: 6 }}>
          <button className="btn btn-primary" type="button" onClick={runReconciliation} disabled={busy}>
            {busy ? "Running…" : "Run reconciliation"}
          </button>
        </div>
        {note ? <div className="success">{note}</div> : null}
        {error ? <div className="error">{error}</div> : null}
      </div>

      <section className="grid-3 tight">
        {[
          { label: "Total transactions", value: summary?.totalTransactions ?? "—" },
          { label: "Open cases", value: summary?.openCases ?? "—" },
          { label: "Critical cases", value: summary?.criticalCases ?? "—" }
        ].map((item) => (
          <div key={item.label} className="card card-soft" style={{ padding: 20, display: "grid", gap: 6 }}>
            <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.18em", fontWeight: 820 }}>
              {item.label}
            </div>
            <strong style={{ fontSize: 26, letterSpacing: "-0.05em" }}>{item.value}</strong>
          </div>
        ))}
      </section>

      <section className="card" style={{ padding: 22, display: "grid", gap: 12 }}>
        <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>Channels</strong>
        <div className="help">Transaction distribution by payment channel.</div>
        <div style={{ display: "grid", gap: 10 }}>
          {summary
            ? Object.entries(summary.channels).map(([channel, count]) => (
                <div
                  key={channel}
                  className="card card-soft"
                  style={{ padding: 14, display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}
                >
                  <strong style={{ letterSpacing: "-0.02em" }}>{channel}</strong>
                  <span className="help">{count}</span>
                </div>
              ))
            : null}
        </div>
      </section>
    </div>
  );
}
