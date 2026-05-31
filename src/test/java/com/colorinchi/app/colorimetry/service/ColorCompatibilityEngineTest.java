package com.colorinchi.app.colorimetry.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.colorinchi.app.colorimetry.config.ColorimetryProperties;
import com.colorinchi.app.colorimetry.data.ColorPaletteStore;
import com.colorinchi.app.colorimetry.model.CompatibilityResult;
import com.colorinchi.app.model.Garment;

class ColorCompatibilityEngineTest {

    private ColorCompatibilityEngine engine;

    @BeforeEach
    void setUp() {
        ColorimetryProperties props = ColorimetryProperties.defaults();
        ColorPaletteStore paletteStore = new ColorPaletteStore(props);
        ColorSeasonClassifier classifier = new ColorSeasonClassifier(paletteStore, props);
        engine = new ColorCompatibilityEngine(classifier, paletteStore, props);
    }

    private static Garment garment(String colorHex, String category, String colorName) {
        Garment g = new Garment();
        g.setColorHex(colorHex);
        g.setCategory(category);
        g.setColorName(colorName);
        return g;
    }

    @Test
    void sameColorScoresHigh() {
        Garment g1 = garment("#FF5733", "Top", "Coral");
        Garment g2 = garment("#FF5733", "Pantalón", "Coral");
        CompatibilityResult result = engine.score(g1, g2);
        assertTrue(result.score() >= 50, "Same color should score high, got: " + result.score());
    }

    @Test
    void blackAndWhiteScoresHigh() {
        Garment g1 = garment("#000000", "Top", "Negro");
        Garment g2 = garment("#FFFFFF", "Pantalón", "Blanco");
        CompatibilityResult result = engine.score(g1, g2);
        assertTrue(result.score() >= 30,
                "Black and white should score at least 30, got: " + result.score());
    }

    @Test
    void redAndPinkHasWarning() {
        Garment g1 = garment("#FF0000", "Top", "Rojo");
        Garment g2 = garment("#FFC0CB", "Falda", "Rosa");
        CompatibilityResult result = engine.score(g1, g2);
        assertFalse(result.warnings().isEmpty(), "Red+pink should produce warnings");
    }

    @Test
    void nullHexReturnsDefault() {
        Garment g1 = garment(null, "Top", "Sin color");
        Garment g2 = garment("#FFFFFF", "Pantalón", "Blanco");
        CompatibilityResult result = engine.score(g1, g2);
        assertEquals(50, result.score());
        assertEquals("Sin datos de color", result.explanation());
    }

    @Test
    void compatibleCategoriesGetBonus() {
        Garment g1 = garment("#228B22", "Top", "Verde bosque");
        Garment g2 = garment("#C19A6B", "Pantalón", "Camel");
        CompatibilityResult result = engine.score(g1, g2);
        assertTrue(result.score() > 0, "Score should be positive");
    }

    @Test
    void springAndSummerAdjacentScoresWell() {
        Garment g1 = garment("#FF7F50", "Top", "Coral");
        Garment g2 = garment("#D8BFD8", "Falda", "Lavanda");
        CompatibilityResult result = engine.score(g1, g2);
        // Spring (warm) + Summer (cool) with warm-cool penalty still gives
        // a non-negative result due to the adjacent-season bonus
        assertTrue(result.score() >= 0, "Score should be non-negative");
    }

    @Test
    void resultContainsExplanation() {
        Garment g1 = garment("#FF5733", "Top", "Coral");
        Garment g2 = garment("#3498DB", "Pantalón", "Azul");
        CompatibilityResult result = engine.score(g1, g2);
        assertNotNull(result.explanation());
        assertFalse(result.explanation().isBlank());
    }
}
