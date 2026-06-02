# Visual Redesign — Execution Plan

> **Status**: Locked, pending execution.
> **Created**: 2026-06-01
> **Direction**: Editorial / Magazine
> **Target branch integration**: `integration/visual-redesign`
> **Final release**: PR `Release: visual redesign editorial` → `main`

This document is the **single source of truth** for executing the Armario Capsula visual redesign. It contains the complete plan, governance, subagent mapping, task decomposition, and the launch prompt. Do not modify mid-execution; evolve via PRs that update this file in `STATUS.md` log.

---

## 1. Context

Armario Capsula is a single-user wardrobe management web app (Java 21 + Spring Boot 3.5.14, Thymeleaf + HTMX 2.0.4, vanilla CSS, no frontend build step). The current visual design is "well-made but generic": one gradient hero, a mix of emojis and Unicode glyphs as icons, fragmented brand identity (3 accents, 4 product names), and Playfair Display + Poppins typography that doesn't lean into its editorial roots.

**This plan modernizes the visual layer** to an Editorial / Magazine direction without touching backend logic, controllers, or DB schema.

Related docs:
- `AGENTS.md` — project conventions, stack, gotchas (will be updated in PR 1 and PR 7)
- `README.md` — setup, architecture (will gain a "Design system" section in PR 7)
- `STATUS.md` — implementation log (created in PR 1, updated every PR)

---

## 2. Locked Decisions

### 2.1 Typography stack
| Family | Weights | Use |
|---|---|---|
| **Instrument Serif** | 400, 400 italic | Display editorial italic — `h1`, `h2`, magazine-cover headlines, empty states, chat empty |
| **Fraunces** | 700, 700 italic + variable `opsz 9..144` | Display with real weight — `big-num` of the ring, monogram "A" of the brand, stat blocks |
| **Inter** | 400–700 variable, italic, `opsz 14..32` | Body, UI, labels, buttons, chips, inputs, captions |

```html
<link rel="stylesheet" href="
  https://fonts.googleapis.com/css2?
  family=Instrument+Serif:ital@0;1&
  family=Fraunces:opsz,wght@9..144,700;9..144,700i&
  family=Inter:ital,opsz,wght@0,14..32,400..700;1,14..32,400..700
  &display=swap">
```

> Note: Instrument Serif ships **only** at weight 400 (+ italic). For real weight, use Fraunces.

### 2.2 Color palette
**Bone neutral ramp** (50 → 1000):
- `--neutral-50: #FBF8F3` (page bg)
- `--neutral-100: #F4EFE7` (cards on warm bg, sunken surfaces)
- `--neutral-200: #E8E2DC` (hairline borders)
- `--neutral-300: #D6CFC6` (subtle dividers, inactive icons)
- `--neutral-400: #A89F94` (disabled text/icons)
- `--neutral-500: #6B6F72` (muted)
- `--neutral-700: #3A3F44` (data-heavy body)
- `--neutral-900: #1F2933` (primary fg)
- `--neutral-1000: #0F1419` (pressed/active)

**Accent** (coral, refined):
- `--accent-50: #FFF1ED` · `--accent-200: #FBC4B8` · `--accent-500: #E5553E` (primary) · `--accent-600: #C9442F` · `--accent-700: #A23524`

**AI-accent** (deep teal, reserved for IA / Companion):
- `--ai-50: #E6F2F0` · `--ai-200: #7BBDB4` · `--ai-500: #0E7C72` · `--ai-600: #085952` · `--ai-700: #063F3A`

**Status** (AA-compliant on white):
- `--danger-500: #C5302A` · `--danger-100: #FCE7E5`
- `--success-500: #1F7A4D` · `--success-100: #E0F1E8`
- `--warning-500: #B6791F` · `--warning-100: #FAEED9`

