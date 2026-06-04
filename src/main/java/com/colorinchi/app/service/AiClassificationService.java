package com.colorinchi.app.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.colorinchi.app.config.AiServerProperties;
import com.colorinchi.app.config.UploadProperties;
import com.colorinchi.app.dto.AiClassificationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiClassificationService {

    private static final Logger log = LoggerFactory.getLogger(AiClassificationService.class);

    private final AiServerProperties properties;
    private final UploadProperties uploadProperties;
    private final WebClient aiWebClient;
    private final ObjectMapper objectMapper;

    public AiClassificationService(
            AiServerProperties properties,
            UploadProperties uploadProperties,
            WebClient aiWebClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.uploadProperties = uploadProperties;
        this.aiWebClient = aiWebClient;
        this.objectMapper = objectMapper;
    }

    public AiClassificationResponse classify(String imageUrl) {
        if (!properties.enabled()) {
            return AiClassificationResponse.empty(properties.model(), "IA desactivada");
        }
        try {
            String imageDataUrl = toDataUrl(imageUrl);
            Map<String, Object> request = buildRequest(imageDataUrl);
            String response = aiWebClient.post()
                    .uri(properties.chatPath())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(properties.readTimeout());
            log.debug("Garment AI raw response: {}", truncate(response));
            return parseResponse(response);
        } catch (Exception ex) {
            log.warn("Garment AI classification failed: {}", ex.getMessage(), ex);
            return AiClassificationResponse.empty(properties.model(), "No pudimos analizar la imagen automáticamente. Cárgala manualmente.");
        }
    }

    private Map<String, Object> buildRequest(String imageDataUrl) {
        return Map.of(
                "model", properties.model(),
                "max_tokens", properties.maxTokens(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", "Analiza la imagen para cargar una prenda en un armario digital. Si no hay una prenda clara, responde type=Otro y confidence menor a 0.5. Responde solo JSON con estos campos exactos: type, colorName, colorHex, confidence. type debe ser uno de: Top, Pantalón, Vestido, Falda, Chaqueta, Abrigo, Camisa, Sudadera, Zapatos, Accesorio, Otro. colorName debe estar en español."),
                                Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))))));
    }

    private AiClassificationResponse parseResponse(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode message = root.path("choices").path(0).path("message");

        JsonNode toolArguments = message.path("tool_calls").path(0).path("function").path("arguments");
        String content = !toolArguments.isMissingNode() && !toolArguments.isNull()
                ? toolArguments.asText()
                : message.path("content").asText("{}");

        JsonNode payload = objectMapper.readTree(content);
        AiClassificationResponse parsed = new AiClassificationResponse(
                text(payload, "type"),
                text(payload, "colorName"),
                text(payload, "colorHex"),
                decimal(payload, "confidence"),
                properties.model(),
                null);
        if (!parsed.hasPrediction()) {
            log.warn("Garment AI response did not match expected schema. Content: {}", truncate(content));
        }
        return parsed;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 1200 ? value : value.substring(0, 1200) + "...";
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    private BigDecimal decimal(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return BigDecimal.ZERO;
        }
        return node.path(field).decimalValue();
    }

    private String toDataUrl(String imageUrl) throws IOException {
        Path path = resolveImagePath(imageUrl);
        String mimeType = Files.probeContentType(path);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        return "data:" + mimeType + ";base64," + base64;
    }

    private Path resolveImagePath(String imageUrl) {
        Path baseDir = uploadProperties.directory().toAbsolutePath().normalize();
        if (!imageUrl.startsWith("/uploads/")) {
            throw new SecurityException("Ruta de imagen no permitida: " + imageUrl);
        }
        Path resolved = baseDir.resolve(imageUrl.substring("/uploads/".length())).toAbsolutePath().normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new SecurityException("Intento de path traversal detectado: " + imageUrl);
        }
        return resolved;
    }
}
