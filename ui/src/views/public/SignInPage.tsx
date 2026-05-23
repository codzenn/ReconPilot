import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { apiFetch, ApiError } from "../../lib/api";
import { loadToken, saveToken } from "../../lib/auth";

type AuthTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  userId: string;
  email: string;
  fullName: string;
  role: string;
};

export function SignInPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as any)?.from || "/app";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<AuthTokenResponse | null>(null);

  const canSubmit = useMemo(() => email.trim().length > 0 && password.trim().length > 0 && !busy, [busy, email, password]);

  useEffect(() => {
    const existing = loadToken();
    if (existing) {
      navigate("/app", { replace: true });
    }
  }, [navigate]);

  useEffect(() => {
    if (!success) return;
    navigate(from, { replace: true });
  }, [from, navigate, success]);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    const emailValue = email.trim();
    const passwordValue = password.trim();
    if (!emailValue || !passwordValue) return;

    setBusy(true);
    try {
      const response = await apiFetch<AuthTokenResponse>("/api/auth/login", {
        method: "POST",
        auth: false,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: emailValue, password: passwordValue })
      });
      saveToken(response.accessToken);
      setSuccess(response);
      setPassword("");
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else setError("Unable to sign in.");
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
            <span>Secure access</span>
          </div>
        </Link>
        <div className="nav-actions">
          <Link className="btn btn-link" to="/signup">
            Create account
          </Link>
        </div>
      </header>

      <main style={{ padding: "44px 0 72px", display: "grid", justifyContent: "center" }}>
        <section className="card" style={{ width: "min(560px, 100%)", padding: 32, display: "grid", gap: 18 }}>
          <div style={{ display: "grid", gap: 8 }}>
            <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.22em", fontWeight: 820 }}>
              Account access
            </div>
            <h1 style={{ margin: 0, fontSize: 34, letterSpacing: "-0.06em" }}>Sign in</h1>
            <p className="help" style={{ margin: 0 }}>
              Use your email and password to access the workspace.
            </p>
          </div>

          {!success ? (
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
              <div className="field">
                <label htmlFor="password">Password</label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>


              <button className="btn btn-primary" type="submit" disabled={!canSubmit}>
                {busy ? "Signing in…" : "Sign in"}
              </button>
              <div className="error" aria-live="polite">
                {error || ""}
              </div>
            </form>
          ) : (
            <div className="help" aria-live="polite">
              Redirecting to dashboard…
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
