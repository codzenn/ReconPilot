import { FormEvent, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { apiFetch, ApiError } from "../../lib/api";

function strengthScore(password: string) {
  const value = password.trim();
  const checks = {
    length: value.length >= 8,
    lower: /[a-z]/.test(value),
    upper: /[A-Z]/.test(value),
    digit: /[0-9]/.test(value),
    symbol: /[^A-Za-z0-9]/.test(value)
  };
  const score = [checks.length, checks.lower, checks.upper, checks.digit, checks.symbol].filter(Boolean).length - 1;
  const normalized = Math.max(0, Math.min(4, score));
  const labels = ["Too weak", "Weak", "Fair", "Good", "Strong"];
  return { score: normalized, label: labels[normalized] };
}

export function ResetConfirmPage() {
  const [params] = useSearchParams();
  const initial = params.get("token") || "";
  const [token, setToken] = useState(initial);
  const [newPassword, setNewPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);

  const strength = useMemo(() => strengthScore(newPassword), [newPassword]);
  const canSubmit = useMemo(() => token.trim().length > 0 && newPassword.trim().length > 0 && !busy, [busy, newPassword, token]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setNote(null);

    if (strength.score < 4) {
      setError("Password must include upper, lower, number, and symbol.");
      return;
    }

    setBusy(true);
    try {
      await apiFetch<{ reset: boolean }>("/api/auth/password-reset/confirm", {
        method: "POST",
        auth: false,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token: token.trim(), newPassword })
      });
      setNote("Password reset completed. You can now sign in.");
      setNewPassword("");
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else setError("Unable to reset password.");
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
            <h1 style={{ margin: 0, fontSize: 34, letterSpacing: "-0.06em" }}>Set a new password</h1>
            <p className="help" style={{ margin: 0 }}>
              Provide your reset token and choose a new strong password.
            </p>
          </div>

          <form onSubmit={onSubmit} style={{ display: "grid", gap: 16 }}>
            <div className="field">
              <label htmlFor="token">Reset token</label>
              <input id="token" name="token" placeholder="Paste token" value={token} onChange={(e) => setToken(e.target.value)} required />
            </div>
            <div className="field">
              <label htmlFor="newPassword">New password</label>
              <input
                id="newPassword"
                name="newPassword"
                type="password"
                autoComplete="new-password"
                placeholder="Create a strong password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
              <div className="help" style={{ display: "flex", justifyContent: "space-between", gap: 10, flexWrap: "wrap" }}>
                <span>Strength: {strength.label}</span>
                <span>Upper/lower · number · symbol · 8+ chars</span>
              </div>
            </div>

            <button className="btn btn-primary" type="submit" disabled={!canSubmit}>
              {busy ? "Resetting…" : "Reset password"}
            </button>

            <div className="success" aria-live="polite">
              {note || ""}
            </div>
            <div className="error" aria-live="polite">
              {error || ""}
            </div>

            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
              <Link className="btn btn-link" to="/reset">
                Back to request
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

