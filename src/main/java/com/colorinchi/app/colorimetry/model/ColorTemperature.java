package com.colorinchi.app.colorimetry.model;

public enum ColorTemperature {

    WARM("Cálido"),
    SEMI_WARM("Semi-cálido"),
    COOL("Frío"),
    SEMI_COOL("Semi-frío"),
    NEUTRAL("Neutro");

    private final String displayName;

    ColorTemperature(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
