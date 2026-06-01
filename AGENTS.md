# Armario Capsula — Project Instructions

Project-level AGENTS.md for future AI sessions. Supersedes global defaults only for project-specific concerns.

---

## 1. Project Identity

- **Name**: Armario Capsula
- **Group**: `com.colorinchi`, artifact: `ropa`
- **Purpose**: Single-user wardrobe management app — upload garment photos, AI classifies type/color, plan weekly outfits
- **Language (UI)**: Spanish (español de España — use Pantalón not Pantalon, Sudadera not Buzo)
- **Language (code/docs)**: English

---

## 2. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.14 |
| Build | Maven |
| Templates | Thymeleaf (server-side, NOT REST API) |
| Interactivity | HTMX 2.0.4 (partial page updates, fragments) |
| Database | PostgreSQL 16 Alpine (port 55432) |
| Migrations | Flyway |
| Security | Spring Security 6 (CSRF only — NO authentication) |
| Cache | Caffeine |
| HTTP client | WebClient (reactive, for AI API calls) |
| Image processing | Thumbnailator + TwelveMonkeys WebP |
| Colorimetry | ColorSpaceConverter, ColorSeasonClassifier, ColorCompatibilityEngine, ColorPaletteStore |
| Test DB | H2 (in-memory) |
| Test HTTP mock | WireMock 3.9.1 |
| Test frameworks | Mockito, MockMvc, @DataJpaTest, Playwright (E2E) |

---

## 3. Architecture

### Layered MVC
```
GarmentController → GarmentService → GarmentRepository
FashionChatController → ChatSessionService → ChatSessionRepository
CompanionChatApiController → ChatConversationOrchestrator → Ai provider (SSE)
                              ↓
                         CompanionTipService
```

### Component Inventory

| Type | Count | Details |
|---|---|---|
| Controllers | 6 | `GarmentController` (20 endpoints), `FashionChatController`, `ChatApiController`, `CompanionChatApiController`, `ChatMetricsController`, `AdminChatMetricsController` |
| Services | 16+ | `GarmentService`, `GarmentCompatibilityService`, `AiClassificationService`, `AiRecommendationService`, `WeekPlanService`, `InspirationService`, `ChatSessionService`, `ChatMessageService`, `ChatRunService`, `ChatFeedbackService`, `ChatConversationOrchestrator`, `ChatStreamPersistenceService`, `ChatPromptFactory`, `WardrobeContextAssembler`, `CompanionTipService`, `ChatPolicyService`, `AnonymousOwnerService`, `ChatDataRetentionService`, `CurrentOwnerAccessor` |
| Storage | 1 interface + 1 impl | `ImageStorageService` / `LocalImageStorageService` |
| Repositories | 8 | `GarmentRepository`, `WeekPlanRepository`, `ChatSessionRepository`, `ChatRunRepository`, `ChatFeedbackRepository`, `ChatMessageRepository`, `ChatAnalyticsEventRepository`, `AnonymousOwnerRepository` |
| DTOs | 12+ | `GarmentReviewForm`, `DashboardStats`, `AiClassificationResponse`, `AiRecommendationResponse`, `OutfitSuggestion`, `OutfitPiece`, `InspirationLook`, `CompanionTipContext`, `GarmentSummary`, `ChatSurface`, `ChatSessionResponse`, `ChatMessageResponse`, `CreateSessionRequest`, `ChatFeedbackRequest`, `WeeklyPlanItem` |
| @ConfigurationProperties | 6 | `WardrobeProperties`, `UploadProperties`, `AiServerProperties`, `RateLimitProperties`, `AdminProperties`, `ChatRetentionProperties` |
| Config | 8+ | `SecurityConfig`, `WebMvcConfig`, `WebClientConfig`, `AiServerPropertiesValidator`, `GlobalExceptionHandler`, `ApiExceptionHandler`, `CurrentOwnerFilter`, `AdminProperties`, `ChatRetentionProperties` |

