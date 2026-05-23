import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiFetch, ApiError } from "../../lib/api";

type CaseResponse = {
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
  transaction?: {
    id: string;
    channel: string;
    utr: string;
    customerName: string;
    branch: string;
    amount: string;
    initiatedAt: string;
    cbsStatus: string;
    switchStatus: string;
    gatewayStatus: string;
    customerComplaint: boolean;
    riskScore: number;
    riskBand: string;
  };
};

export function CaseDetailsPage() {
  const params = useParams();
  const caseId = params.caseId || "";

  const [data, setData] = useState<CaseResponse | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const canResolve = useMemo(() => note.trim().length > 0 && !saving, [note, saving]);

  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    apiFetch<CaseResponse>(`/api/cases/${encodeURIComponent(caseId)}`)
      .then((c) => {
        if (!cancelled) {
          setData(c);
          setNote(c.resolutionNote || "");
        }
      })
      .catch(() => {
        if (!cancelled) setError("Unable to load case.");
      })
      .finally(() => {
        if (!cancelled) setBusy(false);
      });
    return () => {
      cancelled = true;
    };
  }, [caseId]);

  async function onResolve(event: FormEvent) {
    event.preventDefault();
    setSaveError(null);
    if (!note.trim()) return;

    setSaving(true);
    try {
      const updated = await apiFetch<CaseResponse>(`/api/cases/${encodeURIComponent(caseId)}/resolve`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ resolutionNote: note.trim() })
      });
      setData(updated);
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) setSaveError("You do not have permission to resolve cases.");
      else setSaveError("Unable to resolve case.");
    } finally {
      setSaving(false);
    }
  }

  if (busy) {
    return (
      <div className="card" style={{ padding: 22 }}>
        <div className="help">Loading…</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card" style={{ padding: 22 }}>
        <div className="error">{error}</div>
      </div>
    );
  }

  if (!data) return null;

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div className="card" style={{ padding: 22, display: "grid", gap: 10 }}>
        <Link className="btn btn-link" to="/app/cases">
          ← Back to cases
        </Link>
        <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
          <h2 style={{ margin: 0, fontSize: 26, letterSpacing: "-0.05em" }}>{data.issueType}</h2>
          <div className="help">
            {data.severity} · {data.status}
          </div>
        </div>
        <div className="help">
          Txn {data.transactionId} · Owner {data.owner} · SLA {data.slaHours}h
        </div>
      </div>

      <div className="grid-2">
        <section className="card" style={{ padding: 22, display: "grid", gap: 12 }}>
          <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>Root cause</strong>
          <p className="help" style={{ margin: 0 }}>
            {data.rootCause}
          </p>
          <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>Recommended action</strong>
          <p className="help" style={{ margin: 0 }}>
            {data.recommendedAction}
          </p>
        </section>

        <section className="card" style={{ padding: 22, display: "grid", gap: 12 }}>
          <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>Resolution</strong>
          <form onSubmit={onResolve} style={{ display: "grid", gap: 12 }}>
            <div className="field">
              <label htmlFor="resolutionNote">Resolution note</label>
              <textarea
                id="resolutionNote"
                name="resolutionNote"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={5}
                style={{ minHeight: 120, resize: "vertical" }}
                placeholder="Describe resolution and any follow-up actions"
              />
            </div>
            <button className="btn btn-primary" type="submit" disabled={!canResolve}>
              {saving ? "Saving…" : "Resolve case"}
            </button>
            {saveError ? <div className="error">{saveError}</div> : null}
          </form>
        </section>
      </div>

      {data.transaction ? (
        <section className="card" style={{ padding: 22, display: "grid", gap: 12 }}>
          <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>Transaction</strong>
          <div className="grid-3 tight">
            {[
              { k: "Channel", v: data.transaction.channel },
              { k: "UTR", v: data.transaction.utr },
              { k: "Amount", v: data.transaction.amount },
              { k: "CBS", v: data.transaction.cbsStatus },
              { k: "Switch", v: data.transaction.switchStatus },
              { k: "Gateway", v: data.transaction.gatewayStatus },
              { k: "Risk", v: `${data.transaction.riskBand} (${data.transaction.riskScore})` },
              { k: "Customer", v: data.transaction.customerName },
              { k: "Branch", v: data.transaction.branch }
            ].map((item) => (
              <div key={item.k} className="card card-soft" style={{ padding: 14, display: "grid", gap: 6, borderRadius: 22 }}>
                <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.18em", fontWeight: 820 }}>
                  {item.k}
                </div>
                <strong style={{ letterSpacing: "-0.02em" }}>{item.v}</strong>
              </div>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
