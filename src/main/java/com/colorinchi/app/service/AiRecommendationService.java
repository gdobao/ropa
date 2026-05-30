package com.colorinchi.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.colorinchi.app.config.AiServerProperties;
import com.colorinchi.app.dto.AiRecommendationResponse;
import com.colorinchi.app.dto.OutfitPiece;
import com.colorinchi.app.dto.OutfitSuggestion;
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

        return "Sos un asesor de estilo experto. Trabajás con el guardarropa de una persona real."
                + " Proponé entre 3 y 5 outfits completos y usables.\n\n"
                + "REGLAS DE ESTILO:\n"
                + "- Cada outfit debe combinar colores que armonicen (neutros + 1 color de acento, monocromático con texturas distintas, o complementarios).\n"
                + "- NO pongas el MISMO color en todas las piezas del mismo outfit. Variá.\n"
                + "- Cada outfit debe incluir una mezcla de categorías distintas: parte superior + inferior + calzado + opcional abrigo/accesorio.\n"
                + "- NO repitas la misma prenda en distintos outfits.\n"
                + "- Los outfits deben ser variados entre sí (no todos formales ni todos casuales).\n"
                + "- Puntuá cada outfit del 1 al 10 según su armonía y estilo.\n\n"
                + "Responde SOLO JSON, sin texto adicional:\n"
                + "{\"outfits\":[{\"name\":\"\",\"description\":\"\",\"score\":8,\"pieces\":[{\"category\":\"\",\"colorName\":\"\",\"colorHex\":\"\"}]}]}\n\n"
                + "Guardarropa disponible:\n" + wardrobeLines;
    }

    private String garmentLine(Garment garment) {
        return "- " + safe(garment.getName())
                + " | category=" + safe(garment.getCategory())
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
            AiRecommendationResponse raw = objectMapper.readValue(content, AiRecommendationResponse.class);
            List<OutfitSuggestion> enriched = raw.outfits().stream()
                    .map(this::enrichOutfit)
                    .toList();
            return new AiRecommendationResponse(enriched);
        } catch (Exception ex) {
            log.warn("AI recommendations parse failed: {}", ex.getMessage(), ex);
            return AiRecommendationResponse.empty();
        }
    }

    private OutfitSuggestion enrichOutfit(OutfitSuggestion outfit) {
        List<OutfitPiece> sorted = outfit.pieces().stream()
                .map(p -> new OutfitPiece(
                        p.category(),
                        p.colorName(),
                        p.colorHex(),
                        OutfitPiece.zoneFor(p.category()),
                        OutfitPiece.isLightText(p.colorHex())))
                .sorted(Comparator.comparingInt(OutfitPiece::bodyZone))
                .toList();
        return new OutfitSuggestion(outfit.name(), outfit.description(), outfit.score(), sorted);
    }
}