### Patterns
- All config via `@ConfigurationProperties` records
- `GlobalExceptionHandler` (`@ControllerAdvice`) catches `IllegalArgumentException`, `SecurityException`, `RateLimitExceededException`, `Exception`
- Layout decorator pattern with `th:replace` on `layout.html`
- HTMX fragments: stable wrapper IDs (e.g., `#wardrobe-grid`, `#garment-grid`, `#favDetailButton`)
- Colorín Companion via JS modal (`companion-assistant.js`) con `position: fixed` y viewport-aware panel placement
- CSS design tokens via custom properties (`tokens.css`)

### Colorimetry Engine

New package: `com.colorinchi.app.colorimetry`

| Type | Count | Details |
|---|---|---|
| Enums | 5 | ColorSeason, ColorTemperature, ColorIntensity, ColorDepth, ColorHarmony |
| Records | 3 | ColorProfile, CompatibilityResult, NamedColor |
| Services | 2 | ColorSeasonClassifier (CIELAB matrix + nearest-neighbor fallback), ColorCompatibilityEngine (ΔE00 + season + rules) |
| Data | 1 | ColorPaletteStore (4 seasons × ~25 colors + 15 neutrals) |
| Util | 1 | ColorSpaceConverter (hex→RGB→XYZ→CIELAB, ΔE2000, ΔE76, hex→HSL) |
| Config | 1 | ColorimetryProperties (@ConfigurationProperties("app.colorimetry")) |

Classification: hex → CIELAB → temperature/intensity/depth → season matrix → nearest-neighbor fallback
Scoring: ΔE00(40%) + season(30%) + rules(30%) → additive formula
Blacklist: rojo+rosa, negro+azul marino, warm+cool without neutral bridge

---

## 4. Routes

All endpoints in `GarmentController.java`.

| Method | Path | View / Fragment | Purpose |
|---|---|---|---|
| GET | `/` | redirect:/dashboard | Root redirect |
| GET | `/dashboard` | `dashboard.html` | Stats, latest garments, usage message |
| GET | `/wardrobe` | `wardrobe.html` | All garments, `activeCategory=""` |
| GET | `/wardrobe/filter?category=` | `wardrobe :: grid` | HTMX fragment; `category=favoritos` for favorites |
| POST | `/wardrobe/analyze` | `garment-confirm.html` | Upload + AI classification |
| POST | `/wardrobe` | redirect:/wardrobe/{id} | Create garment from form |
| POST | `/wardrobe/seed` | redirect:/wardrobe | Seed 25 test garments (empty wardrobe only) |
| GET | `/wardrobe/{id}` | `garment-detail.html` | Detail + compatible + companion |
| GET | `/wardrobe/{id}/edit` | `garment-edit.html` | Edit form |
| PUT | `/wardrobe/{id}` | redirect:/wardrobe/{id} OR garment-edit | Update (return edit form on validation errors) |
| DELETE | `/wardrobe/{id}?source=` | `""` (empty body) | Delete; `HX-Redirect: /wardrobe` when source=detail |
| POST | `/wardrobe/{id}/favorite?variant=` | `wardrobe :: grid` OR `garment-detail :: favDetailButton` | Toggle favorite, variant=card|detail |
| GET | `/wardrobe/new` | `garment-new.html` | Upload form |
| GET | `/inspiration` | `inspiration.html` | 6 predefined looks with tags |
| GET | `/recommendation` | `recommendation.html` | AI-generated outfit suggestions |
| GET | `/weekly-plan` | `weekly-plan.html` | Day-by-day planning |
| POST | `/weekly-plan/assign` | `""` (empty body) | Assign garment to day |
| DELETE | `/weekly-plan/{id}` | `""` (empty body) | Remove from plan |
| PUT | `/weekly-plan/reorder?dayOfWeek=&order=` | `""` (empty body) | Reorder garments in a day |
| GET | `/profile` | `profile-stats.html` | Stats, top colors, usage |

- `GarmentController.detail()` now computes and passes `garmentSeason` (ColorSeason displayName) to the view for the season badge

### Colorín Companion API

