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
| Framework | Spring Boot 3.4.5 |
| Build | Maven |
| Templates | Thymeleaf (server-side, NOT REST API) |
| Interactivity | HTMX 2.0.4 (partial page updates, fragments) |
| Database | PostgreSQL 16 Alpine (port 55432) |
| Migrations | Flyway |
| Security | Spring Security 6 (CSRF only — NO authentication) |
| Cache | Caffeine |
| HTTP client | WebClient (reactive, for AI API calls) |
| Image processing | Thumbnailator + TwelveMonkeys WebP |
| Test DB | H2 (in-memory) |
| Test HTTP mock | WireMock 3.9.1 |
| Test frameworks | Mockito, MockMvc, @DataJpaTest, Playwright (E2E) |

---

## 3. Architecture

### Layered MVC
```
Controller (GarmentController) → Service (6) → Repository (2 JPA repos)
                                ↓
                           DTOs (7)
```

### Component Inventory

| Type | Count | Details |
|---|---|---|
| Controllers | 1 | `GarmentController` (17 endpoint methods) |
| Services | 6 | `GarmentService`, `GarmentCompatibilityService`, `AiClassificationService`, `AiRecommendationService`, `WeekPlanService`, `InspirationService` |
| Storage | 1 interface + 1 impl | `ImageStorageService` / `LocalImageStorageService` |
| Repositories | 2 | `GarmentRepository`, `WeekPlanRepository` |
| DTOs | 7 | `GarmentReviewForm`, `DashboardStats`, `AiClassificationResponse`, `AiRecommendationResponse`, `OutfitSuggestion`, `OutfitPiece`, `InspirationLook` |
| @ConfigurationProperties | 4 | `WardrobeProperties`, `UploadProperties`, `AiServerProperties`, `RateLimitProperties` |
| Config | 5+ | `SecurityConfig`, `WebMvcConfig`, `WebClientConfig`, `AiServerPropertiesValidator`, `GlobalExceptionHandler` |

### Patterns
- All config via `@ConfigurationProperties` records
- `GlobalExceptionHandler` (`@ControllerAdvice`) catches `IllegalArgumentException`, `SecurityException`, `RateLimitExceededException`, `Exception`
- Layout decorator pattern with `th:replace` on `layout.html`
- HTMX fragments: stable wrapper IDs (e.g., `#wardrobe-grid`, `#garment-grid`, `#favDetailButton`)
- CSS design tokens via custom properties (`tokens.css`)

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
- CSRF via `<meta>` tags in `head.html` + `htmx:configRequest` event handler
- HTMX fragments use stable wrapper IDs
- `th:attr` with multiple HTMX attributes needs careful quoting
- Layout via `th:replace` on `layout.html` with fragment parameters

### CSS
- Design tokens in `tokens.css` as CSS custom properties on `:root`
- CSS `var()` inside `@media()` queries is **INVALID** — hardcode breakpoint values (600px, 820px, 1024px, 1366px, 1440px, 1920px)
- Mobile-first responsive in `responsive.css`
- Breakpoints: 600 (small tablet), 820 (tablet portrait), 1024 (tablet landscape), 1366 (laptop), 1440 (desktop), 1920 (wide)

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
- **12 test classes**, 118 `@Test` methods
- Run with: `mvn test`

---

## 7. Security Model

- **CSRF-only** — no authentication, no login, no sessions
- `CsrfTokenRequestAttributeHandler` (NOT the default `XorCsrfTokenRequestAttributeHandler`) — required for HTMX compatibility because HTMX doesn't send the XOR-encoded token format
- CSRF token injected via `<meta name="_csrf">` and `<meta name="_csrf_header">` in `head.html`
- `htmx:configRequest` JavaScript event attaches CSRF header to every HTMX request
- Magic bytes validation on image upload (JPEG: `FF D8 FF`, PNG: `89 50 4E 47`, WebP: `RIFF....WEBP`)
- Path traversal prevention in `AiClassificationService.resolveImagePath()` — validates prefix `/uploads/` and normalizes path
- Rate limiting via `HandlerInterceptor` + Caffeine (NOT Bucket4j):
  - `POST /wardrobe/analyze`: 10/hour per IP → throws `RateLimitExceededException` → `GlobalExceptionHandler` → `error.html`
  - `GET /recommendation`: 5/30min per IP
- SRI integrity hashes on external scripts (HTMX)

---

## 8. Database

- PostgreSQL 16 Alpine via `docker-compose.yml` on port **55432** (avoids conflict with local 5432)
- Flyway migrations in `src/main/resources/db/migration/`:
  - `V1__create_garments.sql` — garments table with indexes on category, color_name, user_confirmed, created_at
  - `V2__add_favorite_to_garments.sql` — add favorite boolean column
  - `V3__create_week_plans.sql` — week_plans table with FK to garments (ON DELETE CASCADE), indexes on day+position and garment_id
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
- `preHandle()` returning `false` in HandlerInterceptor **aborts the request without rendering any view** — throw an exception instead for error page rendering (done in `RateLimitingInterceptor`)

### HTMX
- Fragments need **stable wrapper IDs** (e.g., `#wardrobe-grid`, `#garment-grid`, `#favDetailButton`) or swapping breaks
- `th:attr` with multiple HTMX attributes needs careful quoting — test HTMX attributes after changes
- CSRF token must be injected via `<meta>` tags AND the `htmx:configRequest` JS event — both are required

### Tests
- **NEVER run tests against the shared PostgreSQL** — H2 with `create-drop` will destroy your data
- `@WebMvcTest` with `addFilters = false` bypasses Security filters — CSRF is NOT tested in controller tests
- Mock `RateLimitingInterceptor` in controller tests or it will block requests

### Database
- If shared PostgreSQL tables get dropped: `mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:55432/ropa -Dflyway.user=ropa -Dflyway.password=ropa`

---

## 11. Next Steps / Known Work

### Sprint 3 (current/upcoming)
- Compatibility scoring bar (visual indicator in detail view)
- Insights section on dashboard
- Color palette bar in profile/detail

### Outstanding
- Improve AI prompt to cover all 11 categories more consistently
- Document Playwright smoke test procedure
- Consider dark mode support (`prefers-color-scheme` in CSS)
- Add integration test for CSRF behavior (currently not covered in controller tests)
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

---

## 15. Dev Setup

```bash
# Start database
docker compose up -d postgres

# Set AI API key (optional for dev, disable in config to skip)
export APP_AI_API_KEY=sk-your-key

# Run application
mvn spring-boot:run

# Run tests
mvn test

# Open browser at http://localhost:8080
```
