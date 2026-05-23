import { FormEvent, useEffect, useMemo, useState } from "react";
import { apiFetch } from "../../lib/api";

type PaymentTransactionResponse = {
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

export function TransactionsPage() {
  const [q, setQ] = useState("");
  const [channel, setChannel] = useState("");
  const [risk, setRisk] = useState("");
  const [rows, setRows] = useState<PaymentTransactionResponse[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const query = useMemo(() => {
    const params = new URLSearchParams();
    if (q.trim()) params.set("q", q.trim());
    if (channel) params.set("channel", channel);
    if (risk) params.set("risk", risk);
    const suffix = params.toString();
    return suffix ? `/api/transactions?${suffix}` : "/api/transactions";
  }, [channel, q, risk]);

  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    apiFetch<PaymentTransactionResponse[]>(query)
      .then((data) => {
        if (!cancelled) setRows(data);
      })
      .catch(() => {
        if (!cancelled) setError("Unable to load transactions.");
      })
      .finally(() => {
        if (!cancelled) setBusy(false);
      });
    return () => {
      cancelled = true;
    };
  }, [query]);

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    setQ(q.trim());
  }

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div className="card" style={{ padding: 22, display: "grid", gap: 14 }}>
        <div style={{ display: "grid", gap: 8 }}>
          <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
            Investigation
          </div>
          <h2 style={{ margin: 0, fontSize: 26, letterSpacing: "-0.05em" }}>Transactions</h2>
        </div>

        <form onSubmit={onSubmit} className="filters">
          <div className="field">
            <label htmlFor="q">Search</label>
            <input id="q" value={q} onChange={(e) => setQ(e.target.value)} placeholder="UTR, customer, branch…" />
          </div>
          <div className="field">
            <label htmlFor="channel">Channel</label>
            <select id="channel" value={channel} onChange={(e) => setChannel(e.target.value)}>
              <option value="">All</option>
              <option value="UPI">UPI</option>
              <option value="IMPS">IMPS</option>
              <option value="NEFT">NEFT</option>
              <option value="RTGS">RTGS</option>
              <option value="CARD">CARD</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="risk">Risk</label>
            <select id="risk" value={risk} onChange={(e) => setRisk(e.target.value)}>
              <option value="">All</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
          <div>
            <button className="btn btn-outline" type="submit">
              Apply
            </button>
          </div>
        </form>
        {error ? <div className="error">{error}</div> : null}
      </div>

      <div className="card" style={{ padding: 18, overflowX: "auto" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", minWidth: 920 }}>
          <thead>
            <tr style={{ textAlign: "left" }}>
              {["Channel", "UTR", "Customer", "Amount", "Statuses", "Risk"].map((h) => (
                <th
                  key={h}
                  style={{
                    padding: "10px 12px",
                    borderBottom: "1px solid var(--line)",
                    color: "var(--muted)",
                    fontSize: 12,
                    letterSpacing: "0.14em",
                    textTransform: "uppercase"
                  }}
                >
                  {h}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {busy ? (
              <tr>
                <td colSpan={6} className="help" style={{ padding: 14 }}>
                  Loading…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={6} className="help" style={{ padding: 14 }}>
                  No transactions found.
                </td>
              </tr>
            ) : (
              rows.map((tx) => (
                <tr key={tx.id} style={{ borderBottom: "1px solid rgba(234, 242, 255, 0.06)" }}>
                  <td style={{ padding: "12px 12px", fontWeight: 780 }}>{tx.channel}</td>
                  <td style={{ padding: "12px 12px" }}>{tx.utr}</td>
                  <td style={{ padding: "12px 12px" }}>
                    <div style={{ fontWeight: 760 }}>{tx.customerName}</div>
                    <div className="help">{tx.branch}</div>
                  </td>
                  <td style={{ padding: "12px 12px", fontWeight: 780 }}>{tx.amount}</td>
                  <td style={{ padding: "12px 12px" }}>
                    <div className="help">
                      CBS: {tx.cbsStatus} · Switch: {tx.switchStatus} · Gateway: {tx.gatewayStatus}
                    </div>
                  </td>
                  <td style={{ padding: "12px 12px" }}>
                    <div style={{ fontWeight: 820 }}>
                      {tx.riskBand} ({tx.riskScore})
                    </div>
                    {tx.customerComplaint ? <div className="error">Complaint</div> : <div className="help">No complaint</div>}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
