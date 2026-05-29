package com.colorinchi.app.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.colorinchi.app.config.AiServerProperties;
import com.colorinchi.app.dto.AiRecommendationResponse;
import com.colorinchi.app.model.Garment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

    private final WebClient aiWebClient;
    private final AiServerProperties properties;
    private final ObjectMapper objectMapper;
    private final GarmentService garmentService;

    public AiRecommendationService(
            WebClient aiWebClient,
            AiServerProperties properties,
            ObjectMapper objectMapper,
            GarmentService garmentService) {
        this.aiWebClient = aiWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.garmentService = garmentService;
    }

    public AiRecommendationResponse generate() {
        List<Garment> garments = garmentService.all();
        if (!properties.enabled() || garments.size() < 3) {
            return AiRecommendationResponse.empty();
        }

        try {
            String prompt = buildPrompt(garments);
            Map<String, Object> request = Map.of(
                    "model", properties.model(),
                    "max_tokens", properties.maxTokens(),
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", prompt)));

            String response = aiWebClient.post()
                    .uri(properties.chatPath())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(properties.readTimeout());

            return parseResponse(response);
        } catch (Exception ex) {
            log.warn("AI recommendations failed: {}", ex.getMessage(), ex);
            return AiRecommendationResponse.empty();
        }
    }

    private String buildPrompt(List<Garment> garments) {
        String wardrobeLines = garments.stream()
                .map(this::garmentLine)
                .collect(Collectors.joining("\n"));

        return "Sos un asesor de estilo. Con el guardarropa disponible, propone entre 1 y 3 outfits completos y usables. "
                + "Responde SOLO JSON con esta estructura exacta: "
                + "{\"outfits\":[{\"name\":\"\",\"description\":\"\",\"pieces\":[{\"category\":\"\",\"colorName\":\"\",\"colorHex\":\"\"}]}]}. "
                + "No agregues texto fuera del JSON.\n\n"
                + "Guardarropa:\n" + wardrobeLines;
    }

    private String garmentLine(Garment garment) {
        return "- category=" + safe(garment.getCategory())
                + ", colorName=" + safe(garment.getColorName())
                + ", colorHex=" + safe(garment.getColorHex())
                + ", material=" + safe(garment.getMaterial());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private AiRecommendationResponse parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode message = root.path("choices").path(0).path("message");
            String content = message.path("content").asText("{}");
            return objectMapper.readValue(content, AiRecommendationResponse.class);
        } catch (Exception ex) {
            log.warn("AI recommendations parse failed: {}", ex.getMessage(), ex);
            return AiRecommendationResponse.empty();
        }
    }
}
