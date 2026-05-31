package com.colorinchi.app.colorimetry.model;

public enum ColorTemperature {

    WARM("Cálido"),
    COOL("Frío"),
    NEUTRAL("Neutro");

    private final String displayName;

    ColorTemperature(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