### 2.3 Component primitives (PR 2)
- **Button**: 6 variants (`primary`, `ghost`, `subtle`, `destructive`, `icon`, default) × 3 sizes (`sm` 32px, `md` 44px, `lg` 52px) + `is-loading` state with spinner.
- **Input**: top-label for compact forms, **floating-label** (`field--float`) for hero forms (garment confirm/edit, profile edit).
- **Chip / Tag / Badge**: 4 states (default, active = neutral-900 inverted, accent = coral tint, ai = teal tint) + `--dot` modifier.
- **Card**: 3 variants (`default` padding 16, `feature` image-led overflow hidden, `compact` 12).
- **Icon**: 4 sizes (`sm` 16, `md` 20, `lg` 24, `xl` 32) + `currentColor` for fills.

### 2.4 Iconography
- **Library**: Lucide, static SVG sprite at `static/css/icons.svg`.
- **No emojis in chrome** (nav, buttons, empty states). Emojis OK in user-generated content or AI output.
- **Use macro**: `<th:block th:replace="~{fragments/icons :: home(class='icon')}"></th:block>` — never inline SVG in templates.
- **Icon set (~25)**: `house`, `shirt`, `plus`, `sparkles`, `wand-sparkles`, `message-circle`, `calendar-days`, `circle-user`, `chevron-left`, `arrow-right`, `x`, `heart`, `clipboard-list`, `thumbs-up`, `thumbs-down`, `star`, `menu`, `image`, `trash-2`, `search`, `search-x`, `user-round`, `image-plus`, `palette`, `send`, `refresh-cw`, `book-open`.

### 2.5 Motion patterns
- **Entrance** (`.enter`): `editorial-rise` 320ms `cubic-bezier(0.2, 0.8, 0.2, 1)`, `translateY(8px)` → 0. Used for chat messages, rec cards, section blocks.
- **Hover** (`.lift`): 250ms `cubic-bezier(0.2, 0.8, 0.2, 1)`, `translateY(-2px)` + `box-shadow: var(--shadow-2)`. Used for cards, links, FAB, primary CTAs on dark.
- **Page transition** (HTMX): `.htmx-swapping` opacity 0 in 120ms, `.htmx-settling` opacity 1 in 200ms. No full-page slide.
- **Streaming cursor**: `▊` blink at 0.8s in `var(--accent-500)` (chat) or `var(--ai-500)` (companion).
- **Reduced motion**: hard reset `*` rule at top of `prefers-reduced-motion: reduce` block.

### 2.6 Spacing / Radius / Elevation
- **Spacing** (12-step, 4px base): `--space-1` 2px → `--space-12` 96px. Migration: `--space-md` → `--space-6`, `--space-lg` → `--space-8`, `--space-xl` → `--space-9`.
- **Radius** (7-step): `xs` 4 / `sm` 8 / `md` 12 / `lg` 16 / `xl` 24 / `2xl` 32 / `pill` 999.
- **Elevation** (4-step): `e1` 1px subtle (resting), `e2` 12px (hover), `e3` 28px (modal/FAB), `e4` 56px (toast/hero).

### 2.7 Naming consistency
- **Brand**: "Armario Capsula" (with tildes: "Armario Cápsula" if a future marketing pass decides so — for now keep as-is in code, fix tildes in copy).
- **Companion name**: "Colorín" (kept).
- **Chat generic**: "Asistente" (not "Asistente de Moda").
- **Drop**: "Colorinchi" from top title. Use "Armario" / page name in top bar.

### 2.8 Out of scope (this plan)
- Backend logic, controllers, services, repositories, DB schema, Flyway migrations.
- Authentication model (stays CSRF-only).
- Dark mode UI toggle (tokens are prepared in PR 7, toggle is future work).
- New routes or new product features.

---

## 3. Governance

### 3.1 Branches
- **Integration branch** (longeva, temporal): `integration/visual-redesign` — created from `main`. All 7 PRs target it. Deleted after release to main.
- **Per-PR sub-branches** (one per PR): `pr/0X-short-name` — created from `main`. Each opens a PR against `integration/visual-redesign`.
- **Release PR**: at the end of PR 7, a final PR `Release: visual redesign editorial` merges `integration/visual-redesign` into `main` with a merge commit.

