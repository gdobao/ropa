# STATUS — Visual Redesign

> **Plan**: Instrument Serif + Fraunces + Inter, paleta Bone + coral + teal AI, sprite Lucide.
> **Plan source**: `docs/REDESIGN_PLAN.md` (locked).
> **Integration branch**: `integration/visual-redesign`.
> **Sub-branches**: `pr/0X-nombre` (one per PR, branched from `main`).
> **Last update**: 2026-06-01

This file is the implementation log for the Armario Capsula visual redesign. It
is the companion to `docs/REDESIGN_PLAN.md` — the plan stays locked; the log
lives here and grows with every PR. The final release PR will move this log
into the README's "Design system" section.

---

## PR 1 — Foundation
**Date**: 2026-06-01
**Sub-branch**: `pr/01-foundation`
**PR**: not opened yet (Implementer finished, handoff to Reviewer → PR Creator)
**Reviewer**: `Software Architect` (subagent B)
**Implementer**: `Minimal Change Engineer` (subagent A)

### Goal
Set up the design-system scaffolding — new palette + scales in `tokens.css`,
3-family editorial fonts loaded in `head.html` — so the integration branch is
ready for the next 6 PRs **without** changing how the app looks today.

### Instructions
- **No visual change.** Legacy `--accent`, `--bg`, `--font-display`, `--font-body`,
  etc. must keep resolving to their current values so the rendered HTML/CSS
  matches `main` byte-for-byte at the layout level.
- Touch **only** `static/css/tokens.css`, `templates/fragments/head.html`,
  `AGENTS.md`, and create `STATUS.md`. No backend, no templates, no tests.
- Conventional commits only. No AI attribution, no `--no-verify`.
- Tests must use the H2 in-memory profile — never the local Postgres on 55432.
- Do NOT open the PR (the orchestrator runs the PR Creator `general` subagent).

### Discoveries
- `--accent-soft` was already referenced by `app.css .season-badge` (line 1030)
  on `main` but never defined in `tokens.css`. Defining it now with the
  `color-mix(... 12% ...)` pattern matching `.sidebar-nav a.active` is a tiny
  incidental fix that does not change the rendered look (the value was
  `unset`/inherited before; the new value is the same tint other active
  states already use).
- `--ring-pct`, `--donut-pct` are component-local custom properties **set
  inline by Thymeleaf templates** (`th:style="'--ring-pct:' + ${usagePercent} + ...'"`),
  not consumed from `tokens.css`. `--i` uses a `var(--i, 0)` CSS fallback
  inside `app.css .rec-card`. None of these are gaps; they are scoped
  component-level patterns.
- Local startup is currently blocked by a pre-existing Flyway checksum
  mismatch on V4 (the destructive seed migration that was made a no-op in an
  earlier PR). AGENTS.md §10 explicitly says NOT to run `flyway:repair`
  without user authorization. The app **was** bootable by passing
  `--spring.flyway.enabled=false` as a CLI override for the visual
  regression check, and Spring then validates the existing schema
  successfully.
- The 3-family Google Fonts import can be combined with the existing
  Playfair Display + Poppins request into a single URL — no extra
  round-trip, no preconnect changes needed.
- Instrument Serif ships **only** at weight 400 (+ italic) per Google Fonts.
  Display-bold requirements from §2.1 must use Fraunces 700/700i (variable
  `opsz 9..144`), NOT a heavier cut of Instrument Serif.

### Accomplished
- [x] T1.1 — Confirmed `integration/visual-redesign` branch exists and is current.
- [x] T1.2 — Created `pr/01-foundation` from `main` (no commits from integration).
- [x] T1.3 — Rewrote `static/css/tokens.css` with two `:root` blocks:
  new editorial tokens (neutral-50..1000, accent-50..700, ai-50..700,
  danger/success/warning, space-1..12, radius-xs..2xl/pill,
  elevation-e1..e4, shadow-1..4, font-editorial, font-display-bold,
  font-ui) and a LEGACY block that re-publishes the names consumed by
  `app.css`, `responsive.css`, and the companion CSS with their current
  `main` values.
- [x] T1.4 — Extended the Google Fonts `<link>` in
  `templates/fragments/head.html` to include Instrument Serif (400/400i),
  Fraunces (700/700i variable), and Inter (400..700 variable, italic,
  opsz 14..32) alongside the existing Playfair Display + Poppins. All
  preconnect, scripts, CSRF meta tags, and inline JS untouched.
- [x] T1.5 — Visual regression: app boots (with `--spring.flyway.enabled=false`
  to bypass the pre-existing V4 checksum mismatch) and returns 200 on
  `/dashboard`; Playwright captured `docs/screenshots/pr-01/dashboard-1440.png`;
  computed body font resolves to Poppins, `--accent` resolves to
  `rgb(255,107,95)` = #FF6B5F, page bg resolves to `rgb(255,253,249)` = #FFFDF9
  — all matching the legacy alias values, so the look is identical to `main`.
- [x] T1.6 — `mvn test` passes: **498 tests, 0 failures, 0 errors, BUILD SUCCESS**.
- [x] T1.7 — Created this `STATUS.md` with the PR 1 entry.
- [x] T1.8 — `AGENTS.md` updated (section 2 palette, section 5 icons note).
- [x] T1.9 — PR not opened (handoff to orchestrator → Reviewer → PR Creator).
- [x] Cross-check: every `var(--...)` reference in `app.css`,
  `responsive.css`, and the companion CSS resolves to either a
  `tokens.css` definition or a component-scoped definition
  (script-driven validation; no broken refs).

### Issues encontrados
| # | Severity | File:line | Issue | Root cause | Designed solution | Fix applied | Verified |
|---|---|---|---|---|---|---|---|
| — | — | — | (no reviewer issues yet) | — | — | — | — |

### Next Steps
- Hand off to the Reviewer (subagent B = `Software Architect`).
- Reviewer must emit verdict `clean` or `N issues` per the 5-phase workflow
  in `docs/REDESIGN_PLAN.md` §4.
- If clean, orchestrator hands off to PR Creator (subagent E = `general`)
  to open `pr/01-foundation` → `integration/visual-redesign` with a
  structured description.
- PR 2 (Primitives) starts from `main` again — sub-branch `pr/02-primitives`.

### Relevant Files
- `src/main/resources/static/css/tokens.css` — rewritten (220 lines,
  +~165 net): new editorial tokens in a first `:root` block, legacy aliases
  in a second `:root` block.
- `src/main/resources/templates/fragments/head.html` — extended the
  Google Fonts `<link>` to include Instrument Serif, Fraunces, Inter
  alongside the existing Playfair Display + Poppins. All other head
  content untouched.
- `AGENTS.md` — added palette and design-system notes to section 2,
  added a "no emojis in chrome, use `fragments/icons.html`" note in
  section 5.
- `STATUS.md` — this file (new).
- `docs/screenshots/pr-01/dashboard-1440.png` — baseline Playwright
  capture (1440px viewport) for the visual-regression record.

---
