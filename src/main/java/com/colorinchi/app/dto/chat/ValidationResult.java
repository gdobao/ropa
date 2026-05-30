package com.colorinchi.app.dto.chat;

import java.util.List;

public record ValidationResult(
    boolean isValid,
    List<String> warnings
) {
    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(String warning) {
        return new ValidationResult(false, List.of(warning));
    }

    public static ValidationResult withWarnings(List<String> warnings) {
        return new ValidationResult(true, warnings);
    }
}