### 3.2 Sub-branch naming
```
pr/01-foundation
pr/02-primitives
pr/03-layout-dashboard-error
pr/04-wardrobe-detail
pr/05-garment-flow
pr/06-pages
pr/07-chat-companion-motion-dark-release
```

### 3.3 PR checklist (per PR, enforced by reviewer)
- [ ] Sub-rama `pr/0X-nombre` creada desde `main`
- [ ] Cambios commiteados con conventional commits
- [ ] `mvn test` pasa en local
- [ ] Capturas Playwright de 5 páginas clave en 3 viewports (375, 820, 1440) — para PRs 3-7
- [ ] `STATUS.md` actualizado con la entrada del PR (incluye iteraciones de review)
- [ ] Doc del proyecto actualizada si aplica (AGENTS.md en PR 1 y 7; README.md en PR 7)
- [ ] Reviewer (subagente B) marcó **clean** explícitamente
- [ ] PR abierto contra `integration/visual-redesign` con descripción estructurada

---

## 4. Workflow per PR (5 phases)

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. IMPLEMENT       Subagente A: implementa el PR                │
│ 2. REVIEW          Subagente B: revisa código + visual + tests  │
│ 3. ¿Issues?                                                     │
│    ├─ NO  → 5. CREATE PR                                         │
│    └─ SÍ  ↓                                                      │
│ 4. FIX LOOP                                                      │
│    ├─ 4a. SOLUTION DESIGN   Subagente C: diseña el fix           │
│    ├─ 4b. IMPLEMENT FIX     Subagente D (o A): aplica el fix    │
│    └─ 4c. RE-REVIEW         Subagente B: verifica               │
│         └─ ¿Limpio? volver a 4a si no                            │
│ 5. CREATE PR         Subagente E (general): abre el PR           │
└──────────────────────────────────────────────────────────────────┘
```

### 4.1 Subagent mapping per PR

| PR | A. Implementer | B. Reviewer | C. Solution Designer | D. Fix Implementer | E. PR Creator |
|---|---|---|---|---|---|
| 1 Foundation | `Minimal Change Engineer` | `Software Architect` | `Software Architect` | `Minimal Change Engineer` | `general` |
| 2 Primitives | `Frontend Developer` | `UX Architect` + `Code Reviewer` | `UX Architect` | `Frontend Developer` | `general` |
| 3 Layout+Dash+Error | `Frontend Developer` | `Evidence Collector` + `UI Designer` | `UI Designer` | `Frontend Developer` | `general` |
| 4 Wardrobe+Detail | `Frontend Developer` | `Evidence Collector` | `UI Designer` | `Frontend Developer` | `general` |
| 5 Garment flow | `Frontend Developer` | `Evidence Collector` + `UI Designer` | `UI Designer` | `Frontend Developer` | `general` |
| 6 Pages | `Frontend Developer` | `Evidence Collector` | `UI Designer` | `Frontend Developer` | `general` |
| 7 Chat+Companion+Motion+Dark+Release | `Frontend Developer` | `Evidence Collector` + `Code Reviewer` | `UI Designer` (+ `Backend Architect` si refactor de rutas) | `Frontend Developer` | `general` |

### 4.2 Fix loop rules
- **Maximum 3 iterations** of 4a→4b→4c per PR. If still dirty after 3, document in `STATUS.md` and escalate to the user.
- Each iteration appends to `STATUS.md`: issue → root cause → designed solution → applied fix → verified by.
- PR is not opened until the reviewer marks **clean** explicitly.

---

## 5. Plan: 7 PRs with full task lists

### PR 1 — Foundation (no visual changes)
| T | Task | Output |
|---|---|---|
| T1.1 | Create `integration/visual-redesign` from `main` | Branch created |
| T1.2 | Create sub-branch `pr/01-foundation` from `main` | Branch created |
| T1.3 | Rewrite `static/css/tokens.css` with new palette + scales | File rewritten |
| T1.4 | Update `templates/fragments/head.html` with 3-family Google Fonts import + preconnect | File updated |
| T1.5 | Visual regression: confirm app looks identical (Playwright diff vs baseline) | Comparison capture |
| T1.6 | `mvn test` passes | Tests green |
| T1.7 | Create `STATUS.md` in project root with template + first entry | File created |
| T1.8 | Update `AGENTS.md` section 2 with new palette and stack | Doc updated |
| T1.9 | PR `pr/01-foundation` → `integration/visual-redesign` | PR open |

### PR 2 — Primitives (no templates touched)
| T | Task | Output |
|---|---|---|
| T2.1 | Sub-branch `pr/02-primitives` from `main` | Branch created |
| T2.2 | Create `static/css/primitives.css` with `.btn` (6 variants × 3 sizes + loading), `.field--float`, `.chip` (4 states + `--dot`), `.card` (3 variants), `.icon` (4 sizes) | File created |
| T2.3 | Create `static/css/icons.svg` sprite with ~25 Lucide icons | File created |
| T2.4 | Create `templates/fragments/icons.html` with Thymeleaf macros | Fragment created |
| T2.5 | Create `templates/dev/primitives.html` showcase (gated by `dev` profile) | Dev template |
| T2.6 | Activate `dev` profile in `application-dev.yml` (minimum: no security for primitive review) | Profile created |
| T2.7 | `mvn test` + visual verification of `/dev/primitives.html` | Tests + capture |
| T2.8 | `STATUS.md` entry for PR 2 | Doc updated |
| T2.9 | PR → `integration/visual-redesign` | PR open |

### PR 3 — Layout shell + Dashboard + Error (first visible PR)
| T | Task | Output |
|---|---|---|
| T3.1 | Sub-branch `pr/03-layout-dashboard-error` | Branch created |
| T3.2 | Rewrite `sidebar.html`: Lucide icons + numbered kicker (01–08) + short description | Template updated |
| T3.3 | Rewrite `top-bar.html` as editorial masthead | Template updated |
| T3.4 | Rewrite `bottom-nav.html` moving FAB to bottom-right floating with `env(safe-area-inset-bottom)` | Template updated |
| T3.5 | Rewrite `dashboard.html` with editorial hero + stat blocks + inverted "Reto cápsula" section + latest-garments grid | Template updated |
| T3.6 | Rewrite `error.html` with display italic + sub + 2 CTAs | Template updated |
| T3.7 | `mvn test` + Playwright captures of `/dashboard` and `/error` in 3 viewports | Tests + captures |
| T3.8 | `STATUS.md` entry | Doc updated |
| T3.9 | PR → `integration/visual-redesign` | PR open |

### PR 4 — Wardrobe + Garment detail
| T | Task | Output |
|---|---|---|
| T4.1 | Sub-branch `pr/04-wardrobe-detail` | Branch created |
| T4.2 | Rewrite `wardrobe.html`: filter rail with hairline, responsive 4/3/2-up grid, image-led cards, season badge visible | Template updated |
| T4.3 | Rewrite `garment-detail.html`: 3/4 portrait photo, kicker + h1 + italic season caption, "Combina con" editorial strip, "Usada en looks" italic numbered list | Template updated |
| T4.4 | `mvn test` + Playwright captures of `/wardrobe` and `/wardrobe/{id}` in 3 viewports | Tests + captures |
| T4.5 | `STATUS.md` entry | Doc updated |
| T4.6 | PR | PR open |

### PR 5 — Garment flow (new + confirm + edit)
| T | Task | Output |
|---|---|---|
| T5.1 | Sub-branch `pr/05-garment-flow` | Branch created |
| T5.2 | Rewrite `garment-new.html`: editorial intake, single column 600px, dashed dropzone with large `image-plus` icon, instructions in italic display | Template updated |
| T5.3 | Rewrite `garment-confirm.html`: 200px photo thumb, AI Detection callout with `sparkles` icon, floating-label form, `--lg` full-width submit | Template updated |
| T5.4 | Rewrite `garment-edit.html`: confirm shell, AI footnote, delete as link-styled destructive | Template updated |
| T5.5 | `mvn test` + end-to-end Playwright capture | Tests + captures |
| T5.6 | `STATUS.md` entry | Doc updated |
| T5.7 | PR | PR open |

### PR 6 — Remaining pages
| T | Task | Output |
|---|---|---|
| T6.1 | Sub-branch `pr/06-pages` | Branch created |
| T6.2 | Rewrite `weekly-plan.html`: numbered kicker per day, dropzone with day placeholder, garment pool as horizontal scroll rail | Template updated |
| T6.3 | Rewrite `inspiration.html`: magazine hero with kicker + italic h1, search bar with leading icon, 3-col gallery 16/9 | Template updated |
| T6.4 | Rewrite `recommendation.html`: 3-col grid with kicker "LOOK 0X" + italic title + palette + chips | Template updated |
| T6.5 | Rewrite `profile-stats.html`: italic display title, stat blocks 3-up, 2-col section categorías/colores | Template updated |
| T6.6 | Rewrite `admin-chat-metrics.html`: numbered sections, compact stat cards | Template updated |
| T6.7 | `mvn test` + Playwright captures of all pages in 3 viewports | Tests + captures |
| T6.8 | `STATUS.md` entry | Doc updated |
| T6.9 | PR | PR open |

### PR 7 — Chat + Companion + Motion + Dark prep + Release
| T | Task | Output |
|---|---|---|
| T7.1 | Sub-branch `pr/07-chat-companion-motion-dark-release` | Branch created |
| T7.2 | Rewrite `chat.html`: kicker "ASISTENTE" + italic session title, magazine cover empty with quick-prompt chips, messages on warm bg | Template updated |
| T7.3 | Refactor `companion-base.css` and `companion-default.css` with `ai-500/600` tokens, trigger as square with SVG "Colorín" avatar | CSS updated |
| T7.4 | Unify `/chat` ↔ companion: companion is entry point on all pages, `/chat` is expanded view (same DOM, no duplication) | JS/controller refactor if needed |
| T7.5 | Implement 3 motion patterns: `.enter` (320ms), `.lift` (250ms hover), `.htmx-swapping/settling` (120/200ms fade) | CSS updated |
| T7.6 | Harden `prefers-reduced-motion` with universal `*` reset | CSS updated |
| T7.7 | Fix desktop chat layout bug: `100dvh` + `var(--top-bar-h, 0px)` + `var(--nav-h, 0px)` (with `@supports` fallback to `100vh`) | CSS updated |
| T7.8 | Prepare dark mode tokens `[data-theme="dark"]` (no UI toggle) | CSS updated |
| T7.9 | `mvn test` + `axe-core` + Lighthouse mobile | Tests + audit |
| T7.10 | Playwright captures of `/chat` + companion FAB in 3 viewports | Captures |
| T7.11 | Update `README.md` with "Design system" section + screenshots + link to `STATUS.md` | Doc updated |
| T7.12 | Update `AGENTS.md` sections 2, 5, 10 with new conventions | Doc updated |
| T7.13 | `STATUS.md` final consolidated entry | Doc updated |
| T7.14 | PR → `integration/visual-redesign` | PR open |
| T7.15 | PR `Release: visual redesign editorial` → `main` with merge commit | Release merged |

---

## 6. STATUS.md template

```markdown
# STATUS — Visual Redesign

