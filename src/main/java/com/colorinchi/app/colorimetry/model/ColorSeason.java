package com.colorinchi.app.colorimetry.model;

import java.util.Optional;

public enum ColorSeason {

    SPRING("Primavera"),
    SUMMER("Verano"),
    AUTUMN("Otoño"),
    WINTER("Invierno");

    private final String displayName;

    ColorSeason(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Optional<ColorSeason> fromString(String value) {
        if (value == null) {
            return Optional.empty();
        }
        for (ColorSeason season : values()) {
            if (season.displayName.equalsIgnoreCase(value)) {
                return Optional.of(season);
            }
        }
        return Optional.empty();
    }
}
