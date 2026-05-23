import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { apiFetch, ApiError } from "../../lib/api";
import { clearToken } from "../../lib/auth";

type UserMeResponse = { userId: string; email: string; fullName: string; role: string };

export function AppLayout() {
  const navigate = useNavigate();
  const [me, setMe] = useState<UserMeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const canSeeAudit = me?.role === "ADMIN" || me?.role === "AUDITOR";

  useEffect(() => {
    let cancelled = false;
    setError(null);
    apiFetch<UserMeResponse>("/api/auth/me")
      .then((data) => {
        if (!cancelled) setMe(data);
      })
      .catch((e) => {
        if (!cancelled) {
          if (e instanceof ApiError && e.status === 401) {
            navigate("/signin", { replace: true });
            return;
          }
          setError("Unable to load profile.");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [navigate]);

  return (
    <div className="container" style={{ paddingBottom: 52 }}>
      <header className="nav" style={{ marginBottom: 18 }}>
        <Link className="brand" to="/app">
          <img src="/favicon.svg" alt="ReconPilot" />
          <div>
            <strong>ReconPilot</strong>
            <span>{me ? `${me.fullName || me.email} · ${me.role}` : "Workspace"}</span>
          </div>
        </Link>
        <div className="nav-actions">
          <button
            className="btn btn-outline"
            type="button"
            onClick={() => {
              clearToken();
              navigate("/signin", { replace: true });
            }}
          >
            Sign out
          </button>
        </div>
      </header>

      <div className="app-layout">
        <aside className="card card-soft" style={{ padding: 18, display: "grid", gap: 10, alignContent: "start" }}>
          <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
            Navigation
          </div>
          <nav style={{ display: "grid", gap: 8 }}>
            {[
              { to: "/app", label: "Summary", end: true },
              { to: "/app/transactions", label: "Transactions" },
              { to: "/app/cases", label: "Cases" },
              ...(canSeeAudit ? [{ to: "/app/audit", label: "Audit" }] : [])
            ].map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={(item as any).end}
                className={({ isActive }) =>
                  `btn ${isActive ? "btn-primary" : "btn-outline"}`
                }
                style={{ justifyContent: "flex-start", width: "100%" }}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          {error ? <div className="error">{error}</div> : null}
        </aside>

        <section style={{ minWidth: 0 }}>
          <Outlet />
        </section>
      </div>
    </div>
  );
}