All endpoints in `CompanionChatApiController.java` (prefix `/api/companion`).

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/companion/sessions` | List all sessions |
| POST | `/api/companion/sessions` | Create new session |
| PATCH | `/api/companion/sessions/{id}` | Rename session title |
| DELETE | `/api/companion/sessions/{id}` | Delete session and cascade messages |
| GET | `/api/companion/sessions/{id}/messages` | Get all messages for session |
| POST | `/api/companion/sessions/{id}/messages` | Send message (SSE streaming response) |
| GET | `/api/companion/stream/{runId}` | Stream a companion run over SSE |
| POST | `/api/companion/sessions/{id}/messages/{msgId}/feedback` | Submit feedback for a message |
| POST | `/api/companion/messages/{msgId}/feedback` | Backward-compatible feedback route |
| GET | `/api/companion/context` | Get contextual wardrobe summary and styling tips |
| GET | `/api/companion/tips` | Alias for contextual wardrobe summary and styling tips |

---

## 5. Coding Conventions

### Java
- Records for DTOs and `@ConfigurationProperties`
- Prefer constructor injection (single constructor = implicit `@Autowired`)
- `@Cacheable` with `unless = "#result.error() != null"` on AI services to avoid caching errors
- `@Valid` on controller params, `BindingResult` immediately after the model attribute
- `@PrePersist` / `@PreUpdate` lifecycle callbacks for timestamp management
- `Map.ofEntries()` for maps with 11+ entries (`Map.of()` caps at 10)

### Templates (Thymeleaf + HTMX)
- UI copy in Spanish (español de España)
- CSRF via `<meta>` tags in `head.html` + `htmx:configRequest` event handler for HTMX requests
- Normal browser forms include hidden `<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />` — required in `garment-new.html`, `garment-confirm.html`, `garment-edit.html`, and `wardrobe.html`
- HTMX fragments use stable wrapper IDs
- `th:attr` with multiple HTMX attributes needs careful quoting
- Layout via `th:replace` on `layout.html` with fragment parameters

### CSS
- Design tokens in `tokens.css` as CSS custom properties on `:root`
- CSS `var()` inside `@media()` queries is **INVALID** — hardcode breakpoint values (600px, 820px, 1024px, 1366px, 1440px, 1920px)
- Mobile-first responsive in `responsive.css`
- Breakpoints: 600 (small tablet), 820 (tablet portrait), 1024 (tablet landscape), 1366 (laptop), 1440 (desktop), 1920 (wide)

### Colorimetry
- Classification is deterministic O(1) math on existing `colorHex` — no DB migration needed
- `ColorSeasonClassifier.classify()` returns `ColorProfile` with season, temperature, intensity, depth, confidence
- `ColorCompatibilityEngine.score()` returns `CompatibilityResult` with score (0-100), harmony, explanation, warnings
- All configurable via `ColorimetryProperties` — weights, sigmoid parameters, thresholds
- Nearest-neighbor fallback uses CIEDE76 on CIELAB values

### Documentation
- Documentation is part of the change, not an afterthought: update `README.md` in the same PR whenever behavior, setup, routes, security, validation, dependencies, or testing commands change.
- Update `.env.example` whenever environment variables are added, renamed, removed, or their safe defaults change.
- Update this `AGENTS.md` when a project convention, gotcha, architecture decision, or workflow rule should survive into future AI sessions.
- Keep README reader-first: happy path first, then architecture, security, operations, validation, and troubleshooting.

---

## 6. Testing Conventions

### Database
- **ALL tests use H2 in-memory** — NEVER touch the shared PostgreSQL on port 55432
- Test profile: `src/test/resources/application-test.yml` uses H2 (`jdbc:h2:mem:testdb`), `ddl-auto: create-drop`, Flyway disabled
- `@DataJpaTest` tests use `@ActiveProfiles("test")` with `@AutoConfigureTestDatabase(replace = NONE)` and connect via the test profile's H2 datasource
- If tests drop tables in the shared PostgreSQL, recover with: `mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:55432/ropa -Dflyway.user=ropa -Dflyway.password=ropa`

### Controller Tests
- `@WebMvcTest(GarmentController.class)` with `@AutoConfigureMockMvc(addFilters = false)` — CSRF is NOT tested in controller tests
- Use `@MockitoBean` (Spring Boot 3.4+) for mocking dependencies
- Mock `RateLimitingInterceptor` to prevent interceptor from blocking requests
- Use `SecurityMockMvcRequestPostProcessors.csrf()` when CSRF token is needed

### Service Tests
- `@TempDir` for file-based tests (image upload, thumbnail)
- WireMock on random port (`new WireMockServer(0)`), stop in `@AfterEach`
- AI service tests mock the HTTP endpoint, NOT WebClient itself

### Test Statistics
- **57 test classes**, ~490 `@Test` methods
- Run with: `mvn test`
- Surefire loads Mockito as a Java agent; keep this when running on JDKs that block Mockito/Byte Buddy self-attach

---

## 7. Security Model

- **CSRF-only** — no authentication, no login, no sessions
- `CsrfTokenRequestAttributeHandler` (NOT the default `XorCsrfTokenRequestAttributeHandler`) — required for HTMX compatibility because HTMX doesn't send the XOR-encoded token format
- CSRF token injected via `<meta name="_csrf">` and `<meta name="_csrf_header">` in `head.html`
- `htmx:configRequest` JavaScript event attaches CSRF header to every HTMX request
- Magic bytes validation on image upload (JPEG: `FF D8 FF`, PNG: `89 50 4E 47`, WebP: `RIFF....WEBP`)
- Path traversal prevention in `AiClassificationService.resolveImagePath()` — validates prefix `/uploads/` and normalizes path
- **Anonymous owner isolation via opaque token**: Cookie stores an opaque `owner_token` (not the DB UUID `owner_id`). Server hashes it (SHA-256) and persists `token_hash` in `anonymous_owners`. Prevents owner takeover via known UUID bootstrap sequence.
  - Cookie flags: `HttpOnly`, `SameSite=Lax`, `Secure` (with `X-Forwarded-Proto=https` support)
  - Legacy `owner_id` cookie is ignored
- **Admin protection**: `AdminProperties` (`app.admin.token`) configures an optional token. Endpoints under `/api/admin/**` and `/admin/**` are protected via `SecurityConfig` — require `X-Admin-Token` header matching the configured value. If no token is set, admin endpoints are effectively closed.
- Rate limiting via `HandlerInterceptor` + Caffeine (NOT Bucket4j):
  - Keys are **per-endpoint** (`endpointKey:ip`) to avoid bucket collisions
  - `POST /wardrobe/analyze`: 10/hour per IP → throws `RateLimitExceededException` → `GlobalExceptionHandler` → `error.html`
  - `GET /recommendation`: 5/30min per IP
  - Companion API feedback POST/PATCH/DELETE covered
- SRI integrity hashes on external scripts (HTMX)

---

## 8. Database

- PostgreSQL 16 Alpine via `docker-compose.yml` on port **55432** (avoids conflict with local 5432)
- Flyway migrations in `src/main/resources/db/migration/`:
  - `V1__create_garments.sql` — garments table with indexes on category, color_name, user_confirmed, created_at
  - `V2__add_favorite_to_garments.sql` — add favorite boolean column
  - `V3__create_week_plans.sql` — week_plans table with FK to garments (ON DELETE CASCADE), indexes on day+position and garment_id
  - `V4__seed_test_data.sql` — deprecated no-op; seed demo data through guarded `/wardrobe/seed` only
  - `V10__add_chat_session_surface.sql` — add surface column to chat_sessions for companion isolation
  - `V13__improve_chat_sessions_main_chat_index.sql` — composite index improvements
  - `V14__drop_legacy_chat_sessions_owner_index.sql` — drop deprecated index
  - `V15__add_message_id_to_chat_feedback.sql` — link feedback directly to message
  - `V16__add_chat_sessions_active_updated_at_index.sql` — index for active sessions query
  - `V17__add_anonymous_owner_token_hash.sql` — add `token_hash` column to `anonymous_owners`, create index, backfill bootstrap owner hash
  - `V18__enforce_week_plan_ordering.sql` — add `UNIQUE (day_of_week, position)` deferred constraint, reindex positions, add composite index
  - `V19__link_chat_messages_to_runs.sql` — add `run_id` FK to `chat_messages`, backfill from chat_runs, index
  - `V20__drop_chat_session_model_default.sql` — remove DEFAULT from `chat_sessions.model`, model resolved via ModelRouter now
- `ddl-auto: validate` in production — Flyway manages schema
- `open-in-view: false` — explicit transaction boundaries in services
- Hidden HTTP method filter enabled (`spring.mvc.hiddenmethod.filter.enabled: true`) for PUT/DELETE via forms

---

## 9. Key Decisions (with rationale)

| Decision | Rationale |
|---|---|
| **Caffeine over Bucket4j** for rate limiting | Single-user app; concurrent HashMap + TTL sufficient; zero new dependencies |
| **CsrfTokenRequestAttributeHandler over XOR default** | HTMX doesn't send XOR-encoded tokens; default handler breaks all HTMX POST/PUT/DELETE |
| **H2 over Testcontainers** for tests | Faster startup, no Docker dependency, no Docker-in-CI complexity |
| **Map.ofEntries() over Map.of()** for 11+ entries | `Map.of()` has a 10-entry hard limit, throws on 11th |
| **Static Docker container over Testcontainers** for dev | Simpler setup, pre-configured healthcheck, no Java wrapper |
| **Rate limit strategy**: IP-based per endpoint via HandlerInterceptor | Throws `RateLimitExceededException` → `GlobalExceptionHandler` → `error.html` view |
| **`ddl-auto: validate` in production** | Flyway owns the schema; Hibernate must not auto-create or modify it |
| **`@Cacheable` with `unless = "#result.error() != null"`** | Prevents caching of AI error responses that should retry next time |
| **Opaque `owner_token` over UUID `owner_id`** | Prevents owner takeover via known UUID bootstrap sequence; SHA-256 hash stored server-side, raw token in cookie only |
| **Admin token via `X-Admin-Token` header** | Explicit token required for admin endpoints; no token = admin closed, no reliance on `remoteAddr` |
| **Disable default generated user auto-config** | Security is CSRF-only plus custom admin token checks; excluding `UserDetailsServiceAutoConfiguration` avoids unused generated password logs |
| **Rate limit keys per endpoint** (`endpointKey:ip`) | Prevents bucket collisions across different rate limit windows; 10/h analyze vs 5/30min recommendation use separate buckets |
| **ModelRouter for session model resolution** | No more hardcoded `"qwen3.6"` — sessions resolve via `ModelRouter`; UI sends `""`, backend converts to null → default model; V20 drops the `DEFAULT` on `chat_sessions.model` |
| **WardrobeContextAssembler stateless cache** | Mutable `classificationCache` field removed; cache is now method-local and passed through helpers — thread-safe by design, no shared mutable state |
| **CSRF hidden inputs in normal forms** | Browser form submissions (`garment-new.html`, `garment-confirm.html`, `garment-edit.html`, `wardrobe.html`) include hidden CSRF fields; HTMX handles CSRF via headers independently |
| **Prod logging profile separation** | Logback root logger separated by profile: `prod` → JSON appender (logstash-logback-encoder), `!prod` → CONSOLE; avoids structured logging noise in dev |
| **Prompt injection boundaries via data markers** | Wardrobe data in `AiRecommendationService` is enclosed in `=== INICIO DATOS DE PRENDAS (NO CONFIABLE) ===` markers with clear warnings — prevents prompt injection from garment names/colors |
| **No seed data in Flyway migrations** | `V4__seed_test_data.sql` is intentionally a no-op so enabling Flyway cannot delete or insert user wardrobe data; use `/wardrobe/seed` for local demo data |

---

## 10. Gotchas & Common Mistakes

### Caching
- `@Cacheable` caches error responses unless you add `unless = "#result.error() != null"`
- AiClassificationService.classify() is `@Cacheable(cacheNames = "garment-classifications", key = "#imageUrl", unless = "#result.error() != null")`

### CSS
- `var(--bp-md)` inside `@media()` is INVALID CSS — hardcode the numeric value (e.g., `@media (min-width: 820px)`)
- All CSS custom properties for breakpoints in `tokens.css` (`--bp-sm: 600px`, etc.) are NOT usable in media queries

### Java
- `Map.of()` limited to **10 entries** — use `Map.ofEntries()` or `HashMap` for 11+

### HTMX
- Fragments need **stable wrapper IDs** (e.g., `#wardrobe-grid`, `#garment-grid`, `#favDetailButton`) or swapping breaks
- `th:attr` with multiple HTMX attributes needs careful quoting — test HTMX attributes after changes
- CSRF token must be injected via `<meta>` tags AND the `htmx:configRequest` JS event — both are required
- Normal browser forms still need hidden CSRF fields even though HTMX handles CSRF via headers

### Controller / Exception Handling
- `preHandle()` returning `false` in HandlerInterceptor **aborts the request without rendering any view** — throw an exception instead for error page rendering (done in `RateLimitingInterceptor`)
- `GlobalExceptionHandler` uses `mav.setStatus(...)` for explicit HTTP status codes on MVC error views — don't rely solely on `@ResponseStatus` for controller advice methods
- If `ChatConversationOrchestrator.persistAssistantMessageAndCompleteRun()` throws, the run is marked `failed` and a `stream-error` SSE event is sent — the run lifecycle must handle persistence failures gracefully
- REST controllers that accept `@RequestBody Map<String, String>` must handle a JSON `null` body explicitly before accessing fields; title rename endpoints return `invalid_request` instead of a 500.

### JPA
- `ChatMessage.content` uses `@Column(columnDefinition = "TEXT")` to match Flyway's schema — H2 create-drop now produces TEXT too; always align `columnDefinition` with the Flyway migration when using DDL-auto=create-drop in tests

### Tests
- **NEVER run tests against the shared PostgreSQL** — H2 with `create-drop` will destroy your data
- `@WebMvcTest` with `addFilters = false` bypasses Security filters — CSRF is NOT tested in controller tests
- Mock `RateLimitingInterceptor` in controller tests or it will block requests

### Companion JS
- Panel usa `position: fixed` como overlay sibling del root para que Playwright y el navegador midan el panel dentro del viewport real; viewport clipping via JS en `positionPanelForViewport()`
- El trigger se guarda en localStorage; `ensureRootInViewport()` debe mantenerse sincronizado con el panel fijo al redimensionar ventana

### Database
- If shared PostgreSQL tables get dropped: `mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:55432/ropa -Dflyway.user=ropa -Dflyway.password=ropa`
- `docker-compose.yml` reads `DB_PASSWORD` for `POSTGRES_PASSWORD`; keep it aligned with `spring.datasource.password`. Changing it after the local Docker volume exists may require a deliberate local DB reset or password update.
- Do not reintroduce tracked root DB probes with hardcoded credentials; `TestDb` is deprecated and must stay credential-free
- If local startup fails with `Migration checksum mismatch for migration version 4`, the database still has the old destructive seed migration checksum. Do not repair automatically; ask before running `mvn flyway:repair ...` because it mutates `flyway_schema_history`.

### Colorimetry
- `ColorSeasonClassifier` uses nearest-neighbor as fallback when the CIELAB matrix is ambiguous (NEUTRAL temperature or MEDIUM depth)
- The matrix can misclassify borderline colors (e.g., mostaza as SPRING instead of AUTUMN) — the nearest-neighbor override corrects this
- `ColorCompatibilityEngine` uses additive scoring (not weighted), so max score can exceed 100 — always clamp to [0, 100]
- `@Component` on `ColorPaletteStore` is required for Spring DI — it's NOT a plain utility class
- `ColorProfile.fromLab()` uses configurable `warmCoolThreshold` and `intensityThreshold` from `ColorimetryProperties`

---

## 11. Next Steps / Known Work

### Outstanding
- Fix resize bug in companion panel (ensureRootInViewport + panel max-height)
- Improve AI prompt to cover all 11 categories more consistently
- Document Playwright smoke test procedure
- Consider dark mode support (`prefers-color-scheme` in CSS)
- Keep CSRF integration coverage in `ChatSecurityIntegrationTest` when adding new state-changing chat endpoints
- AI recommendation parse error handling could be more robust

---

## 12. Agent Usage Patterns

For different task types, delegate to the appropriate agent:

| Task Type | Agent | Example |
|---|---|---|
| Java infrastructure, config, services, repos | Backend Architect | Adding a new service, changing JPA config |
| Thymeleaf templates, HTMX, CSS, accessibility | Frontend Developer | New view, fragment refactor, responsive fix |
| MockMvc, WireMock, Mockito tests | API Tester | New controller test, fixing flaky test |
| README, AGENTS.md, documentation | Technical Writer | Updating this file |
| Codebase research, pattern discovery | Explore | Finding all uses of a pattern |
| Multi-step tasks spanning files | General | Feature that touches controller + service + template |

When tasks don't share files, launch agents in **parallel**.

---

## 13. Data Model

### Garment
```
id: Long (PK, auto)
name: String(120) NOT NULL
category: String(50) NOT NULL     — one of 11 categories
colorName: String(50) NOT NULL
colorHex: String(7)               — #RRGGBB
material: String(80)
season: String(50)
imageUrl: String(500) NOT NULL    — /uploads/<uuid>.jpg
aiType: String(50)                — raw AI type prediction
aiColorName: String(50)
aiColorHex: String(7)
aiConfidence: BigDecimal(3,2)
aiModel: String(80)
favorite: boolean NOT NULL
userConfirmed: boolean NOT NULL
createdAt: OffsetDateTime NOT NULL
updatedAt: OffsetDateTime NOT NULL
```

### WeekPlan
```
id: Long (PK, auto)
garment: @ManyToOne(LAZY) → Garment  (ON DELETE CASCADE)
dayOfWeek: String(10) NOT NULL       — Lunes, Martes, ...
position: int NOT NULL               — ordering within a day
createdAt, updatedAt: OffsetDateTime
```
`UNIQUE (day_of_week, position)` deferred constraint on PostgreSQL (V18).

### AnonymousOwner
```
id: UUID (PK, auto)
tokenHash: String(64) NOT NULL       — SHA-256 hash of opaque owner_token (V17)
bootstrap: boolean NOT NULL DEFAULT FALSE
createdAt: OffsetDateTime NOT NULL
updatedAt: OffsetDateTime NOT NULL
```

### ChatMessage
```
id: UUID (PK, auto)
runId: UUID FK → chat_runs           — run that produced this message (V19)
sessionId: UUID FK → chat_sessions
role: String(50) NOT NULL
content: TEXT NOT NULL
...
```

> **Note**: `colorSeason` is computed on-the-fly via `ColorSeasonClassifier` and stored in DTOs (`GarmentSummary`, `ColorInfo`, `OutfitPiece`) but NOT in the database.

### Categories (11)
`Top`, `Pantalón`, `Vestido`, `Falda`, `Chaqueta`, `Abrigo`, `Camisa`, `Sudadera`, `Zapatos`, `Accesorio`, `Otro`

### Days (7)
`Lunes`, `Martes`, `Miercoles`, `Jueves`, `Viernes`, `Sabado`, `Domingo`

---

## 14. Configuration Properties

### `app.wardrobe` (WardrobeProperties)
- `categories`: List of 11 category strings
- `days`: List of 7 day strings
- `color-limit`: int (default 5)

### `app.ai` (AiServerProperties)
- `enabled`: boolean (default true)
- `base-url`, `chat-path`, `model`, `api-key`
- `max-tokens`: int (default 500)
- `connect-timeout`, `read-timeout`: Duration

### `app.upload` (UploadProperties)
- `directory`: Path (default `uploads/`)
- `max-size`: DataSize (default 8MB)
- `allowed-content-types`: List (JPEG, PNG, WebP)

### `app.rate-limit` (RateLimitProperties)
- `analyze.capacity`: int (default 10)
- `analyze.refill-minutes`: int (default 60)
- `recommendation.capacity`: int (default 5)
- `recommendation.refill-minutes`: int (default 30)

### `app.chat.retention` (ChatRetentionProperties)
- `analytics-events-days`: int (default 90)
- `session-inactive-days`: int (default 180)
- `orphan-upload-cleanup`: boolean (default false)

### `app.admin` (AdminProperties)
- `token`: String (optional, default empty — admin endpoints closed)

---

## 15. Dev Setup

```bash
# Start database
docker compose up -d postgres

# Set AI API key (optional for dev, disable in config to skip)
export APP_AI_API_KEY=replace-with-your-api-key

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Open browser at http://localhost:8081
```
