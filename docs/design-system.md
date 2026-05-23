# ReconPilot Design System

## Tokens (CSS Variables)

Tokens live in `src/main/resources/static/styles.css` under `:root`.

- Background and surfaces: `--bg`, `--surface`, `--surface-2`
- Text: `--text`, `--text-secondary`, `--muted`
- Brand: `--accent`, `--accent-2`, `--accent-3`, `--accent-soft`
- Status: `--critical`, `--high`, `--medium`, `--low` (+ `*-soft`)
- Depth: `--shadow`, `--shadow-lg`
- Shape: `--radius`

## Components

Layout

- App shell: `.app-shell` (sidebar + content grid)
- Sidebar: `.sidebar` with off-canvas behavior under 940px
- Top bar: `.topbar` (sticky-feeling glass surface)

Navigation

- Primary nav item: `.nav-item` (+ `.active`)
- Mobile toggle: `.icon-button` + scrim overlay `.scrim`

Cards and panels

- KPI card: `.metric`
- Panel container: `.panel`, `.panel-head`

Data display

- Tables: `.table-wrap table` with compact rows and muted secondary text
- Channel exposure chart: `.bar-chart`, `.bar-row`, `.bar-track`, `.bar-fill`

Status

- Pills: `.badge` with variant classes derived from API values:
  - `.CRITICAL`, `.HIGH`, `.MEDIUM`, `.LOW`
  - `.status` for neutral state tags

Feedback

- Toast: `#toast.toast` (+ `.show`)
- View transitions: `.view.enter` animation on navigation

## Accessibility

- Skip link: `.skip-link` to jump to `#mainContent`
- Focus visibility: `:focus-visible` ring meets contrast expectations
- Reduced motion: global opt-out via `prefers-reduced-motion`
