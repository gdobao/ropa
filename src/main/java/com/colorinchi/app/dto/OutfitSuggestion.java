package com.colorinchi.app.dto;

import java.util.List;

public record OutfitSuggestion(String name, String description, List<OutfitPiece> pieces) {
}