> Plan: Instrument Serif + Fraunces + Inter, paleta Bone + coral + teal AI, sprite Lucide.
> Rama integración: `integration/visual-redesign`. Sub-ramas: `pr/0X-nombre`.
> Última actualización: YYYY-MM-DD

## PR N — <Name>
**Fecha**: YYYY-MM-DD
**Sub-rama**: `pr/0X-name`
**PR**: #<number>

### Goal
[qué cambia y por qué, en 1-2 frases]

### Instructions
[restricciones heredadas, p.ej. "no romper comportamiento actual", "mantener CSRF handler"]

### Discoveries
[hallazgos no obvios durante implementación, ej. "Instrument Serif no tiene weight 700 — usar Fraunces 700i para big-num"]

### Accomplished
- [x] TN.X ...
- [x] TN.X_review — verdict: **clean** / **N issues**
- [x] (si hubo fixes) iteración K: TN.X_design_fixes → TN.X_apply_fixes → TN.X_re_review

### Issues encontrados
| # | Severidad | Archivo:línea | Issue | Causa raíz | Solución diseñada | Fix aplicado | Verificado |
|---|---|---|---|---|---|---|---|
| 1 | major | app.css:312 | hover no respeta reduced-motion | regla específica no agregada al media query global | mover a clase .lift y agregar a reset universal | sí (PR-fix-1) | sí (re-review iter 1) |

