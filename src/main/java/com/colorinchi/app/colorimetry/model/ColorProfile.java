package com.colorinchi.app.colorimetry.model;

public record ColorProfile(
    ColorSeason season,
    ColorTemperature temperature,
    ColorIntensity intensity,
    ColorDepth depth,
    double confidence,
    double[] lab,
    String hex
) {

    /**
     * Factory method that derives a {@link ColorProfile} from CIELAB values and the original hex.
     * It determines temperature, intensity, depth and season heuristically.
     *
     * @param L                  CIELAB L* (0–100)
     * @param a                  CIELAB a* (green–red axis)
     * @param b                  CIELAB b* (blue–yellow axis)
     * @param hex                original hex colour (e.g. {@code #FF5733})
     * @param warmCoolThreshold  b* value above which a colour is considered warm,
     *                           and below which (negated) it is considered cool
     * @param intensityThreshold normalised-chroma value above which a colour
     *                           is considered {@link ColorIntensity#BRIGHT}
     * @return a fully derived {@code ColorProfile}
     */
    public static ColorProfile fromLab(double L, double a, double b, String hex,
                                       double warmCoolThreshold, double intensityThreshold) {
        // 1. Temperature
        ColorTemperature temperature;
        if (b > warmCoolThreshold) {
            temperature = ColorTemperature.WARM;
        } else if (b > warmCoolThreshold * 0.6) {
            temperature = ColorTemperature.SEMI_WARM;
        } else if (b < -warmCoolThreshold) {
            temperature = ColorTemperature.COOL;
        } else if (b < -warmCoolThreshold * 0.6) {
            temperature = ColorTemperature.SEMI_COOL;
        } else {
            temperature = ColorTemperature.NEUTRAL;
        }

        // 2. Intensity — chroma C = sqrt(a² + b²), normalised cap at 100
        double chroma = Math.sqrt(a * a + b * b);
        double normalisedChroma = Math.min(chroma, 100.0) / 100.0;
        ColorIntensity intensity;
        if (normalisedChroma > intensityThreshold) {
            intensity = ColorIntensity.BRIGHT;
        } else if (normalisedChroma > intensityThreshold * 0.4) {
            intensity = ColorIntensity.MEDIUM;
        } else {
            intensity = ColorIntensity.SOFT;
        }

        // 3. Depth
        ColorDepth depth;
        if (L < 25) {
            depth = ColorDepth.DARK;
        } else if (L <= 50) {
            depth = ColorDepth.MEDIUM;
        } else {
            depth = ColorDepth.LIGHT;
        }

        // 4. Season — four-season matrix
        boolean ambiguousTemp = temperature == ColorTemperature.NEUTRAL
                || temperature == ColorTemperature.SEMI_WARM
                || temperature == ColorTemperature.SEMI_COOL;
        boolean ambiguousDepth = depth == ColorDepth.MEDIUM;

        ColorSeason season;
        if (!ambiguousTemp && !ambiguousDepth && L > 50 && temperature == ColorTemperature.WARM && intensity == ColorIntensity.BRIGHT) {
            season = ColorSeason.SPRING;
        } else if (!ambiguousTemp && !ambiguousDepth && L > 50 && temperature == ColorTemperature.COOL && intensity == ColorIntensity.SOFT) {
            season = ColorSeason.SUMMER;
        } else if (!ambiguousTemp && !ambiguousDepth && L <= 50 && temperature == ColorTemperature.WARM && intensity == ColorIntensity.SOFT) {
            season = ColorSeason.AUTUMN;
        } else if (!ambiguousTemp && !ambiguousDepth && L <= 50 && temperature == ColorTemperature.COOL && intensity == ColorIntensity.BRIGHT) {
            season = ColorSeason.WINTER;
        } else {
            season = null;
        }

        // 5. Confidence
        double confidence = 1.0;
        if (ambiguousTemp) {
            confidence -= 0.2;
        }
        if (ambiguousDepth) {
            confidence -= 0.2;
        }
        if (season == null) {
            confidence -= 0.3;
        }
        confidence = Math.max(0.0, Math.min(1.0, confidence));

        return new ColorProfile(season, temperature, intensity, depth, confidence, new double[]{L, a, b}, hex);
    }
}
