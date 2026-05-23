import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiFetch, ApiError } from "../../lib/api";

type ResetRequestResponse = { requested: boolean; resetToken?: string | null };

export function ResetRequestPage() {
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const [token, setToken] = useState<string | null>(null);

  const canSubmit = useMemo(() => email.trim().length > 0 && !busy, [busy, email]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setNote(null);
    setToken(null);

    const value = email.trim();
    if (!value) return;

    setBusy(true);
    try {
      const response = await apiFetch<ResetRequestResponse>("/api/auth/password-reset/request", {
        method: "POST",
        auth: false,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: value })
      });
      setNote("If the email exists, a reset link has been sent.");
      if (response.resetToken) setToken(response.resetToken);
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else setError("Unable to request reset.");
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
            <span>Password recovery</span>
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
              Password reset
            </div>
            <h1 style={{ margin: 0, fontSize: 34, letterSpacing: "-0.06em" }}>Request a reset link</h1>
            <p className="help" style={{ margin: 0 }}>
              Enter your email to begin the reset flow. This endpoint does not confirm whether an account exists.
            </p>
          </div>

          <form onSubmit={onSubmit} style={{ display: "grid", gap: 16 }}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="username"
                placeholder="name@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <button className="btn btn-primary" type="submit" disabled={!canSubmit}>
              {busy ? "Requesting…" : "Send reset link"}
            </button>

            <div className="success" aria-live="polite">
              {note || ""}
            </div>
            <div className="error" aria-live="polite">
              {error || ""}
            </div>

            {token ? (
              <div className="card card-soft" style={{ padding: 18, display: "grid", gap: 10 }}>
                <div className="help" style={{ margin: 0 }}>
                  Reset token (dev):
                </div>
                <div style={{ fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace", fontSize: 12, wordBreak: "break-all" }}>
                  {token}
                </div>
                <Link className="btn btn-outline" to={`/reset/confirm?token=${encodeURIComponent(token)}`}>
                  Continue to reset
                </Link>
              </div>
            ) : null}

            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
              <Link className="btn btn-link" to="/signin">
                Back to sign in
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

