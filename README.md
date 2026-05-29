# Armario Capsula

Aplicacion local en Java 21 + Spring Boot 3 para cargar prendas desde imagen, sugerir tipo/color con un servidor IA propio y confirmar los datos antes de guardarlos.

## Stack

| Capa | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.4 |
| Build | Maven |
| UI | Thymeleaf mobile-first |
| DB | PostgreSQL 16 + Flyway |
| IA vision | Servidor local OpenAI-compatible |
| Storage | Filesystem local en `uploads/` |

## Levantar local

1. Iniciar PostgreSQL:

```bash
docker compose up -d postgres
```

La base queda expuesta en `localhost:55432` para no chocar con instalaciones locales en `5432`.

2. Configurar la API key del servidor IA:

```bash
export APP_AI_API_KEY=sk-tu-key-aqui
```

3. Configurar el servidor IA en `src/main/resources/application.yml` si no usa los defaults:

```yaml
app:
  ai:
    base-url: https://api.nan.builders
    chat-path: /v1/chat/completions
    model: qwen3.6
    api-key: ${APP_AI_API_KEY}
    max-tokens: 500
```

4. Ejecutar la app:

```bash
mvn spring-boot:run
```

5. Abrir:

```text
http://localhost:8080
```

## Flujo implementado

1. `+` abre `/wardrobe/new`.
2. Se sube una imagen JPG, PNG o WebP.
3. La app comprime la imagen y consulta el servidor IA.
4. La usuaria confirma o corrige tipo/color.
5. La prenda queda visible en `/wardrobe` y `/wardrobe/{id}`.

Si el servidor IA no responde, el flujo no se bloquea: permite completar la prenda manualmente.
