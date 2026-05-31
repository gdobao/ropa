package com.colorinchi.app.colorimetry.model;

public enum ColorHarmony {

    MONOCHROME("Monocromático", "Variaciones en tono y saturación de un mismo color."),
    ANALOGOUS("Análogo", "Colores vecinos en el círculo cromático, crean armonía natural."),
    COMPLEMENTARY("Complementario", "Colores opuestos en el círculo cromático, alto contraste."),
    TRIADIC("Triádico", "Tres colores equidistantes en el círculo cromático, equilibrio vibrante."),
    NEUTRAL_BASE("Base neutra", "Combinación de neutros que permite destacar acentos de color.");

    private final String displayName;
    private final String description;

    ColorHarmony(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
