import { useEffect, useState } from "react";
import { apiFetch, ApiError } from "../../lib/api";

type AuditEventResponse = {
  id: number;
  timestamp: string;
  actor: string;
  action: string;
  reference: string;
  details: string;
};

export function AuditPage() {
  const [rows, setRows] = useState<AuditEventResponse[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setBusy(true);
    setError(null);
    apiFetch<AuditEventResponse[]>("/api/audit")
      .then((data) => {
        if (!cancelled) setRows(data);
      })
      .catch((e) => {
        if (!cancelled) {
          if (e instanceof ApiError && e.status === 403) setError("You do not have access to the audit trail.");
          else setError("Unable to load audit trail.");
        }
      })
      .finally(() => {
        if (!cancelled) setBusy(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div style={{ display: "grid", gap: 18 }}>
      <div className="card" style={{ padding: 22, display: "grid", gap: 10 }}>
        <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
          Compliance
        </div>
        <h2 style={{ margin: 0, fontSize: 26, letterSpacing: "-0.05em" }}>Audit trail</h2>
        <p className="help" style={{ margin: 0 }}>
          Authentication and operator events recorded for traceability.
        </p>
        {error ? <div className="error">{error}</div> : null}
      </div>

      <div className="card" style={{ padding: 18, overflowX: "auto" }}>
        <table style={{ width: "100%", borderCollapse: "collapse", minWidth: 840 }}>
          <thead>
            <tr style={{ textAlign: "left" }}>
              {["Time", "Actor", "Action", "Reference", "Details"].map((h) => (
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
                <td colSpan={5} className="help" style={{ padding: 14 }}>
                  Loading…
                </td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={5} className="help" style={{ padding: 14 }}>
                  No audit events.
                </td>
              </tr>
            ) : (
              rows.map((ev) => (
                <tr key={ev.id} style={{ borderBottom: "1px solid rgba(234, 242, 255, 0.06)" }}>
                  <td style={{ padding: "12px 12px" }}>{ev.timestamp}</td>
                  <td style={{ padding: "12px 12px", fontWeight: 760 }}>{ev.actor}</td>
                  <td style={{ padding: "12px 12px" }}>{ev.action}</td>
                  <td style={{ padding: "12px 12px" }}>{ev.reference}</td>
                  <td style={{ padding: "12px 12px" }}>
                    <span className="help">{ev.details}</span>
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

