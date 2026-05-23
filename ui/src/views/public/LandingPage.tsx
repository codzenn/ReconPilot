import { Link } from "react-router-dom";

export function LandingPage() {
  return (
    <div className="container">
      <header className="nav">
        <Link className="brand" to="/">
          <img src="/favicon.svg" alt="ReconPilot" />
          <div>
            <strong>ReconPilot</strong>
            <span>Payments operations workspace</span>
          </div>
        </Link>
        <div className="nav-actions">
          <Link className="btn btn-link" to="/signin">
            Sign in
          </Link>
          <Link className="btn btn-primary" to="/signup">
            Create account
          </Link>
        </div>
      </header>

      <main style={{ padding: "44px 0 56px", display: "grid", gap: 28 }}>
        <section className="card" style={{ padding: 36 }}>
          <div style={{ display: "grid", gap: 18, maxWidth: 860 }}>
            <div className="help" style={{ textTransform: "uppercase", letterSpacing: "0.24em", fontWeight: 800 }}>
              Reconciliation · Exceptions · Audit
            </div>
            <h1 style={{ margin: 0, fontSize: "clamp(2.6rem, 5vw, 4.4rem)", lineHeight: 0.95, letterSpacing: "-0.08em" }}>
              A focused workspace for high-signal payments operations.
            </h1>
            <p className="help" style={{ fontSize: 15, lineHeight: 1.8, maxWidth: 620 }}>
              ReconPilot helps teams reconcile payment channels, triage exceptions, document root-cause decisions, and preserve an
              audit-ready history with secure account access.
            </p>
            <div style={{ display: "flex", gap: 14, flexWrap: "wrap" }}>
              <Link className="btn btn-primary" to="/signup">
                Start with email sign-up
              </Link>
              <Link className="btn btn-outline" to="/signin">
                Sign in
              </Link>
            </div>
          </div>
        </section>

        <section className="grid-3">
          {[
            {
              title: "Secure access",
              body: "Strong credentials and explicit session handling keep entry controlled."
            },
            {
              title: "Operational clarity",
              body: "Structured views for runs, transactions, and exceptions help teams stay decisive."
            },
            {
              title: "Traceability first",
              body: "Key actions remain attributable, making reviews and audits straightforward."
            }
          ].map((item) => (
            <article key={item.title} className="card card-soft" style={{ padding: 26, display: "grid", gap: 10 }}>
              <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>{item.title}</strong>
              <p className="help" style={{ margin: 0 }}>
                {item.body}
              </p>
            </article>
          ))}
        </section>

        <section className="card" style={{ padding: 32, display: "grid", gap: 20 }}>
          <div style={{ display: "grid", gap: 10, maxWidth: 820 }}>
            <strong style={{ fontSize: 20, letterSpacing: "-0.05em" }}>Get started in three steps</strong>
            <p className="help" style={{ margin: 0 }}>
              Create an account, sign in, and open the workspace when you are ready.
            </p>
          </div>
          <div className="grid-3 tight">
            {[
              { step: "01", title: "Create account", body: "Use a work email and a strong password." },
              { step: "02", title: "Sign in", body: "Use the credentials you just created." },
              { step: "03", title: "Open workspace", body: "Enter the dashboard without surprise redirects." }
            ].map((item) => (
              <div
                key={item.step}
                className="card card-soft"
                style={{ padding: 22, display: "grid", gap: 10, borderRadius: 24 }}
              >
                <div className="help" style={{ fontWeight: 850, letterSpacing: "0.2em" }}>
                  {item.step}
                </div>
                <strong style={{ fontSize: 16, letterSpacing: "-0.03em" }}>{item.title}</strong>
                <p className="help" style={{ margin: 0 }}>
                  {item.body}
                </p>
              </div>
            ))}
          </div>
          <div style={{ display: "flex", gap: 14, flexWrap: "wrap" }}>
            <Link className="btn btn-primary" to="/signup">
              Create account
            </Link>
            <Link className="btn btn-outline" to="/signin">
              Sign in
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}