### Next Steps
[lo que queda para el próximo PR, o "release completo" si es el último]

### Relevant Files
- path/to/file — qué cambió
- path/to/other — qué cambió
```

---

## 7. Documentation updates per PR

| Doc | PR | Change |
|---|---|---|
| `STATUS.md` (new) | All | Created in PR 1, updated every PR |
| `AGENTS.md` | 1, 7 | PR 1: section 2 (stack) + note "no emojis in chrome, use fragments/icons.html". PR 7: sections 5, 10 with new conventions |
| `README.md` | 7 | New "Design system" section: palette table, typography table, screenshot of dashboard, link to STATUS.md |
| `docs/REDESIGN_PLAN.md` (this file) | — | Locked at start. Future changes go via PR that updates STATUS.md log |

---

## 8. Verification

### Per-PR
- `mvn test` green
- Playwright captures of `/dashboard`, `/wardrobe`, `/wardrobe/{id}`, `/chat`, `/weekly-plan` in 3 viewports (375, 820, 1440) for PRs 3–7
- Visual diff vs previous PR (alert if unintended change)

### End of PR 7
- `axe-core` via Playwright: **0 serious or critical violations**
- Lighthouse mobile: **Performance ≥ 90, Accessibility ≥ 90, Best Practices ≥ 90, SEO ≥ 80**

---

## 9. Risks

| # | Severity | Risk | Mitigation |
|---|---|---|---|
| R1 | Medium | `app.css` 1830-line refactor into primitives can introduce regressions | Playwright captures as baseline, run before/after each PR |
| R2 | Low | `/chat` ↔ companion unification may require controller refactor | Addressed in PR 7 with `Backend Architect` subagent if needed |
| R3 | Low | Lucide static sprite has no SRI (valid for SVG) but verify CSP allows | Test in PR 2; if blocked, fall back to Lucide JS with SRI |
| R4 | Low | `100dvh` not supported in Safari < 15.4 | `@supports (height: 100dvh)` fallback to `100vh` |
| R5 | Low | Old `var(--shadow-hero)` used `color-mix` with old `--accent`; new value shifts shadow tint slightly | Migrate to `--shadow-4`; QA visual diff in PR 3 |
| R6 | Low | `rec-card` stagger animation re-plays on HTMX swap | Intentional; verify reduced-motion override covers it |
| R7 | Low | `prefers-reduced-motion` not in base reset (only specific selectors today) | PR 7 adds universal `*` reset inside the media query |
| R8 | Low | Mobile-only chat layout bug today (138px empty on desktop) | Fixed in PR 7 via `100dvh` + fallback tokens |

---

## 10. Launch prompt (TL;DR, copy-paste ready)

> Ejecutá el rediseño editorial de Armario Capsula en 7 chained PRs sobre la rama de integración `integration/visual-redesign`. Stack: Instrument Serif 400/400i + Fraunces 700/700i + Inter variable. Paleta: accent coral #E5553E + AI-teal #0E7C72. Iconografía Lucide en sprite SVG estático. Orden: PR1 Foundation, PR2 Primitives, PR3 Layout+Dashboard+Error, PR4 Wardrobe+Detail, PR5 Garment flow, PR6 Pages, PR7 Chat+Companion+Motion+Dark+Release. Cada PR en sub-rama `pr/0X-nombre` desde main, PR contra `integration/visual-redesign`. Por cada PR seguí el workflow de 5 fases: IMPLEMENT (Implementer) → REVIEW (Reviewer emite clean/issues) → si issues, FIX LOOP (Solution Designer diseña + Fix Implementer aplica + Reviewer re-revisa, máx 3 iteraciones) → CREATE PR. Mapeo de subagentes: PR1 Minimal Change Engineer / Software Architect, PR2 Frontend Developer / UX Architect+Code Reviewer, PR3-5 Frontend Developer / Evidence Collector+UI Designer, PR6 Frontend Developer / Evidence Collector, PR7 Frontend Developer / Evidence Collector+Code Reviewer (+ Backend Architect si refactor de rutas). Crea `STATUS.md` en raíz con plantilla Goal/Instructions/Discoveries/Accomplished/Issues/Next Steps/Relevant Files y actualizalo en cada PR con las iteraciones de review. Documentación: STATUS.md siempre, AGENTS.md en PR1+7, README.md sección 'Design system' en PR7. Después de cada PR: mvn test + capturas Playwright de /dashboard /wardrobe /wardrobe/{id} /chat /weekly-plan en 3 viewports. Cerrá PR7 con axe-core + Lighthouse mobile ≥ 90 + PR release 'Release: visual redesign editorial' hacia main.

---

## 11. Glossary of substitutions (icons, names)

| Old | New |
|---|---|
| `⌂` (sidebar Inicio, bottom-nav) | `house` Lucide |
| `👔` (sidebar Prendas) | `shirt` Lucide |
| `+` (sidebar Añadir, top-bar action, FAB) | `plus` Lucide |
| `✧` (sidebar Inspiración) | `sparkles` Lucide |
| `✦` (sidebar Sugerencias) | `wand-sparkles` Lucide |
| `💬` (sidebar Chat, bottom-nav) | `message-circle` Lucide |
| `▦` (sidebar Plan, bottom-nav) | `calendar-days` Lucide |
| `♙` (sidebar Perfil, bottom-nav) | `circle-user` Lucide |
| `‹` (top-bar back) | `chevron-left` Lucide |
| `→` (chat send, companion send) | `arrow-right` / `send` Lucide |
| `✕` (delete card, close modal, day remove) | `x` Lucide |
| `♡` / `❤` (favorite) | `heart` Lucide (outline / filled) |
| `🔍` (empty wardrobe filter) | `search-x` Lucide |
| `👔` (empty weekly plan) | `shirt` Lucide |
| `👤` (empty profile) | `user-round` Lucide |
| `💬` (empty chat) | `message-circle` Lucide |
| `📋` (chat context badge) | `clipboard-list` Lucide |
| `✨` (chat copy banner) | `sparkles` Lucide |
| `👍` / `👎` (chat feedback) | `thumbs-up` / `thumbs-down` Lucide |
| `★` (rec score pill) | `star` Lucide |
| `☰` (chat mobile session toggle) | `menu` Lucide |
| "Inspiracion" (no tilde) | "Inspiración" |
| "Combinacion" | "Combinación" |
| "Aun no hay" | "Aún no hay" |
| "Asistente de Moda" | "Asistente" |
| "Colorinchi" (top title) | "Armario" (or page name) |
| Active filter chip teal-dark | Active filter chip neutral-900 |
| Coral gradient hero | Editorial two-column hero with bone bg |

---

*End of plan. Execute via the launch prompt in section 10.*
