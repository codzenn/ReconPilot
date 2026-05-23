# ReconPilot Test Report

Date: 2026-05-23

## Automated Tests

- Backend unit/integration tests: `mvn test`
  - Result: PASS
  - Suite: `com.reconcileguard.service.ReconciliationServiceTest`
  - Coverage focus: seeded KPI summary, reconciliation execution, case resolution, audit writeback

## UI Smoke Checks (Manual)

Recommended verification steps:

- Load `http://localhost:8080` and confirm:
  - Click **Create account** and confirm `/signup.html` loads
  - Verify email (dev token shown when enabled) and confirm `/verify.html` completes verification
  - Sign in at `/signin.html` and confirm redirect to `/app.html`
  - Sidebar navigation switches views without page reload
  - Mobile nav drawer opens/closes (toggle button, scrim click, ESC key)
  - Dashboard renders KPIs, channel exposure bars, and critical case cards
  - Transactions filters update results
  - Toast notifications appear and dismiss (auto + click + ESC)

## Accessibility Checks (Manual)

- Keyboard navigation:
  - Skip link works and moves focus to main content
  - Visible focus ring on buttons/inputs/links
  - Sidebar items and actions are reachable via Tab/Shift+Tab
- Screen reader basics:
  - Navigation buttons expose current page using `aria-current="page"`
  - Toast uses `role="status"` and `aria-live="polite"`
- Reduced motion:
  - `prefers-reduced-motion` disables animations and transitions

## Cross-Browser Guidance

This UI is standards-based HTML/CSS/JS (no framework/runtime dependencies). Validate in:

- Chromium (Chrome/Edge)
- Firefox
- Safari (macOS/iOS)

Key features to confirm:

- CSS grid layout responsiveness (sidebar off-canvas at < 940px)
- Backdrop blur gracefully degrades where unsupported
