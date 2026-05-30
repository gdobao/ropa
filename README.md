# Armario Cápsula

Aplicación local en Java 21 + Spring Boot 3 para gestionar un armario cápsula: cargar prendas desde imagen, sugerir tipo y color con un servidor IA propio, confirmar los datos antes de guardarlos, planificar la semana y recibir recomendaciones de outfits.

## Stack

| Capa             | Tecnología                                |
|------------------|-------------------------------------------|
| Backend          | Java 21, Spring Boot 3.4.5                |
| Build            | Maven                                     |
| UI               | Thymeleaf mobile-first + HTMX             |
| DB               | PostgreSQL 16 + Flyway                    |
| IA visión        | Servidor local OpenAI-compatible          |
| Almacenamiento   | Filesystem local en `uploads/`            |
| Caché            | Caffeine (classification en 24h)          |
| Testing          | JUnit 5, Mockito, WireMock, H2, TestContainers |

## Índice

- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Modelo de datos](#modelo-de-datos)
- [Rutas completas](#rutas-completas)
- [Configuración de referencia](#configuración-de-referencia)
- [Levantar local](#levantar-local)
- [Flujo implementado](#flujo-implementado)
- [Guía de testing](#guía-de-testing)
- [Workflow de desarrollo](#workflow-de-desarrollo)
- [Solución de problemas](#solución-de-problemas)

---

## Arquitectura

La aplicación sigue una arquitectura MVC clásica con controladores, servicios y repositorios.

### Capas

```
┌─────────────────────────────────────────────────────┐
│                    Controller                         │
│           GarmentController (17 endpoints)           │
├─────────────────────────────────────────────────────┤
│                      Service                          │
│  GarmentService  │  WeekPlanService                  │
│  AiClassificationService  │  AiRecommendationService │
│  GarmentCompatibilityService  │  InspirationService   │
├─────────────────────────────────────────────────────┤
│                   Repository                          │
│     GarmentRepository  │  WeekPlanRepository         │
├─────────────────────────────────────────────────────┤
│                   Model / DTO                         │
│       Garment (JPA)  │  WeekPlan (JPA)               │
│       7 DTOs para boundaries de API                  │
├─────────────────────────────────────────────────────┤
│                    Config                             │
│  6 @ConfigurationProperties  │  SecurityConfig       │
│  WebMvcConfig  │  WebClientConfig                    │
│  GlobalExceptionHandler  │  RateLimitingInterceptor  │
└─────────────────────────────────────────────────────┘
```

**Controller**: `GarmentController` con 17 endpoints. Maneja toda la interacción HTTP, usa Thymeleaf para las vistas y responde con fragmentos HTMX para las interacciones dinámicas.

**Service layer (6 servicios)**:
- **GarmentService**: CRUD de prendas, estadísticas del dashboard, colores principales.
- **AiClassificationService**: Clasifica imágenes contra un servidor IA compatible con OpenAI. Cachea resultados 24h con Caffeine. Si falla, permite carga manual.
- **AiRecommendationService**: Genera sugerencias de outfits basadas en el guardarropa actual mediante IA.
- **GarmentCompatibilityService**: Encuentra prendas compatibles por categoría y temporada (hasta 6 resultados).
- **WeekPlanService**: Planificación semanal: asignar prendas a días, reordenar, buscar acompañantes.
- **InspirationService**: Looks predefinidos de inspiración con tags y paletas de color.

**Repository layer (2 JPA repositories)**:
- **GarmentRepository**: Consultas por categoría, favoritos, agrupaciones por categoría y color.
- **WeekPlanRepository**: Consultas por día, por prenda, reordenamiento.

**Config layer (11 clases)**:
- `AiServerProperties` — conexión al servidor IA (URL, modelo, API key, timeouts)
- `WardrobeProperties` — categorías, días de la semana, límite de colores
- `UploadProperties` — directorio de subida, tamaño máximo, tipos permitidos
- `RateLimitProperties` — capacidad y tiempo de recarga por endpoint
- `SecurityConfig` — protección CSRF básica, sin autenticación
- `WebMvcConfig` — recursos estáticos, interceptores de rate limiting
- `WebClientConfig` — cliente HTTP reactivo con timeouts configurables
- `GlobalExceptionHandler` — manejo centralizado de errores con vistas amigables
- `RateLimitingInterceptor` — control de tasa por IP con Caffeine
- `RateLimitExceededException` — excepción específica para 429
- `AiServerPropertiesValidator` — validación al arranque de propiedades IA

**DTO layer (7 records/classes)**:
- `GarmentReviewForm` — formulario de revisión/edición con validación
- `AiClassificationResponse` — respuesta de clasificación (type, colorName, colorHex, confidence)
- `AiRecommendationResponse` — lista de outfits sugeridos
- `OutfitSuggestion` — un outfit con nombre, descripción y piezas
- `OutfitPiece` — una pieza del outfit (categoría, color)
- `InspirationLook` — look de inspiración con tags y paleta
- `DashboardStats` — estadísticas con desglose por categoría

---

## Estructura del proyecto

```
src/
├── main/
│   ├── java/com/colorinchi/app/
│   │   ├── ColorinchiApplication.java          # Entry point (@EnableCaching, @EnableRetry)
│   │   ├── config/
│   │   │   ├── AiServerProperties.java         # app.ai.* — servidor IA
│   │   │   ├── AiServerPropertiesValidator.java # Validación al startup
│   │   │   ├── GlobalExceptionHandler.java     # @ControllerAdvice global
│   │   │   ├── RateLimitExceededException.java # Excepción 429
│   │   │   ├── RateLimitingInterceptor.java    # Rate limiter por IP con Caffeine
│   │   │   ├── RateLimitProperties.java        # app.rate-limit.*
│   │   │   ├── SecurityConfig.java            # CSRF-only, sin auth
│   │   │   ├── UploadProperties.java          # app.upload.*
│   │   │   ├── WardrobeProperties.java        # app.wardrobe.*
│   │   │   ├── WebClientConfig.java           # WebClient reactivo
│   │   │   └── WebMvcConfig.java              # Recursos estáticos + interceptores
│   │   ├── controller/
│   │   │   └── GarmentController.java         # 17 endpoints MVC
│   │   ├── dto/
│   │   │   ├── AiClassificationResponse.java  # Respuesta de clasificación IA
│   │   │   ├── AiRecommendationResponse.java  # Respuesta de recomendación IA
│   │   │   ├── DashboardStats.java            # Estadísticas del dashboard
│   │   │   ├── GarmentReviewForm.java         # Formulario de revisión/edición
│   │   │   ├── InspirationLook.java           # Look de inspiración
│   │   │   ├── OutfitPiece.java               # Pieza individual de outfit
│   │   │   └── OutfitSuggestion.java          # Sugerencia de outfit completo
│   │   ├── model/
│   │   │   ├── Garment.java                   # Entidad JPA: prendas
│   │   │   └── WeekPlan.java                  # Entidad JPA: planificación semanal
│   │   ├── repository/
│   │   │   ├── GarmentRepository.java         # Consultas de prendas
│   │   │   └── WeekPlanRepository.java        # Consultas de planificación
│   │   ├── service/
│   │   │   ├── AiClassificationService.java   # Clasificación por IA con caché
│   │   │   ├── AiRecommendationService.java   # Recomendación de outfits por IA
│   │   │   ├── GarmentCompatibilityService.java # Compatibilidad entre prendas
│   │   │   ├── GarmentService.java            # CRUD y estadísticas
│   │   │   ├── InspirationService.java        # Looks predefinidos
│   │   │   └── WeekPlanService.java           # Planificación semanal
│   │   └── upload/
│   │       ├── ImageStorageService.java       # Interfaz de almacenamiento
│   │       └── LocalImageStorageService.java  # Implementación local con Thumbnailator
│   └── resources/
│       ├── application.yml                    # Configuración principal
│       ├── db/migration/
│       │   ├── V1__create_garments.sql        # Tabla garments + índices
│       │   ├── V2__add_favorite_to_garments.sql # Columna favorite
│       │   └── V3__create_week_plans.sql      # Tabla week_plans
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
        │   └── RateLimitingInterceptorTest.java # Tests de rate limiting
        ├── controller/
        │   └── GarmentControllerTest.java     # Tests del controlador (MockMvc)
        ├── repository/
        │   ├── GarmentRepositoryTest.java     # Tests de repositorio
        │   └── WeekPlanRepositoryTest.java    # Tests de repositorio
        ├── service/
        │   ├── AiClassificationServiceTest.java # Tests con WireMock
        │   ├── AiRecommendationServiceTest.java # Tests con WireMock + Mockito
        │   ├── GarmentCompatibilityServiceTest.java # Tests de compatibilidad
        │   ├── GarmentServiceTest.java        # Tests de CRUD
        │   ├── InspirationServiceTest.java    # Tests de inspiración
        │   └── WeekPlanServiceTest.java       # Tests de planificación
        └── upload/
            └── LocalImageStorageServiceTest.java # Tests de subida con imágenes reales

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
| user_confirmed   | BOOLEAN        | NOT NULL DEFAULT FALSE      | Confirmada por el usuario            |
| created_at       | TIMESTAMPTZ    | NOT NULL DEFAULT NOW()      | Fecha de creación                    |
| updated_at       | TIMESTAMPTZ    | NOT NULL DEFAULT NOW()      | Fecha de modificación                |

**Índices**: `category`, `color_name`, `user_confirmed`, `created_at DESC`.

### WeekPlan (`week_plans`)

| Columna    | Tipo        | Restricciones               | Descripción                    |
|------------|-------------|-----------------------------|--------------------------------|
| id         | BIGSERIAL   | PK                          | Identificador único            |
| garment_id | BIGINT      | FK → garments(id), NOT NULL | Prenda asignada                |
| day_of_week| VARCHAR(10) | NOT NULL                    | Día (Lunes, Martes, etc.)      |
| position   | INTEGER     | NOT NULL                    | Orden dentro del día           |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW()      | Fecha de creación              |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT NOW()      | Fecha de modificación          |

---

## Rutas completas

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

### Rate limiting

Las rutas `/wardrobe/analyze` (POST) y `/recommendation` (GET) tienen rate limiting por IP:

| Endpoint          | Capacidad | Ventana     |
|-------------------|-----------|-------------|
| POST /wardrobe/analyze | 10 solicitudes | 60 minutos |
| GET /recommendation   | 5 solicitudes  | 30 minutos |

Cuando se excede el límite, la aplicación responde con un error 429 y una vista amigable "Demasiadas solicitudes".

---

## Configuración de referencia

### `app.ai.*` — Servidor IA

| Propiedad           | Default                         | Descripción                                     |
|---------------------|---------------------------------|-------------------------------------------------|
| `app.ai.enabled`    | `true`                          | Activa/desactiva la integración con IA           |
| `app.ai.base-url`   | `https://api.nan.builders`      | URL base del servidor OpenAI-compatible          |
| `app.ai.chat-path`  | `/v1/chat/completions`          | Path del endpoint de chat                        |
| `app.ai.model`      | `qwen3.6`                       | Modelo a utilizar                               |
| `app.ai.api-key`    | `${NAN_API_KEY:}`               | API key (variable de entorno recomendada)        |
| `app.ai.max-tokens` | `500`                           | Máximo de tokens en la respuesta                |
| `app.ai.connect-timeout` | `5s`                        | Timeout de conexión                              |
| `app.ai.read-timeout` | `20s`                         | Timeout de lectura                               |

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

### `app.rate-limit.*` — Control de tasa

| Propiedad                              | Default | Descripción                          |
|----------------------------------------|---------|--------------------------------------|
| `app.rate-limit.analyze.capacity`      | `10`    | Máximo de análisis por ventana       |
| `app.rate-limit.analyze.refill-minutes` | `60`   | Minutos para recarga completa        |
| `app.rate-limit.recommendation.capacity` | `5`  | Máximo de recomendaciones por ventana|
| `app.rate-limit.recommendation.refill-minutes` | `30` | Minutos para recarga completa |

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
| `server.port`                          | `8080`                               | Puerto del servidor                      |

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

2. **Configurar la API key del servidor IA:**

```bash
export APP_AI_API_KEY=sk-tu-key-aqui
```

También se puede usar `NAN_API_KEY` (que es la variable que `application.yml` lee por defecto).

3. **Configurar el servidor IA** en `src/main/resources/application.yml` si no usa los defaults:

```yaml
app:
  ai:
    base-url: https://api.nan.builders
    chat-path: /v1/chat/completions
    model: qwen3.6
    api-key: ${APP_AI_API_KEY}
    max-tokens: 500
```

4. **Ejecutar la aplicación:**

```bash
mvn spring-boot:run
```

5. **Abrir en el navegador:**

```text
http://localhost:8080
```

> **Nota:** La primera vez que se ejecuta, Flyway aplica automáticamente las migraciones (`V1__create_garments.sql`, `V2__add_favorite_to_garments.sql`, `V3__create_week_plans.sql`).

---

## Flujo implementado

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

---

## Guía de testing

### Ejecutar tests

```bash
mvn test
```

Todos los tests usan JUnit 5. Los tests de integración con base de datos usan **H2 en memoria** (no PostgreSQL), configurado en `src/test/resources/application-test.yml`.

### Tests existentes (12 clases)

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

### Tests con WireMock

Los servicios que se comunican con el servidor IA (`AiClassificationService`, `AiRecommendationService`) usan WireMock para simular respuestas HTTP. El servidor WireMock se levanta en un puerto aleatorio antes de cada test y se detiene al finalizar.

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

Con IA desactivada, las prendas se cargan completamente a mano (el formulario de revisión aparece sin datos de clasificación).

---

## Solución de problemas

| Problema | Causa probable | Solución |
|----------|---------------|----------|
| `missing table garments` | Las tablas fueron eliminadas (tests con H2 u otro proceso) | Ejecutar `mvn flyway:migrate ...` para recrear las tablas |
| `relation "garments" does not exist` | Flyway no se ejecutó | Verificar `spring.flyway.enabled=true` y conectar a PostgreSQL |
| `APP_AI_API_KEY is required` | Falta la variable de entorno | `export APP_AI_API_KEY=sk-tu-key-aqui` o desactivar IA con `app.ai.enabled=false` |
| 429 Too Many Requests | Se excedió el rate limit | Esperar a que se recargue (análisis: 60 min, recomendaciones: 30 min) |
| `La imagen supera el tamano maximo permitido` | Archivo > 8 MB | Redimensionar o comprimir la imagen antes de subir |
| `El archivo no es una imagen válida` | Formato no soportado o archivo corrupto | Usar JPG, PNG o WebP válidos |
| `Connection refused: localhost/127.0.0.1:55432` | PostgreSQL no está corriendo | `docker compose up -d postgres` y esperar al healthcheck |
| La clasificación IA falla siempre | API key incorrecta o servidor no accesible | Verificar `APP_AI_API_KEY` y `app.ai.base-url` |
| Las imágenes nuevas no se ven | El directorio `uploads/` no existe o no tiene permisos | `mkdir -p uploads` o verificar permisos de escritura |
| Las vistas no se actualizan | Caché del navegador | Hard refresh (Cmd+Shift+R) o abrir en incógnito |
| Error al hacer clic en "Guardar" | Validación del formulario | Revisar que todos los campos obligatorios estén completos |
