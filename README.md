# Armario Cápsula

Aplicación local en Java 21 + Spring Boot 3 para gestionar un armario cápsula: cargar prendas desde imagen, sugerir tipo y color con un servidor IA propio, confirmar los datos antes de guardarlos, planificar la semana, recibir recomendaciones de outfits y chatear con un asistente de moda con streaming IA.

## Stack

| Capa             | Tecnología                                |
|------------------|-------------------------------------------|
| Backend          | Java 21, Spring Boot 3.4.5                |
| Build            | Maven                                     |
| UI               | Thymeleaf mobile-first + HTMX             |
| DB               | PostgreSQL 16 + Flyway                     |
| IA visión        | Servidor local OpenAI-compatible           |
| IA chat          | Streaming SSE con WebClient reactivo       |
| Almacenamiento   | Filesystem local en `uploads/`             |
| Caché            | Caffeine (classification en 24h, rate limiting, analytics buffer) |
| Testing          | JUnit 5, Mockito, WireMock, H2, TestContainers |
| Observabilidad   | Spring Boot Actuator, logstash-logback-encoder (opcional) |

## Índice

- [Arquitectura](#arquitectura)
- [Subsistema de chat](#subsistema-de-chat)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Modelo de datos](#modelo-de-datos)
- [Rutas completas](#rutas-completas)
- [Configuración de referencia](#configuración-de-referencia)
- [Ready for production](#ready-for-production)
- [Levantar local](#levantar-local)
- [Flujo implementado](#flujo-implementado)
- [Guía de testing](#guía-de-testing)
- [Workflow de desarrollo](#workflow-de-desarrollo)
- [Solución de problemas](#solución-de-problemas)

---

## Arquitectura

La aplicación sigue una arquitectura MVC clásica con controladores, servicios y repositorios, organizada como un **monolito modular** con 6+1 tracks funcionales.

### Capas

```
┌──────────────────────────────────────────────────────────┐
│                       Controller                           │
│   GarmentController (17 endpoints)   │  FashionChatController │
│   ChatApiController (REST+SSE)      │  ChatMetricsController │
├──────────────────────────────────────────────────────────┤
│                        Service                             │
│   GarmentService / WeekPlanService                        │
│   AiClassificationService / AiRecommendationService       │
│   GarmentCompatibilityService / InspirationService        │
│   ChatSessionService / ChatMessageService / ChatRunService│
│   ChatFeedbackService / ChatPolicyService                 │
│   ChatPromptFactory / WardrobeContextAssembler             │
│   ChatIntentClassifier / ModelRouter / StreamingChatClient │
│   ChatAnalyticsService / ChatMetricsService               │
│   ChatDataRetentionService / AnonymousOwnerService         │
├──────────────────────────────────────────────────────────┤
│                      Repository                            │
│   GarmentRepository / WeekPlanRepository                  │
│   ChatSessionRepository / ChatMessageRepository            │
│   ChatRunRepository / ChatFeedbackRepository               │
│   ChatAnalyticsEventRepository / AnonymousOwnerRepository  │
├──────────────────────────────────────────────────────────┤
│                      Model / DTO                            │
│   Garment (JPA) / WeekPlan (JPA) / AnonymousOwner (JPA)   │
│   ChatSession / ChatMessage / ChatRun / ChatFeedback       │
│   ChatAnalyticsEvent                                       │
│   7 DTOs para boundaries de API (originales)               │
│   12+ DTOs para chat (WardrobeContext, StreamChunk, etc.)  │
├──────────────────────────────────────────────────────────┤
│                       Config                                │
│   12 @ConfigurationProperties    │  SecurityConfig          │
│   WebMvcConfig / WebClientConfig │  AsyncConfig             │
│   GlobalExceptionHandler / ApiExceptionHandler              │
│   RateLimitingInterceptor / HealthConfig / CurrentOwnerFilter│
└──────────────────────────────────────────────────────────┘
```

### 6+1 Tracks

| Track | Área | Descripción |
|-------|------|-------------|
| **A** | Anonymous Ownership | Sesiones de propietario sin autenticación (cookie-based). Cada navegador obtiene un `owner_id` UUID persistente. |
| **B** | Chat Persistence | Sesiones de chat, mensajes, runs (intentos de ejecución) y feedback. Todo con owner isolation. |
| **C** | AI Streaming Gateway | SSE bidireccional con WebClient reactivo. Soporte multi-modelo (Gemma 4, Qwen 3.6, DeepSeek V4 Flash). |
| **D** | Wardrobe Context + Policy Engine | Contexto del guardarropa ensamblado en tiempo real. Policy engine con clasificación de intención (regex) y rate limiting. |
| **E** | Premium UI | Página de chat Thymeleaf + HTMX, endpoints REST + SSE, feedback loop. |
| **F** | Analytics + Observability | Eventos batch asíncronos, métricas operativas en memoria, logging estructurado. |
| **G** | Production Hardening | Actuator health endpoints, data retention programada, rate limiting granular, security headers (CSP), structured logging opcional. |

**Controller**: `GarmentController` con 17 endpoints — maneja toda la interacción HTTP de gestión de prendas, usa Thymeleaf para vistas y fragmentos HTMX. `FashionChatController` (2 endpoints) para la interfaz de chat Thymeleaf. `ChatApiController` (8 endpoints) para la API REST/SSE del chat. `ChatMetricsController` (1 endpoint) para métricas operativas.

**Service layer**: 14 servicios divididos en:
- **Gestión de armario**: `GarmentService`, `AiClassificationService`, `AiRecommendationService`, `GarmentCompatibilityService`, `WeekPlanService`, `InspirationService`
- **Chat core**: `ChatSessionService`, `ChatMessageService`, `ChatRunService`, `ChatFeedbackService`, `ChatPolicyService`, `ChatPromptFactory`, `WardrobeContextAssembler`, `ChatIntentClassifier`, `ModelRouter`, `StreamingChatClient`
- **Infraestructura**: `ChatAnalyticsService`, `ChatMetricsService`, `ChatDataRetentionService`, `AnonymousOwnerService`

**Repository layer (8 JPA repositories)**: Los 2 originales (`GarmentRepository`, `WeekPlanRepository`) más `ChatSessionRepository`, `ChatMessageRepository`, `ChatRunRepository`, `ChatFeedbackRepository`, `ChatAnalyticsEventRepository`, `AnonymousOwnerRepository`.

**Config layer (17 clases)**: Las 11 originales más `ApiExceptionHandler`, `AsyncConfig`, `HealthConfig`, `AiModelConfig`, `CurrentOwnerFilter`, `ChatAnalyticsEventRepository` (count methods).

---

## Subsistema de chat

### Flujo end-to-end

```
Navegador                     Servidor                      AI Provider
   │                             │                              │
   │  GET /chat                  │                              │
   │────────────────────────────>│                              │
   │   Chat page (Thymeleaf)     │                              │
   │<────────────────────────────│                              │
   │                             │                              │
   │  POST /api/chat/sessions/{id}/messages                    │
   │────────────────────────────>│                              │
   │  { content: "qué me pongo?" }                              │
   │                             │                              │
   │                  ┌──────────┴──────────┐                  │
   │                  │ Policy Evaluation   │                  │
   │                  │ ─ Clasificar intento│                  │
   │                  │ ─ Rate limit check  │                  │
   │                  │ ─ BLOCK/FLAG/ALLOW  │                  │
   │                  └──────────┬──────────┘                  │
   │                             │                              │
   │                  Persist user message                      │
   │                  Create ChatRun (pending→running)           │
   │                  Return { runId, sessionId }               │
   │<────────────────────────────│                              │
   │                             │                              │
   │  GET /api/chat/stream/{runId} (SSE)                       │
   │────────────────────────────>│                              │
   │                             │  POST /v1/chat/completions   │
   │                             │  (stream: true)              │
   │                             │─────────────────────────────>│
   │                             │                              │
   │  event: chunk               │  data: {"choices":[{"delta": │
   │  "recomiendo..."   ◄────────│  {"content":"recomiendo..."}}]│
   │                             │                              │
   │  event: chunk               │        ...                   │
   │  "un look..."      ◄────────│                              │
   │                             │                              │
   │  event: done                │                              │
   │  { messageId }     ◄────────│                              │
   │                             │                              │
   │                  Persist assistant message + complete run  │
   │  POST /api/chat/runs/{id}/feedback                        │
   │────────────────────────────>│                              │
   │  { rating: "helpful" }      │                              │
   │<────────────────────────────│                              │
```

### Arquitectura de 4 controladores

| Controlador | Path base | Tipo | Propósito |
|-------------|-----------|------|-----------|
| `FashionChatController` | `/chat` | Thymeleaf MVC | Renderiza la página de chat con sesiones, modelos y contexto del armario |
| `ChatApiController` | `/api/chat` | REST + SSE | API JSON para sesiones, mensajes, streaming SSE y feedback |
| `StreamingChatClient` | (servicio) | Reactivo | Cliente WebClient que consume SSE del AI provider y emite `Flux<String>` |
| `ChatMetricsController` | `/api/admin/metrics` | REST | Expone métricas operativas (solo acceso localhost) |

### Wardrobe Context Assembly

Cada vez que se renderiza la página de chat, `WardrobeContextAssembler` construye un snapshot en tiempo real del guardarropa del owner:

- **Total de prendas**, desglose por categoría
- **Top N colores** por cantidad de prendas
- **Materiales** más frecuentes
- **Temporadas** representadas
- **Plan semanal**: día actual + próximos días (incluye nombres de prendas)
- **Favoritos** count

Este contexto se inyecta en el system prompt del modelo para que el asistente conozca el armario del usuario sin necesidad de RAG externo.

### Policy Engine

`ChatPolicyService` evalúa cada mensaje antes de enviarlo al AI provider:

1. **Clasificación de intención** (`ChatIntentClassifier`): usa regex para detectar si el mensaje es una consulta sobre el armario, un pedido de styling, advice de color, un pedido de outfit definitivo, o charla general.
2. **Bloqueo de outfit requests**: si el usuario pide elegir un outfit por él, se bloquea con un mensaje amigable que redirige la conversación.
3. **Rate limiting**: límite de 30 mensajes por minuto por owner en la policy layer (adicional al rate limiting HTTP del interceptor).
4. **Flagging**: pedidos de styling/color se marcan como FLAG para analytics pero se permiten.

### Analytics Pipeline

El sistema de analytics usa un buffer asíncrono thread-safe:

```
Request path → ChatAnalyticsService.recordEvent() → buffer (ConcurrentLinkedQueue)
                                                          │
                                            ┌─────────────┴─────────────┐
                                            │ Size trigger (10 events)  │
                                            │ Time trigger (30 seg)     │
                                            └─────────────┬─────────────┘
                                                          │
                                            @Async("analyticsTaskExecutor")
                                                          │
                                                     saveAll(batch)
```

**Eventos registrados**: `SESSION_CREATED`, `STREAM_STARTED`, `STREAM_COMPLETED`, `STREAM_DISCONNECTED`, `RUN_STARTED`, `RUN_COMPLETED`, `RUN_FAILED`, `POLICY_BLOCK`, `POLICY_FLAG`, `RATE_LIMIT_HIT`, `FEEDBACK_SUBMITTED`.

**Métricas operativas** (`ChatMetricsService`): contadores atómicos en memoria para `sessions.created`, `streams.completed`, `policy.blocks`, `tokens.total`, `latency.avg_ms`, etc. Expuestas vía `GET /api/admin/metrics`.

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/colorinchi/app/
│   │   ├── ColorinchiApplication.java          # Entry point (@EnableCaching, @EnableRetry, @EnableAsync, @EnableScheduling)
│   │   ├── config/
│   │   │   ├── AiServerProperties.java         # app.ai.* — servidor IA
│   │   │   ├── AiServerPropertiesValidator.java # Validación al startup
│   │   │   ├── AiModelConfig.java             # Record para modelos múltiples
│   │   │   ├── ApiExceptionHandler.java       # @RestControllerAdvice con RFC 7807 ProblemDetail
│   │   │   ├── AsyncConfig.java               # analyticsTaskExecutor (core 2, max 4)
│   │   │   ├── CurrentOwnerFilter.java        # Filtro que propaga owner_id desde cookie
│   │   │   ├── GlobalExceptionHandler.java    # @ControllerAdvice global para vistas
│   │   │   ├── HealthConfig.java              # AI provider health indicator custom
│   │   │   ├── RateLimitExceededException.java # Excepción 429
│   │   │   ├── RateLimitingInterceptor.java   # Rate limiter por IP + owner con Caffeine
│   │   │   ├── RateLimitProperties.java       # app.rate-limit.*
│   │   │   ├── SecurityConfig.java            # CSRF + CSP headers + localhost-restricted admin
│   │   │   ├── UploadProperties.java          # app.upload.*
│   │   │   ├── WardrobeProperties.java        # app.wardrobe.*
│   │   │   ├── WebClientConfig.java           # WebClient reactivo
│   │   │   └── WebMvcConfig.java              # Recursos estáticos + interceptores
│   │   ├── controller/
│   │   │   ├── GarmentController.java         # 17 endpoints MVC (prendas, plan semanal)
│   │   │   ├── FashionChatController.java     # 2 endpoints Thymeleaf (chat page)
│   │   │   ├── ChatApiController.java         # 8 endpoints REST/SSE
│   │   │   └── ChatMetricsController.java     # 1 endpoint métricas admin
│   │   ├── dto/
│   │   │   ├── (originales)                   # 7 DTOs para prendas
│   │   │   └── chat/
│   │   │       ├── ChatSessionResponse.java
│   │   │       ├── ChatMessageResponse.java
│   │   │       ├── ChatRunResponse.java
│   │   │       ├── ChatSendResponse.java
│   │   │       ├── ChatFeedbackRequest.java
│   │   │       ├── CreateSessionRequest.java
│   │   │       ├── ErrorResponse.java
│   │   │       ├── StreamChunk.java
│   │   │       ├── PolicyDecision.java
│   │   │       ├── ValidationResult.java
│   │   │       ├── WardrobeContext.java
│   │   │       ├── CategoryInfo.java
│   │   │       ├── ColorInfo.java
│   │   │       ├── MaterialInfo.java
│   │   │       └── DailyPlanInfo.java
│   │   ├── model/
│   │   │   ├── Garment.java                   # Entidad JPA: prendas
│   │   │   ├── WeekPlan.java                  # Entidad JPA: planificación semanal
│   │   │   ├── AnonymousOwner.java            # Entidad JPA: ownership tracking
│   │   │   ├── ChatSession.java               # Entidad JPA: sesiones de chat
│   │   │   ├── ChatMessage.java               # Entidad JPA: mensajes
│   │   │   ├── ChatRun.java                   # Entidad JPA: runs de ejecución
│   │   │   ├── ChatFeedback.java              # Entidad JPA: feedback
│   │   │   └── ChatAnalyticsEvent.java        # Entidad JPA: eventos de analytics
│   │   ├── repository/
│   │   │   ├── GarmentRepository.java
│   │   │   ├── WeekPlanRepository.java
│   │   │   ├── AnonymousOwnerRepository.java
│   │   │   ├── ChatSessionRepository.java
│   │   │   ├── ChatMessageRepository.java
│   │   │   ├── ChatRunRepository.java
│   │   │   ├── ChatFeedbackRepository.java
│   │   │   └── ChatAnalyticsEventRepository.java
│   │   ├── service/
│   │   │   ├── (originales)                   # 6 servicios de armario
│   │   │   ├── AnonymousOwnerService.java     # Creación/resolución de owners
│   │   │   ├── CurrentOwnerAccessor.java      # Acceso al owner desde el filter
│   │   │   ├── ChatSessionService.java        # CRUD de sesiones
│   │   │   ├── ChatMessageService.java        # Persistencia de mensajes
│   │   │   ├── ChatRunService.java            # Ciclo de vida de runs
│   │   │   ├── ChatFeedbackService.java       # Feedback de usuarios
│   │   │   ├── ChatPolicyService.java         # Policy engine + rate limiting
│   │   │   ├── ChatPromptFactory.java         # Construcción de system prompts
│   │   │   ├── ChatIntentClassifier.java      # Clasificación por regex
│   │   │   ├── ModelRouter.java               # Resolución de modelos
│   │   │   ├── WardrobeContextAssembler.java  # Snapshot del guardarropa
│   │   │   ├── StreamingChatClient.java       # Cliente SSE reactivo
│   │   │   ├── ProviderRequestFactory.java    # Construcción de requests al provider
│   │   │   ├── ProviderResponseParser.java    # Parsing de SSE del provider
│   │   │   ├── ChatResponseValidator.java     # Validación de respuestas
│   │   │   └── ChatDataRetentionService.java  # Cleanup programado
│   │   ├── service/analytics/
│   │   │   ├── ChatAnalyticsService.java      # Buffer batch + flush asíncrono
│   │   │   ├── ChatMetricsService.java        # Contadores atómicos in-memory
│   │   │   ├── ChatEventType.java             # Enum de tipos de evento
│   │   │   └── LogSanitizer.java             # Sanitización de logs
│   │   └── upload/
│   │       ├── ImageStorageService.java       # Interfaz de almacenamiento
│   │       └── LocalImageStorageService.java  # Implementación local con Thumbnailator
│   └── resources/
│       ├── application.yml                    # Configuración principal
│       ├── db/migration/
│       │   ├── V1__create_garments.sql        # Tabla garments + índices
│       │   ├── V2__add_favorite_to_garments.sql
│       │   ├── V3__create_week_plans.sql
│       │   ├── V4__seed_test_data.sql         # 70 prendas + 88 week plans de prueba
│       │   ├── V5__add_anonymous_owners.sql   # Tabla anonymous_owners + owner_id en garments/week_plans
│       │   ├── V6__create_chat_tables.sql     # Tablas chat_sessions, messages, runs, feedback, analytics
│       │   └── V7__add_chat_session_archived.sql # Columna archived + índices
│       └── templates/
│           ├── layout.html                    # Layout base
│           ├── dashboard.html                 # Panel principal
│           ├── wardrobe.html                  # Grid de prendas
│           ├── garment-new.html               # Subir nueva prenda
│           ├── garment-confirm.html           # Confirmar clasificación IA
│           ├── garment-edit.html              # Editar prenda
│           ├── garment-detail.html            # Detalle de prenda
│           ├── weekly-plan.html               # Planificación semanal
│           ├── recommendation.html            # Recomendaciones IA
│           ├── inspiration.html               # Looks de inspiración
│           ├── profile-stats.html             # Estadísticas de perfil
│           ├── chat.html                      # Chat premium con streaming SSE
│           ├── error.html                     # Página de error genérica
│           └── fragments/
│               ├── head.html                  # Fragment <head>
│               ├── sidebar.html               # Navegación lateral
│               ├── top-bar.html               # Barra superior
│               └── bottom-nav.html            # Navegación inferior móvil
└── test/
    └── java/com/colorinchi/app/
        ├── ColorinchiApplicationTests.java    # Smoke test de contexto
        ├── config/
        │   ├── RateLimitingInterceptorTest.java
        │   ├── WebMvcConfigTest.java
        │   ├── SecurityConfigTest.java
        │   ├── CurrentOwnerFilterTest.java
        │   └── ... (9+ config test classes)
        ├── controller/
        │   ├── GarmentControllerTest.java
        │   ├── FashionChatControllerTest.java
        │   └── ChatApiControllerTest.java
        ├── dto/
        │   └── OutfitPieceTest.java
        ├── repository/
        │   ├── GarmentRepositoryTest.java
        │   ├── WeekPlanRepositoryTest.java
        │   ├── ChatSessionRepositoryTest.java
        │   └── ChatMessageRepositoryTest.java
        ├── service/
        │   ├── (originales)                   # 6 servicios de armario
        │   ├── AnonymousOwnerServiceTest.java
        │   ├── ChatSessionServiceTest.java
        │   ├── ChatMessageServiceTest.java
        │   ├── ChatPolicyServiceTest.java
        │   ├── ChatIntentClassifierTest.java
        │   ├── WardrobeContextAssemblerTest.java
        │   ├── StreamingChatClientTest.java
        │   ├── ModelRouterTest.java
        │   ├── ChatResponseValidatorTest.java
        │   ├── ProviderResponseParserTest.java
        │   └── analytics/
        │       ├── ChatAnalyticsServiceTest.java
        │       ├── ChatMetricsServiceTest.java
        │       └── LogSanitizerTest.java
        └── upload/
            └── LocalImageStorageServiceTest.java

uploads/                                        # Imágenes subidas (almacenamiento local)
docker-compose.yml                              # PostgreSQL 16 en puerto 55432
pom.xml                                         # Dependencias y configuración Maven
```

---

## Modelo de datos

### Garment (`garments`)

| Columna          | Tipo           | Restricciones               | Descripción                          |
|------------------|----------------|-----------------------------|--------------------------------------|
| id               | BIGSERIAL      | PK                          | Identificador único                  |
| name             | VARCHAR(120)   | NOT NULL                    | Nombre de la prenda                  |
| category         | VARCHAR(50)    | NOT NULL                    | Categoría (Top, Pantalón, etc.)      |
| color_name       | VARCHAR(50)    | NOT NULL                    | Nombre del color                     |
| color_hex        | VARCHAR(7)     |                             | Código hexadecimal del color         |
| material         | VARCHAR(80)    |                             | Material (opcional)                  |
| season           | VARCHAR(50)    |                             | Temporada (opcional)                 |
| image_url        | VARCHAR(500)   | NOT NULL                    | Ruta de la imagen                    |
| ai_type          | VARCHAR(50)    |                             | Tipo sugerido por IA                 |
| ai_color_name    | VARCHAR(50)    |                             | Color sugerido por IA                |
| ai_color_hex     | VARCHAR(7)     |                             | Hex sugerido por IA                  |
| ai_confidence    | NUMERIC(3,2)   |                             | Confianza de la IA (0.00–1.00)      |
| ai_model         | VARCHAR(80)    |                             | Modelo de IA usado                   |
| favorite         | BOOLEAN        | NOT NULL DEFAULT FALSE      | Marca como favorita                  |
| owner_id         | UUID           | NOT NULL FK → anonymous_owners | Propietario anónimo                  |
| user_confirmed   | BOOLEAN        | NOT NULL DEFAULT FALSE      | Confirmada por el usuario            |
| created_at       | TIMESTAMPTZ    | NOT NULL DEFAULT NOW()      | Fecha de creación                    |
| updated_at       | TIMESTAMPTZ    | NOT NULL DEFAULT NOW()      | Fecha de modificación                |

**Índices**: `category`, `color_name`, `user_confirmed`, `owner_id + created_at DESC`.

### WeekPlan (`week_plans`)

| Columna    | Tipo        | Restricciones               | Descripción                    |
|------------|-------------|-----------------------------|--------------------------------|
| id         | BIGSERIAL   | PK                          | Identificador único            |
| garment_id | BIGINT      | FK → garments(id), NOT NULL | Prenda asignada                |
| day_of_week| VARCHAR(10) | NOT NULL                    | Día (Lunes, Martes, etc.)      |
| position   | INTEGER     | NOT NULL                    | Orden dentro del día           |
| owner_id   | UUID        | NOT NULL FK → anonymous_owners | Propietario anónimo          |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW()      | Fecha de creación              |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW()      | Fecha de modificación          |

### AnonymousOwner (`anonymous_owners`)

| Columna    | Tipo        | Restricciones          | Descripción                             |
|------------|-------------|------------------------|-----------------------------------------|
| id         | UUID        | PK                     | Identificador único del owner           |
| bootstrap  | BOOLEAN     | NOT NULL DEFAULT FALSE | Owner por defecto para migración        |
| claimed_at | TIMESTAMPTZ |                        | Cuándo se asoció a un navegador         |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Fecha de creación                       |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Fecha de modificación                   |

### ChatSession (`chat_sessions`)

| Columna    | Tipo         | Restricciones                    | Descripción                         |
|------------|--------------|----------------------------------|-------------------------------------|
| id         | UUID         | PK                               | Identificador único                 |
| owner_id   | UUID         | NOT NULL FK → anonymous_owners   | Propietario anónimo                 |
| title      | VARCHAR(255) | NOT NULL DEFAULT 'Nueva conversación' | Título de la conversación       |
| model      | VARCHAR(100) | NOT NULL DEFAULT 'gpt-4o'        | Modelo seleccionado                 |
| status     | VARCHAR(50)  | NOT NULL DEFAULT 'active'        | Estado (active, archived)           |
| archived   | BOOLEAN      | NOT NULL DEFAULT FALSE           | Soft-delete por retention           |
| created_at | TIMESTAMPTZ  | NOT NULL DEFAULT NOW()           | Fecha de creación                   |
| updated_at | TIMESTAMPTZ  | NOT NULL DEFAULT NOW()           | Fecha de modificación               |

**Índices**: `owner_id + updated_at DESC`, `archived`.

### ChatMessage (`chat_messages`)

| Columna    | Tipo        | Restricciones                           | Descripción                    |
|------------|-------------|-----------------------------------------|--------------------------------|
| id         | UUID        | PK                                      | Identificador único            |
| session_id | UUID        | NOT NULL FK → chat_sessions ON DELETE CASCADE | Sesión padre              |
| owner_id   | UUID        | NOT NULL FK → anonymous_owners          | Propietario                    |
| role       | VARCHAR(50) | NOT NULL                                | `user` o `assistant`           |
| content    | TEXT        | NOT NULL DEFAULT ''                     | Contenido del mensaje          |
| tokens     | INT         | NOT NULL DEFAULT 0                      | Tokens estimados               |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW()                  | Fecha de creación              |

**Índices**: `session_id + created_at ASC`, `owner_id + created_at DESC`.

### ChatRun (`chat_runs`)

| Columna         | Tipo        | Restricciones                           | Descripción                    |
|-----------------|-------------|-----------------------------------------|--------------------------------|
| id              | UUID        | PK                                      | Identificador único            |
| session_id      | UUID        | NOT NULL FK → chat_sessions ON DELETE CASCADE | Sesión padre              |
| owner_id        | UUID        | NOT NULL FK → anonymous_owners          | Propietario                    |
| model_requested | VARCHAR(100)| NOT NULL                                | Modelo solicitado              |
| model_resolved  | VARCHAR(100)|                                         | Modelo resuelto real           |
| status          | VARCHAR(50) | NOT NULL DEFAULT 'pending'              | pending / running / completed / failed |
| started_at      | TIMESTAMPTZ |                                         | Inicio del streaming           |
| completed_at    | TIMESTAMPTZ |                                         | Fin del streaming              |
| total_tokens    | INT         | NOT NULL DEFAULT 0                      | Tokens consumidos              |
| error_message   | TEXT        |                                         | Mensaje de error si falló      |
| created_at      | TIMESTAMPTZ | NOT NULL DEFAULT NOW()                  | Fecha de creación              |

**Índices**: `session_id + started_at DESC`, `owner_id + started_at DESC`.

### ChatFeedback (`chat_feedback`)

| Columna    | Tipo        | Restricciones                           | Descripción                    |
|------------|-------------|-----------------------------------------|--------------------------------|
| id         | UUID        | PK                                      | Identificador único            |
| run_id     | UUID        | NOT NULL FK → chat_runs ON DELETE CASCADE | Run evaluado                 |
| session_id | UUID        | NOT NULL FK → chat_sessions ON DELETE CASCADE | Sesión padre              |
| owner_id   | UUID        | NOT NULL FK → anonymous_owners          | Propietario                    |
| rating     | VARCHAR(50) | NOT NULL                                | `helpful`, `not_helpful`, etc. |
| comment    | TEXT        |                                         | Comentario opcional            |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW()                  | Fecha de creación              |

**Índices**: `run_id`, `session_id`.

### ChatAnalyticsEvent (`chat_analytics_events`)

| Columna    | Tipo         | Restricciones                  | Descripción                    |
|------------|--------------|--------------------------------|--------------------------------|
| id         | UUID         | PK                             | Identificador único            |
| owner_id   | UUID         | NOT NULL FK → anonymous_owners | Propietario                    |
| event_type | VARCHAR(100) | NOT NULL                       | Tipo de evento                 |
| event_data | TEXT         | NOT NULL DEFAULT '{}'          | JSON con datos del evento      |
| created_at | TIMESTAMPTZ  | NOT NULL DEFAULT NOW()         | Fecha de creación              |

**Índices**: `event_type + created_at DESC`, `owner_id + created_at DESC`, `created_at`.

---

## Rutas completas

### Gestión de armario (GarmentController)

Todas las rutas están en `GarmentController`. La aplicación usa HTMX para las interacciones dinámicas; los fragmentos se especifican con `HX-Retarget` o selectores en las vistas.

| Método | Ruta                        | Vista / Respuesta              | Parámetros                              | Descripción                                         |
|--------|-----------------------------|--------------------------------|-----------------------------------------|-----------------------------------------------------|
| GET    | `/`                         | redirect → `/dashboard`        | —                                       | Redirige al dashboard                               |
| GET    | `/dashboard`                | `dashboard`                    | —                                       | Estadísticas, últimas prendas, progreso semanal    |
| GET    | `/wardrobe`                 | `wardrobe`                     | —                                       | Grid completo de prendas                            |
| GET    | `/wardrobe/filter`          | `wardrobe :: grid` (fragment)  | `category` (opcional) / `favoritos`     | Filtra por categoría o favoritos                   |
| GET    | `/wardrobe/new`             | `garment-new`                  | —                                       | Formulario de subida                                |
| POST   | `/wardrobe/analyze`         | `garment-confirm`              | `image` (MultipartFile)                 | Sube imagen, clasifica con IA, muestra revisión    |
| POST   | `/wardrobe`                 | redirect → `/wardrobe/{id}`    | `GarmentReviewForm` (body)              | Guarda prenda confirmada                            |
| GET    | `/wardrobe/{id}`            | `garment-detail`               | `id` (path)                             | Detalle + prendas compatibles + acompañantes        |
| GET    | `/wardrobe/{id}/edit`       | `garment-edit`                 | `id` (path)                             | Formulario de edición                               |
| PUT    | `/wardrobe/{id}`            | redirect → `/wardrobe/{id}`    | `id` (path), `GarmentReviewForm` (body) | Actualiza prenda (no modifica campos IA ni imagen) |
| POST   | `/wardrobe/{id}/favorite`   | fragment HTMX                  | `id` (path), `variant=card/detail`      | Marca/desmarca favorito                             |
| DELETE | `/wardrobe/{id}`            | `@ResponseBody ""`             | `id` (path), `source=card/detail`       | Elimina prenda (con redirect si es detail)          |
| GET    | `/inspiration`              | `inspiration`                  | —                                       | Looks de inspiración con tags                       |
| GET    | `/recommendation`           | `recommendation`               | —                                       | Recomendaciones de outfits por IA                   |
| GET    | `/weekly-plan`              | `weekly-plan`                  | —                                       | Planificación semanal                               |
| POST   | `/weekly-plan/assign`       | `@ResponseBody ""`             | `garmentId`, `dayOfWeek`, `position`    | Asigna prenda a un día (reemplaza si ya existe)    |
| PUT    | `/weekly-plan/reorder`      | `@ResponseBody ""`             | `dayOfWeek`, `order` (List<Long>)       | Reordena prendas dentro de un día                   |
| DELETE | `/weekly-plan/{id}`         | `@ResponseBody ""`             | `id` (path)                             | Quita prenda del plan semanal                       |
| GET    | `/profile`                  | `profile-stats`                | —                                       | Estadísticas detalladas + colores principales       |

### Chat (FashionChatController + ChatApiController + ChatMetricsController)

| Método | Ruta                                   | Controlador            | Descripción                                              |
|--------|----------------------------------------|------------------------|----------------------------------------------------------|
| GET    | `/chat`                                | FashionChatController  | Página principal de chat con lista de sesiones y contexto del armario |
| GET    | `/chat/{sessionId}`                    | FashionChatController  | Página de chat con historial de mensajes de una sesión   |
| POST   | `/api/chat/sessions`                   | ChatApiController      | Crea una nueva sesión de chat (`title`, `model` opcionales) |
| GET    | `/api/chat/sessions`                   | ChatApiController      | Lista todas las sesiones del owner actual                |
| GET    | `/api/chat/sessions/{sessionId}`       | ChatApiController      | Obtiene una sesión por ID                                |
| DELETE | `/api/chat/sessions/{sessionId}`       | ChatApiController      | Elimina una sesión y todos sus mensajes/runs/feedback    |
| PATCH  | `/api/chat/sessions/{sessionId}/title` | ChatApiController      | Actualiza el título de una sesión                        |
| POST   | `/api/chat/sessions/{sessionId}/messages` | ChatApiController    | Envía un mensaje, evalúa policy, crea run, devuelve `runId` |
| GET    | `/api/chat/sessions/{sessionId}/messages` | ChatApiController    | Lista los mensajes de una sesión                         |
| GET    | `/api/chat/stream/{runId}`             | ChatApiController      | SSE endpoint: consume el stream de la IA en tiempo real  |
| POST   | `/api/chat/runs/{runId}/feedback`      | ChatApiController      | Envía feedback (`rating`, `comment` opcional) para un run |
| GET    | `/api/chat/models`                     | ChatApiController      | Lista los modelos de IA disponibles                      |
| GET    | `/api/admin/metrics`                   | ChatMetricsController  | Métricas operativas (solo acceso localhost)              |

### Rate limiting

Las rutas de análisis, recomendación y chat tienen rate limiting por IP y/o por owner:

| Endpoint          | Capacidad | Ventana     | Ámbito |
|-------------------|-----------|-------------|--------|
| POST /wardrobe/analyze | 10 solicitudes | 60 minutos | IP |
| GET /recommendation   | 5 solicitudes  | 30 minutos | IP |
| POST /api/chat/sessions/{id}/messages | 10 solicitudes | 1 minuto | Owner |
| POST /api/chat/runs/{id}/feedback    | 10 solicitudes | 1 minuto | Owner |
| GET /api/chat/stream/{runId}         | 30 solicitudes | 1 minuto | IP (global) |

Además, el **Policy Engine** aplica un rate limit interno de 30 mensajes por minuto por owner antes de llegar al AI provider.

Cuando se excede el límite, la aplicación responde con un error 429: en las vistas Thymeleaf se muestra "Demasiadas solicitudes", y en los endpoints REST se devuelve un `ProblemDetail` (RFC 7807).

---

## Configuración de referencia

### `app.ai.*` — Servidor IA

| Propiedad           | Default                         | Descripción                                     |
|---------------------|---------------------------------|-------------------------------------------------|
| `app.ai.enabled`    | `true`                          | Activa/desactiva la integración con IA           |
| `app.ai.base-url`   | `https://api.nan.builders`      | URL base del servidor OpenAI-compatible          |
| `app.ai.chat-path`  | `/v1/chat/completions`          | Path del endpoint de chat                        |
| `app.ai.model`      | `qwen3.6`                       | Modelo por defecto                               |
| `app.ai.api-key`    | `${NAN_API_KEY:}`               | API key (variable de entorno recomendada)        |
| `app.ai.max-tokens` | `2000`                          | Máximo de tokens en la respuesta                |
| `app.ai.connect-timeout` | `5s`                        | Timeout de conexión                              |
| `app.ai.read-timeout` | `60s`                         | Timeout de lectura (aumentado para streaming)   |
| `app.ai.models`     | —                               | Lista de modelos disponibles (ver abajo)         |

**Modelos configurados**:

| ID                 | Nombre            | Provider   | Default |
|--------------------|-------------------|------------|---------|
| `gemma4`           | Gemma 4           | google     | No      |
| `qwen3.6`          | Qwen 3.6          | alibaba    | Sí      |
| `deepseek-v4-flash`| DeepSeek V4 Flash | deepseek   | No      |

### `app.upload.*` — Subida de imágenes

| Propiedad                      | Default                                   | Descripción                     |
|--------------------------------|-------------------------------------------|---------------------------------|
| `app.upload.directory`         | `uploads`                                 | Directorio de almacenamiento    |
| `app.upload.max-size`          | `8MB`                                     | Tamaño máximo por archivo       |
| `app.upload.allowed-content-types` | `image/jpeg`, `image/png`, `image/webp` | Tipos MIME permitidos           |

Las imágenes se redimensionan a 900×900 px con calidad 0.88 mediante Thumbnailator y se convierten a JPEG independientemente del formato original.

### `app.wardrobe.*` — Configuración del armario

| Propiedad              | Default                                                                                     | Descripción                         |
|------------------------|---------------------------------------------------------------------------------------------|-------------------------------------|
| `app.wardrobe.categories` | `Top, Pantalón, Vestido, Falda, Chaqueta, Abrigo, Camisa, Sudadera, Zapatos, Accesorio, Otro` | Categorías disponibles              |
| `app.wardrobe.days`    | `Lunes, Martes, Miercoles, Jueves, Viernes, Sabado, Domingo`                                | Días para planificación semanal    |
| `app.wardrobe.color-limit` | `5`                                                                                      | Límite de colores en estadísticas  |
| `app.wardrobe.upcoming-days-to-include` | `3` (default del código)                                                       | Días a incluir en contexto del chat |

### `app.rate-limit.*` — Control de tasa

| Propiedad                              | Default | Descripción                          |
|----------------------------------------|---------|--------------------------------------|
| `app.rate-limit.analyze.capacity`       | `10`    | Máximo de análisis por ventana       |
| `app.rate-limit.analyze.refill-minutes` | `60`    | Minutos para recarga completa        |
| `app.rate-limit.recommendation.capacity`| `5`     | Máximo de recomendaciones por ventana|
| `app.rate-limit.recommendation.refill-minutes` | `30` | Minutos para recarga completa    |
| `app.rate-limit.chat.capacity`          | `30`    | Máximo de streams SSE por ventana (IP global) |
| `app.rate-limit.chat.refill-minutes`    | `1`     | Minutos para recarga completa        |
| `app.rate-limit.chat-per-owner.capacity`| `10`    | Máximo de mensajes/feedback por owner |
| `app.rate-limit.chat-per-owner.refill-minutes` | `1` | Minutos para recarga completa    |

### `app.chat.retention.*` — Retención de datos

| Propiedad                              | Default | Descripción                                    |
|----------------------------------------|---------|------------------------------------------------|
| `app.chat.retention.analytics-events-days` | `90` | Días antes de purgar eventos de analytics      |
| `app.chat.retention.session-inactive-days` | `180` | Días de inactividad antes de archivar sesiones |
| `app.chat.retention.orphan-upload-cleanup` | `false` | Eliminar archivos huérfanos en `uploads/`   |

### `management.*` — Actuator

| Propiedad                              | Default        | Descripción                                   |
|----------------------------------------|----------------|-----------------------------------------------|
| `management.endpoints.web.exposure.include` | `health,info` | Endpoints expuestos vía HTTP                |
| `management.endpoint.health.show-details` | `when-authorized` | Detalles solo para usuarios autenticados |
| `management.endpoint.health.show-components` | `when-authorized` | Componentes individuales                  |
| `management.endpoint.health.probes.enabled` | `true`       | Probes de liveness/readiness habilitadas     |

**Endpoints de health**:
- `/actuator/health` — Health agregado (DB + AI provider)
- `/actuator/health/liveness` — Liveness probe (siempre UP)
- `/actuator/health/readiness` — Readiness probe (DB + AI)
- `/actuator/health/aiProvider` — Health custom del AI provider

### `spring.*` — Infraestructura

| Propiedad                              | Default                              | Descripción                              |
|----------------------------------------|--------------------------------------|------------------------------------------|
| `spring.datasource.url`                | `jdbc:postgresql://localhost:55432/ropa` | Conexión a PostgreSQL                    |
| `spring.datasource.username`           | `ropa`                               | Usuario de base de datos                 |
| `spring.datasource.password`           | `${DB_PASSWORD:ropa}`                | Contraseña (variable de entorno)         |
| `spring.jpa.hibernate.ddl-auto`        | `validate`                           | Hibernate solo valida, Flyway migra      |
| `spring.jpa.open-in-view`              | `false`                              | OSIV desactivado                         |
| `spring.servlet.multipart.max-file-size` | `8MB`                              | Tamaño máximo de archivo subido          |
| `spring.flyway.enabled`                | `true`                               | Migraciones automáticas                  |
| `spring.cache.type`                    | `caffeine`                           | Caché en memoria                         |
| `spring.cache.caffeine.spec`           | `maximumSize=500,expireAfterWrite=24h` | 500 entradas, expiración a las 24h      |
| `server.port`                          | `8081`                               | Puerto del servidor                      |
| `spring.task.execution.pool.core-size` | `2`                                  | Hilos del pool de tareas async           |
| `spring.task.execution.pool.max-size`  | `4`                                  | Máximo de hilos                          |

---

## Ready for production

### Health checks

La aplicación expone tres niveles de health check vía Spring Boot Actuator:

```bash
# Health general (DB + AI provider)
curl http://localhost:8081/actuator/health

# Readiness probe (para Kubernetes)
curl http://localhost:8081/actuator/health/readiness

# Liveness probe
curl http://localhost:8081/actuator/health/liveness

# AI provider custom health
curl http://localhost:8081/actuator/health/aiProvider
```

El health del AI provider (`HealthConfig`) hace un ping a `app.ai.base-url` con timeout de 5s. Reporta `UP` si responde, `DOWN` si falla, `UNKNOWN` si la IA está desactivada.

### Structured logging

El proyecto incluye dependencia opcional `net.logstash.logback:logstash-logback-encoder`. Para activarla:

1. Agregar `logback-spring.xml` en `src/main/resources/` con appender Logstash
2. Descomentar la dependencia en `pom.xml` (está como `<optional>true</optional>`)
3. Los logs incluirán campos estructurados: `runId`, `sessionId`, `model`, `ownerId` vía MDC

### Data retention

`ChatDataRetentionService` ejecuta un ciclo de limpieza diario a las **3:00 AM** (configurable vía `@Scheduled(cron = "0 0 3 * * ?")`):

- **Analytics events**: Elimina eventos más antiguos que `app.chat.retention.analytics-events-days` (default: 90 días)
- **Sessions inactivas**: Archiva (soft-delete con `archived = true`) sesiones sin actividad por más de `app.chat.retention.session-inactive-days` (default: 180 días)
- **Orphan uploads** (opcional): Si `app.chat.retention.orphan-upload-cleanup=true`, elimina archivos en `uploads/` no referenciados por ninguna prenda

### Security headers

Configurados en `SecurityConfig`:

| Header | Valor |
|--------|-------|
| `Content-Security-Policy` | `default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` |
| `X-Frame-Options` | `DENY` |

### Admin endpoint restriction

El endpoint `/api/admin/metrics` solo es accesible desde **localhost** (`127.0.0.1`, `::1`). Cualquier intento desde otra IP recibe `403 Forbidden`.

### CSRF

CSRF está habilitado con `CsrfTokenRequestAttributeHandler`. Los endpoints REST (`/api/chat/**`) deben incluir el token CSRF en las solicitudes mutantes (POST, DELETE, PATCH). Las vistas Thymeleaf lo incluyen automáticamente via `_csrf`.

---

## Levantar local

### Requisitos previos

- Java 21+
- Docker Desktop (para PostgreSQL)
- Maven (incluido con el wrapper o instalado global)

### Pasos

1. **Iniciar PostgreSQL:**

```bash
docker compose up -d postgres
```

La base queda expuesta en `localhost:55432` para no chocar con instalaciones locales en `5432`. La imagen usa `postgres:16-alpine` con healthcheck automático.

2. **Configurar variables de entorno:**

```bash
# Requerida para conexión a base de datos
export DB_PASSWORD=ropa

# Requerida para integración con IA
export NAN_API_KEY=sk-tu-key-aqui
```

3. **Configurar el servidor IA** en `src/main/resources/application.yml` si no usa los defaults:

```yaml
app:
  ai:
    base-url: https://api.nan.builders
    chat-path: /v1/chat/completions
    model: qwen3.6
    api-key: ${NAN_API_KEY}
    max-tokens: 2000
    read-timeout: 60s
```

4. **Ejecutar la aplicación:**

```bash
mvn spring-boot:run
```

> **Regla del proyecto:** ejecutar siempre en `http://localhost:8081`. No levantar la app en otros puertos para desarrollo ni validación Playwright.

5. **Abrir en el navegador:**

```text
http://localhost:8081
```

> **Nota:** La primera vez que se ejecuta, Flyway aplica automáticamente las migraciones (`V1` a `V7`).

### Test profile

Para ejecutar tests (usa H2 en memoria, no requiere PostgreSQL):

```bash
mvn test
```

El perfil de test está configurado en `src/test/resources/application-test.yml` con:
- Base de datos H2 en memoria (`jdbc:h2:mem:testdb`)
- Flyway deshabilitado (`ddl-auto: create-drop`)
- Rate limits elevados (capacity: 1000, refill: 1 min) para evitar falsos positivos

---

## Flujo implementado

### Gestión de armario

1. La usuaria pulsa `+` y se abre `/wardrobe/new`.
2. Selecciona y sube una imagen JPG, PNG o WebP.
3. La app comprime la imagen a 900×900 px y la envía al servidor IA como base64.
4. La IA responde con tipo, nombre de color y código hexadecimal.
5. La usuaria **confirma o corrige** tipo/color/material/temporada en la pantalla de revisión.
6. La prenda queda visible en `/wardrobe` (grid general) y `/wardrobe/{id}` (detalle).
7. Desde el detalle, la usuaria puede:
   - Marcar como favorita
   - Editar la información
   - Eliminar la prenda
   - Ver prendas compatibles (por categoría y temporada)
   - Ver prendas que la acompañan en el plan semanal
8. En la planificación semanal, asigna prendas a cada día con orden personalizado.
9. La IA puede sugerir outfits completos basados en el guardarropa actual.

**Tolerancia a fallos:** Si el servidor IA no responde, el flujo no se bloquea: permite completar la prenda manualmente. La clasificación IA se cachea 24h, por lo que re-clasificar la misma imagen es instantáneo.

### Chat premium

1. La usuaria navega a `/chat` y ve la lista de sesiones + contexto del armario.
2. Crea una nueva sesión o selecciona una existente.
3. Escribe un mensaje (ej: "qué me pongo con este pantalón rojo?").
4. El mensaje pasa por el **Policy Engine**: se clasifica la intención, se verifica rate limit.
   - Si es un pedido de outfit definitivo → se bloquea con mensaje amigable.
   - Si es styling/color advice → se permite y se registra como FLAG.
5. Se persiste el mensaje como `user`, se crea un `ChatRun` en estado `running`.
6. El frontend abre una conexión SSE a `/api/chat/stream/{runId}`.
7. El servidor construye el system prompt con el contexto del guardarropa, envía el historial al AI provider y stremea la respuesta token por token.
8. Al completar, se persiste el mensaje como `assistant`, el run pasa a `completed`.
9. La usuaria puede enviar feedback (helpful / not helpful) que se persiste y registra en analytics.

---

## Guía de testing

### Ejecutar tests

```bash
mvn test
```

Todos los tests usan JUnit 5. Los tests de integración con base de datos usan **H2 en memoria** (no PostgreSQL), configurado en `src/test/resources/application-test.yml`.

### Tests existentes (40+ clases)

#### Gestión de armario (12 tests originales)

| Clase                                | Técnica               | Cobertura                                                |
|--------------------------------------|-----------------------|----------------------------------------------------------|
| `GarmentControllerTest`              | MockMvc + MockitoBean | 27 escenarios: dashboard, wardrobe, subida, análisis IA, creación, edición, borrado, favoritos, plan semanal, recomendaciones, rate limiting |
| `AiClassificationServiceTest`        | WireMock              | Clasificación exitosa, timeout, servidor caído, formato de respuesta inesperado, path traversal |
| `AiRecommendationServiceTest`        | WireMock + Mockito    | Recomendación exitosa, pocas prendas (< 3), servidor caído, formato inválido |
| `GarmentServiceTest`                 | Mockito               | CRUD completo, toggle favorito, estadísticas, colores principales |
| `GarmentCompatibilityServiceTest`    | Mockito               | Compatibilidad por categoría y temporada, sin categoría, límite de 6 resultados |
| `GarmentRepositoryTest`              | H2 + TestContainers   | Consultas por categoría, favoritos, agrupaciones, paginación |
| `WeekPlanRepositoryTest`             | H2 + TestContainers   | Consultas por día, por prenda, reordenamiento, días distintos |
| `WeekPlanServiceTest`                | Mockito               | Asignación, reordenamiento, eliminación, acompañantes    |
| `InspirationServiceTest`             | Unitario              | Looks predefinidos, tags, estructura de datos             |
| `LocalImageStorageServiceTest`       | TempDir + imágenes reales | Validación de tipos, tamaño, formato, redimensionado |
| `RateLimitingInterceptorTest`        | Mockito               | Límite de solicitudes por endpoint, reset de contador    |
| `ColorinchiApplicationTests`         | Smoke                 | Verifica que el contexto de Spring carga                 |

#### Chat subsystem (15+ tests nuevos)

| Clase                                | Técnica               | Cobertura                                                |
|--------------------------------------|-----------------------|----------------------------------------------------------|
| `FashionChatControllerTest`          | MockMvc               | Página de chat, sesión específica, owner isolation       |
| `ChatApiControllerTest`              | MockMvc               | CRUD de sesiones, envío de mensajes, feedback, SSE, errores, rate limiting |
| `ChatSessionServiceTest`             | Mockito               | Creación, listado, actualización de título, eliminación, owner isolation |
| `ChatMessageServiceTest`             | Mockito               | Creación de mensajes, listado por sesión, límite de tokens |
| `ChatPolicyServiceTest`              | Mockito               | Bloqueo de outfit requests, rate limiting por owner, flagging de styling |
| `ChatIntentClassifierTest`           | Unitario              | Clasificación de intenciones en español e inglés, edge cases |
| `StreamingChatClientTest`            | WireMock              | Streaming exitoso, timeout del provider, error del provider |
| `ModelRouterTest`                    | Unitario              | Resolución de modelos, modelo por defecto, modelo inválido |
| `WardrobeContextAssemblerTest`       | Mockito               | Contexto con/sin prendas, categorías, colores, plan semanal |
| `ChatAnalyticsServiceTest`           | Mockito               | Buffer batch, flush por tamaño y tiempo, eventos de policy |
| `ChatMetricsServiceTest`             | Unitario              | Snapshots, tokens promedio, latencia promedio            |
| `LogSanitizerTest`                   | Unitario              | Sanitización de caracteres especiales en logs            |
| `ChatResponseValidatorTest`          | Unitario              | Validación de respuestas del AI provider                 |
| `ProviderResponseParserTest`         | Unitario              | Parsing de SSE lines, chunks, done signal                |
| `ChatSessionRepositoryTest`          | H2                   | Consultas por owner, archive, delete por owner           |
| `ChatMessageRepositoryTest`          | H2                   | Consultas por sesión, creación, owner isolation          |
| `AnonymousOwnerServiceTest`          | Mockito               | Creación, resolución, cookie management                  |
| `CurrentOwnerFilterTest`             | MockMvc               | Filtro HTTP, cookie reading, fallback a nuevo owner      |
| `SecurityConfigTest`                 | MockMvc               | CSP headers, admin localhost restriction, CSRF protection|
| `GlobalExceptionHandlerTest`         | MockMvc               | Errores 400, 429, 500 con vistas amigables               |
| `WebMvcConfigTest`                   | MockMvc               | Recursos estáticos, interceptors registrados             |

### Tests con WireMock

Los servicios que se comunican con el servidor IA (`AiClassificationService`, `AiRecommendationService`, `StreamingChatClient`) usan WireMock para simular respuestas HTTP. El servidor WireMock se levanta en un puerto aleatorio antes de cada test y se detiene al finalizar.

---

## Workflow de desarrollo

### Arrancar PostgreSQL

```bash
docker compose up -d postgres
```

### Ejecutar la aplicación

```bash
mvn spring-boot:run
```

### Resetear la base de datos

Si los tests contra H2 dejaron tablas inconsistentes o necesitás migraciones limpias:

```bash
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:55432/ropa -Dflyway.user=ropa -Dflyway.password=ropa
```

Esto aplica todas las migraciones pendientes contra PostgreSQL local.

### Limpiar y reconstruir

```bash
mvn clean install
```

### Ejecutar un test específico

```bash
mvn test -Dtest=GarmentControllerTest
```

### Desactivar la IA para desarrollo

Si no tenés conexión al servidor IA, desactivá la clasificación automática:

```yaml
# application.yml
app:
  ai:
    enabled: false
```

Con IA desactivada, las prendas se cargan completamente a mano (el formulario de revisión aparece sin datos de clasificación) y el chat devuelve respuestas vacías.

---

## Solución de problemas

| Problema | Causa probable | Solución |
|----------|---------------|----------|
| `missing table garments` | Las tablas fueron eliminadas (tests con H2 u otro proceso) | Ejecutar `mvn flyway:migrate ...` para recrear las tablas |
| `relation "garments" does not exist` | Flyway no se ejecutó | Verificar `spring.flyway.enabled=true` y conectar a PostgreSQL |
| `APP_AI_API_KEY is required` | Falta la variable de entorno | `export APP_AI_API_KEY=sk-tu-key-aqui` o desactivar IA con `app.ai.enabled=false` |
| 429 Too Many Requests | Se excedió el rate limit | Esperar a que se recargue (análisis: 60 min, recomendaciones: 30 min, chat: 1 min) |
| `La imagen supera el tamano maximo permitido` | Archivo > 8 MB | Redimensionar o comprimir la imagen antes de subir |
| `El archivo no es una imagen válida` | Formato no soportado o archivo corrupto | Usar JPG, PNG o WebP válidos |
| `Connection refused: localhost/127.0.0.1:55432` | PostgreSQL no está corriendo | `docker compose up -d postgres` y esperar al healthcheck |
| La clasificación IA falla siempre | API key incorrecta o servidor no accesible | Verificar `APP_AI_API_KEY` y `app.ai.base-url` |
| Las imágenes nuevas no se ven | El directorio `uploads/` no existe o no tiene permisos | `mkdir -p uploads` o verificar permisos de escritura |
| Las vistas no se actualizan | Caché del navegador | Hard refresh (Cmd+Shift+R) o abrir en incógnito |
| Error al hacer clic en "Guardar" | Validación del formulario | Revisar que todos los campos obligatorios estén completos |
| El chat no stremea respuestas | AI provider timeout o API key inválida | Verificar `NAN_API_KEY` y `app.ai.read-timeout` (default 60s) |
| `Chat run not found` | Owner mismatch o sesión eliminada | Verificar que la cookie `owner_id` esté presente y la sesión no haya sido borrada |
| `Stream timeout` | La respuesta de la IA tarda más de 5 minutos | Aumentar `SSE_TIMEOUT` en `ChatApiController` o verificar conectividad |
| Las métricas de admin dan 403 | No se accede desde localhost | Usar `curl http://localhost:8081/api/admin/metrics` |
| Los eventos de analytics no se persisten | El buffer no se flushó aún | Esperar hasta 30s (flush automático) o enviar 10 eventos para flush temprano |
| El rate limit de chat se excede muy rápido | Aplicación compartida o requests automáticas | Revisar `app.rate-limit.chat-per-owner.capacity` o el interceptor de rate limiting |
