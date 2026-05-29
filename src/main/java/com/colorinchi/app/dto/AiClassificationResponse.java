package com.colorinchi.app.dto;

import java.math.BigDecimal;

public record AiClassificationResponse(
        String type,
        String colorName,
        String colorHex,
        BigDecimal confidence,
        String model,
        String error) {

    public static AiClassificationResponse empty(String model, String error) {
        return new AiClassificationResponse("", "", "", BigDecimal.ZERO, model, error);
    }

    public boolean hasPrediction() {
        return type != null && !type.isBlank() && colorName != null && !colorName.isBlank();
    }
}
