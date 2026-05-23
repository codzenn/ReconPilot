import { FormEvent, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiFetch, ApiError } from "../../lib/api";

type SignUpResponse = {
  userId: string;
};

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

export function SignUpPage() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [acceptTerms, setAcceptTerms] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);

  const strength = useMemo(() => strengthScore(password), [password]);
  const canSubmit = useMemo(() => {
    return (
      fullName.trim().length > 0 &&
      email.trim().length > 0 &&
      password.trim().length > 0 &&
      confirm.trim().length > 0 &&
      acceptTerms &&
      !busy
    );
  }, [acceptTerms, busy, confirm, email, fullName, password]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setNote(null);

    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    if (strength.score < 4) {
      setError("Password must include upper, lower, number, and symbol.");
      return;
    }
    if (!acceptTerms) {
      setError("You must accept the Terms of Service.");
      return;
    }

    setBusy(true);
    try {
      const response = await apiFetch<SignUpResponse>("/api/auth/signup", {
        method: "POST",
        auth: false,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          fullName: fullName.trim(),
          email: email.trim(),
          password: password,
          acceptTerms
        })
      });

      setNote(`Account created for ${response.userId}. You can sign in now.`);
      setPassword("");
      setConfirm("");
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else setError("Unable to create account.");
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
            <span>Create account</span>
          </div>
        </Link>
        <div className="nav-actions">
          <Link className="btn btn-link" to="/signin">
            Sign in
          </Link>
        </div>
      </header>

      <main style={{ padding: "44px 0 72px", display: "grid", justifyContent: "center" }}>
        <section className="card" style={{ width: "min(620px, 100%)", padding: 32, display: "grid", gap: 18 }}>
          <div style={{ display: "grid", gap: 8 }}>
            <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
              New account
            </div>
            <h1 style={{ margin: 0, fontSize: 34, letterSpacing: "-0.06em" }}>Sign up</h1>
            <p className="help" style={{ margin: 0 }}>
              Create your account to access the workspace right away.
            </p>
          </div>

          <form onSubmit={onSubmit} style={{ display: "grid", gap: 16 }}>
            <div className="field">
              <label htmlFor="fullName">Full name</label>
              <input
                id="fullName"
                name="fullName"
                autoComplete="name"
                placeholder="Your full name"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
              />
            </div>

            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                placeholder="name@company.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="field">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="new-password"
                placeholder="Create a strong password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <div className="help" style={{ display: "flex", justifyContent: "space-between", gap: 10, flexWrap: "wrap" }}>
                <span>Strength: {strength.label}</span>
                <span>Upper/lower · number · symbol · 8+ chars</span>
              </div>
              <div
                style={{
                  height: 10,
                  borderRadius: 999,
                  border: "1px solid var(--line)",
                  background: "rgba(5, 8, 20, 0.5)",
                  overflow: "hidden"
                }}
                role="progressbar"
                aria-valuemin={0}
                aria-valuemax={4}
                aria-valuenow={strength.score}
              >
                <div
                  style={{
                    width: `${(strength.score / 4) * 100}%`,
                    height: "100%",
                    background: strength.score >= 4 ? "rgba(34, 198, 163, 0.92)" : "rgba(79, 139, 255, 0.78)",
                    transition: "width 160ms ease"
                  }}
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="confirm">Confirm password</label>
              <input
                id="confirm"
                name="confirm"
                type="password"
                autoComplete="new-password"
                placeholder="Repeat your password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
              />
            </div>

            <label style={{ display: "flex", gap: 12, alignItems: "flex-start" }}>
              <input
                type="checkbox"
                checked={acceptTerms}
                onChange={(e) => setAcceptTerms(e.target.checked)}
                style={{ width: 18, height: 18, marginTop: 3 }}
              />
              <span className="help" style={{ color: "var(--text-2)" }}>
                I agree to the Terms of Service and accept the platform access policy.
              </span>
            </label>

            <button className="btn btn-primary" type="submit" disabled={!canSubmit}>
              {busy ? "Creating account…" : "Create account"}
            </button>

            <div className="success" aria-live="polite">
              {note || ""}
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

