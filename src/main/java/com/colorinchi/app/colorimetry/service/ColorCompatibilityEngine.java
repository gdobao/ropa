package com.colorinchi.app.colorimetry.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.colorinchi.app.colorimetry.model.ColorTemperature.NEUTRAL;
import static com.colorinchi.app.colorimetry.model.ColorTemperature.SEMI_COOL;
import static com.colorinchi.app.colorimetry.model.ColorTemperature.SEMI_WARM;

import org.springframework.stereotype.Service;

import com.colorinchi.app.colorimetry.config.ColorimetryProperties;
import com.colorinchi.app.colorimetry.data.ColorPaletteStore;
import com.colorinchi.app.colorimetry.model.ColorHarmony;
import com.colorinchi.app.colorimetry.model.ColorProfile;
import com.colorinchi.app.colorimetry.model.ColorSeason;
import com.colorinchi.app.colorimetry.model.ColorTemperature;
import com.colorinchi.app.colorimetry.model.CompatibilityResult;
import com.colorinchi.app.colorimetry.util.ColorSpaceConverter;
import com.colorinchi.app.model.Garment;

/**
 * Computes a colour compatibility score between two {@link Garment garments}
 * based on CIEDE2000 colour distance, seasonal colour-analysis rules, and
 * category heuristics.
 *
 * <p>Sigmoid parameters are externalised to {@link ColorimetryProperties}.</p>
 */
@Service
public class ColorCompatibilityEngine {

    // ---- Thresholds (kept as constants — not config-driven) ----
    private static final double APPROXIMATE_THRESHOLD = 25.0;
    private static final double LOW_DELTA_E = 2.0;
    private static final double HIGH_DELTA_E = 20.0;

    // ---- Blacklist hexes ----
    private static final String ROJO_HEX = "#FF0000";
    private static final String ROSA_HEX = "#FFC0CB";
    private static final String NEGRO_HEX = "#000000";
    private static final String AZUL_MARINO_HEX = "#000080";

    // ---- Season adjacency ----
    private static final Set<SeasonPair> ADJACENT_FLOW = Set.of(
            new SeasonPair(ColorSeason.SPRING, ColorSeason.SUMMER),
            new SeasonPair(ColorSeason.AUTUMN, ColorSeason.WINTER));

    private static final Set<SeasonPair> OPPOSITE = Set.of(
            new SeasonPair(ColorSeason.SPRING, ColorSeason.AUTUMN),
            new SeasonPair(ColorSeason.SUMMER, ColorSeason.WINTER));

    // ---- Category groups ----
    private static final Set<String> TOP_CATEGORIES = Set.of("Top", "Camisa", "Sudadera");
    private static final Set<String> BOTTOM_CATEGORIES = Set.of("Pantalón", "Falda", "Vestido");
    private static final Set<String> LAYER_CATEGORIES = Set.of("Chaqueta", "Abrigo");

    private final ColorSeasonClassifier classifier;
    private final ColorPaletteStore paletteStore;
    private final ColorimetryProperties props;

