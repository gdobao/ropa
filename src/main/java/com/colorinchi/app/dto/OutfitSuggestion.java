package com.colorinchi.app.dto;

import java.util.List;

public record OutfitSuggestion(String name, String description, int score, List<OutfitPiece> pieces) {
    public OutfitSuggestion {
        if (score < 1 || score > 10) score = 5;
    }
}
