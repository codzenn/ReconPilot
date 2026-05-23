import { FormEvent, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { apiFetch, ApiError } from "../../lib/api";

export function VerifyPage() {
  const [params] = useSearchParams();
  const initial = params.get("token") || "";
  const [token, setToken] = useState(initial);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [verified, setVerified] = useState(false);

  const canSubmit = useMemo(() => token.trim().length > 0 && !busy, [busy, token]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setVerified(false);

    const value = token.trim();
    if (!value) return;

    setBusy(true);
    try {
      await apiFetch<{ verified: boolean }>(`/api/auth/verify?token=${encodeURIComponent(value)}`, { auth: false });
      setVerified(true);
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else setError("Unable to verify email.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="container">
      <header className="nav">
        <Link className="brand" to="/">
          <img src="/favicon.svg" alt="ReconPilot" />
          <div>
            <strong>ReconPilot</strong>
            <span>Email verification</span>
          </div>
        </Link>
        <div className="nav-actions">
          <Link className="btn btn-link" to="/signin">
            Sign in
          </Link>
        </div>
      </header>

      <main style={{ padding: "44px 0 72px", display: "grid", justifyContent: "center" }}>
        <section className="card" style={{ width: "min(560px, 100%)", padding: 32, display: "grid", gap: 18 }}>
          <div style={{ display: "grid", gap: 8 }}>
            <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
              Verification
            </div>
            <h1 style={{ margin: 0, fontSize: 34, letterSpacing: "-0.06em" }}>Verify your email</h1>
            <p className="help" style={{ margin: 0 }}>
              Paste the verification token to activate your account.
            </p>
          </div>

          <form onSubmit={onSubmit} style={{ display: "grid", gap: 16 }}>
            <div className="field">
              <label htmlFor="token">Verification token</label>
              <input
                id="token"
                name="token"
                placeholder="Paste token"
                value={token}
                onChange={(e) => setToken(e.target.value)}
                required
              />
            </div>

            <button className="btn btn-primary" type="submit" disabled={!canSubmit}>
              {busy ? "Verifying…" : "Verify email"}
            </button>

            <div className="success" aria-live="polite">
              {verified ? "Email verified. You can now sign in." : ""}
            </div>
            <div className="error" aria-live="polite">
              {error || ""}
            </div>

            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
              <Link className="btn btn-link" to="/signin">
                Go to sign in
              </Link>
              <Link className="btn btn-link" to="/">
                Back to landing page
              </Link>
            </div>
          </form>
        </section>
      </main>
    </div>
  );
}