    public ColorCompatibilityEngine(ColorSeasonClassifier classifier,
                                    ColorPaletteStore paletteStore,
                                    ColorimetryProperties props) {
        this.classifier = classifier;
        this.paletteStore = paletteStore;
        this.props = props;
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Compute a colour compatibility score between two garments.
     *
     * @return a {@link CompatibilityResult} with score (0–100), harmony label,
     *         explanation, and any warnings
     */
    public CompatibilityResult score(Garment garment1, Garment garment2) {
        String hex1 = garment1.getColorHex();
        String hex2 = garment2.getColorHex();

        if (hex1 == null || hex1.isBlank() || hex2 == null || hex2.isBlank()) {
            return new CompatibilityResult(50, ColorHarmony.NEUTRAL_BASE,
                    "Sin datos de color", List.of());
        }

        ColorProfile profile1 = classifier.classify(hex1);
        ColorProfile profile2 = classifier.classify(hex2);

        // ---- 1. DeltaE score ----
        double deltaE = ColorSpaceConverter.deltaE2000(
                ColorSpaceConverter.hexToLab(hex1),
                ColorSpaceConverter.hexToLab(hex2));
        double deltaEScore = sigmoidScore(deltaE);

        // Both neutral: high ΔE (e.g. black+white) means high contrast, which is
        // desirable — floor the sigmoid so neutral combos always have a base score.
        if (paletteStore.isNeutral(hex1) && paletteStore.isNeutral(hex2)) {
            deltaEScore = Math.max(deltaEScore, 50);
        }

        // ---- 2. Season score ----
        double seasonScore = computeSeasonScore(profile1, profile2, hex1, hex2);

        // ---- 3. Rule score ----
        double ruleScore = computeRuleScore(profile1, profile2, garment1, garment2, hex1, hex2);

        // ---- 4. Additive final score ----
        double raw = deltaEScore * props.deltaEWeight()
                   + seasonScore * props.seasonWeight()
                   + ruleScore * props.ruleWeight();
        int finalScore = clamp((int) Math.round(raw), 0, 100);

        // ---- 5. Harmony label ----
        ColorHarmony harmony = determineHarmony(finalScore);

        // ---- 6. Explanation & warnings ----
        String explanation = buildExplanation(finalScore, deltaE, profile1, profile2);
        List<String> warnings = buildWarnings(profile1, profile2, hex1, hex2, garment1, garment2);

        return new CompatibilityResult(finalScore, harmony, explanation, warnings);
    }

    // ---------------------------------------------------------------
    // Sigmoid
    // ---------------------------------------------------------------

    /**
     * Convert a CIEDE2000 delta-E into a 0–100 compatibility score using a
     * sigmoid function: {@code score = 100 / (1 + e^(steepness * (deltaE - midpoint)))}.
     *
     * <p>Very low deltaE (&lt;2) → high score; deltaE ≈ 12 → ~50; high deltaE (&gt;20) → low score.</p>
     */
    private double sigmoidScore(double deltaE) {
        return 100.0 / (1.0 + Math.exp(props.deltaESigmoidSteepness() * (deltaE - props.deltaESigmoidMidpoint())));
    }

    // ---------------------------------------------------------------
    // Season score
    // ---------------------------------------------------------------

    private static double computeSeasonScore(ColorProfile p1, ColorProfile p2, String hex1, String hex2) {
        ColorSeason s1 = p1.season();
        ColorSeason s2 = p2.season();

        // Both neutral → moderate positive score; they work together
        if (p1.temperature() == ColorTemperature.NEUTRAL
                && p2.temperature() == ColorTemperature.NEUTRAL) {
            return 15;
        }
        // Only one neutral → neutral acts as a bridge, no season impact
        if (p1.temperature() == ColorTemperature.NEUTRAL
                || p1.temperature() == ColorTemperature.SEMI_WARM
                || p1.temperature() == ColorTemperature.SEMI_COOL
                || p2.temperature() == ColorTemperature.NEUTRAL
                || p2.temperature() == ColorTemperature.SEMI_WARM
                || p2.temperature() == ColorTemperature.SEMI_COOL) {
            return 0;
        }

        if (s1 == null || s2 == null) {
            return 0;
        }

        SeasonPair pair = new SeasonPair(s1, s2);

        if (s1 == s2) {
            return 30;
        }
        if (ADJACENT_FLOW.contains(pair)) {
            return 15;
        }
        if (OPPOSITE.contains(pair)) {
            return -20;
        }
        return 0;
    }

    // ---------------------------------------------------------------
    // Rule score
    // ---------------------------------------------------------------

    private double computeRuleScore(ColorProfile p1, ColorProfile p2,
                                     Garment g1, Garment g2,
                                     String hex1, String hex2) {
        double score = 0;

        // -- Blacklist checks --
        if (isRojoRosaPair(hex1, hex2)) {
            score -= 10;
        }
        if (isNegroAzulMarinoPair(hex1, hex2)) {
            score -= 10;
        }
        if (isWarmCoolWithoutNeutral(p1, p2, hex1, hex2)) {
            score -= 20;
        }

        // -- Positive rules --
        if (isSameTemperatureFamily(p1.temperature(), p2.temperature())) {
            score += 15;
        }
        if (isBasePlusLayer(g1.getCategory(), g2.getCategory())) {
            score += 15;
        }

        return score;
    }

    // ---------------------------------------------------------------
    // Blacklist helpers
    // ---------------------------------------------------------------

    private boolean isRojoRosaPair(String hex1, String hex2) {
        return (isApproximately(hex1, ROJO_HEX) && isApproximately(hex2, ROSA_HEX))
                || (isApproximately(hex1, ROSA_HEX) && isApproximately(hex2, ROJO_HEX));
    }

    private boolean isNegroAzulMarinoPair(String hex1, String hex2) {
        return (isApproximately(hex1, NEGRO_HEX) && isApproximately(hex2, AZUL_MARINO_HEX))
                || (isApproximately(hex1, AZUL_MARINO_HEX) && isApproximately(hex2, NEGRO_HEX));
    }

    private boolean isWarmCoolWithoutNeutral(ColorProfile p1, ColorProfile p2,
                                              String hex1, String hex2) {
        boolean warmCool = (p1.temperature() == ColorTemperature.WARM
                && p2.temperature() == ColorTemperature.COOL)
                || (p1.temperature() == ColorTemperature.COOL
                && p2.temperature() == ColorTemperature.WARM);
        if (!warmCool) {
            return false;
        }
        // Check if neither garment is a neutral — if at least one is neutral it bridges
        return !paletteStore.isNeutral(hex1) && !paletteStore.isNeutral(hex2);
    }

    // ---------------------------------------------------------------
    // Positive rule helpers
    // ---------------------------------------------------------------

    private static boolean isSameTemperatureFamily(ColorTemperature t1, ColorTemperature t2) {
        if (t1 == ColorTemperature.NEUTRAL || t2 == ColorTemperature.NEUTRAL) {
            return true;
        }
        if (t1 == ColorTemperature.SEMI_WARM || t1 == ColorTemperature.SEMI_COOL
                || t2 == ColorTemperature.SEMI_WARM || t2 == ColorTemperature.SEMI_COOL) {
            return true;
        }
        return t1 == t2;
    }

    private static boolean isBasePlusLayer(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return false;
        // Top + Bottom
        if ((TOP_CATEGORIES.contains(cat1) && BOTTOM_CATEGORIES.contains(cat2)) ||
            (BOTTOM_CATEGORIES.contains(cat2) && TOP_CATEGORIES.contains(cat1))) {
            return true;
        }
        // Top/Bottom + Layer
        if ((TOP_CATEGORIES.contains(cat1) || BOTTOM_CATEGORIES.contains(cat1))
            && LAYER_CATEGORIES.contains(cat2)) return true;
        if ((TOP_CATEGORIES.contains(cat2) || BOTTOM_CATEGORIES.contains(cat2))
            && LAYER_CATEGORIES.contains(cat1)) return true;
        // Layer + Bottom
        if ((LAYER_CATEGORIES.contains(cat1) && BOTTOM_CATEGORIES.contains(cat2)) ||
            (LAYER_CATEGORIES.contains(cat2) && BOTTOM_CATEGORIES.contains(cat1))) return true;
        return false;
    }

    // ---------------------------------------------------------------
    // Colour proximity
    // ---------------------------------------------------------------

    /**
     * Returns {@code true} when the perceptual distance (CIEDE76) between
     * {@code hex} and {@code target} is below {@link #APPROXIMATE_THRESHOLD}.
     */
    private static boolean isApproximately(String hex, String target) {
        double[] lab = ColorSpaceConverter.hexToLab(hex);
        double[] targetLab = ColorSpaceConverter.hexToLab(target);
        return ColorSpaceConverter.deltaE76(lab, targetLab) < APPROXIMATE_THRESHOLD;
    }

    // ---------------------------------------------------------------
    // Harmony & explanation
    // ---------------------------------------------------------------

    private static ColorHarmony determineHarmony(int score) {
        if (score > 75) {
            return ColorHarmony.ANALOGOUS;
        } else if (score > 55) {
            return ColorHarmony.NEUTRAL_BASE;
        } else if (score > 30) {
            return ColorHarmony.COMPLEMENTARY;
        }
        return ColorHarmony.MONOCHROME;
    }

    private static String buildExplanation(int score, double deltaE,
                                            ColorProfile p1, ColorProfile p2) {
        String season1 = p1.season() != null ? p1.season().displayName() : "indefinida";
        String season2 = p2.season() != null ? p2.season().displayName() : "indefinida";

        return "Distancia de color ΔE = " + String.format("%.1f", deltaE)
                + ". Perfiles estacionales: " + season1 + " / " + season2 + ". "
                + "Puntuación final: " + score + "/100.";
    }

    private List<String> buildWarnings(ColorProfile p1, ColorProfile p2,
                                        String hex1, String hex2,
                                        Garment g1, Garment g2) {
        List<String> warnings = new ArrayList<>();

        if (isRojoRosaPair(hex1, hex2)) {
            warnings.add("Rojo y rosa juntos pueden crear un efecto visualmente denso. "
                    + "Considera separarlos con un neutral.");
        }
        if (isNegroAzulMarinoPair(hex1, hex2)) {
            warnings.add("Negro y azul marino apenas se distinguen. "
                    + "Prueba con un contraste más claro.");
        }
        if (isWarmCoolWithoutNeutral(p1, p2, hex1, hex2)) {
            warnings.add("Combinación cálido + frío sin un neutral de por medio. "
                    + "Un color neutro puede equilibrar el contraste.");
        }

        return warnings;
    }

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Value-based pair that treats (A, B) the same as (B, A) — ideal for
     * unordered season-relationship lookups.
     */
    private record SeasonPair(ColorSeason first, ColorSeason second) {
        SeasonPair {
            // Normalise order so that (SPRING, SUMMER) equals (SUMMER, SPRING)
            if (first == null || second == null) {
                throw new IllegalArgumentException("Seasons must not be null");
            }
        }

        /**
         * Produce a canonical ordering based on enum ordinal so that
         * {@code equals/hashCode} are order-independent.
         */
        private ColorSeason canonicalMin() {
            return first.ordinal() <= second.ordinal() ? first : second;
        }

        private ColorSeason canonicalMax() {
            return first.ordinal() > second.ordinal() ? first : second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SeasonPair other)) return false;
            return canonicalMin() == other.canonicalMin()
                    && canonicalMax() == other.canonicalMax();
        }

        @Override
        public int hashCode() {
            return 31 * canonicalMin().hashCode() + canonicalMax().hashCode();
        }
    }
}
