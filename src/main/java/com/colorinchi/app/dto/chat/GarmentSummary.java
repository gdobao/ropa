package com.colorinchi.app.dto.chat;

public record GarmentSummary(
    Long id,
    String name,
    String category,
    String colorName,
    String colorHex,
    String material,
    String season,
    boolean favorite
) {}
