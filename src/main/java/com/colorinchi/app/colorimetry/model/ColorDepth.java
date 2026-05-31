package com.colorinchi.app.colorimetry.model;

public enum ColorDepth {

    LIGHT("Claro"),
    MEDIUM("Medio"),
    DARK("Oscuro");

    private final String displayName;

    ColorDepth(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
