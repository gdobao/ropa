package com.colorinchi.app.colorimetry.model;

public enum ColorIntensity {

    BRIGHT("Vívido"),
    MEDIUM("Medio"),
    SOFT("Suave");

    private final String displayName;

    ColorIntensity(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
