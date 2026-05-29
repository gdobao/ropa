package com.colorinchi.app.dto;

import java.util.List;

public record AiRecommendationResponse(List<OutfitSuggestion> outfits) {

    public static AiRecommendationResponse empty() {
        return new AiRecommendationResponse(List.of());
    }
}
