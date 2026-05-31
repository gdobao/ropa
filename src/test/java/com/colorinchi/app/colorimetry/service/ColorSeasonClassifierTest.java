package com.colorinchi.app.colorimetry.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.colorinchi.app.colorimetry.config.ColorimetryProperties;
import com.colorinchi.app.colorimetry.data.ColorPaletteStore;
import com.colorinchi.app.colorimetry.model.ColorDepth;
import com.colorinchi.app.colorimetry.model.ColorProfile;
import com.colorinchi.app.colorimetry.model.ColorSeason;

class ColorSeasonClassifierTest {

    private ColorSeasonClassifier classifier;

    @BeforeEach
    void setUp() {
        ColorimetryProperties props = ColorimetryProperties.defaults();
        ColorPaletteStore paletteStore = new ColorPaletteStore(props);
        classifier = new ColorSeasonClassifier(paletteStore, props);
    }

    @Test
    void coralIsSpring() {
        ColorProfile profile = classifier.classify("#FF7F50");
        assertEquals(ColorSeason.SPRING, profile.season());
    }

    @Test
    void lavandaIsSummer() {
        ColorProfile profile = classifier.classify("#B2A4D4");
        assertEquals(ColorSeason.SUMMER, profile.season());
    }

    @Test
    void mostazaIsAutumn() {
        ColorProfile profile = classifier.classify("#DAA520");
        assertEquals(ColorSeason.AUTUMN, profile.season());
    }

    @Test
    void azulKleinIsWinter() {
        ColorProfile profile = classifier.classify("#002FA7");
        assertEquals(ColorSeason.WINTER, profile.season());
    }

    @Test
    void consistentClassification() {
        ColorProfile first = classifier.classify("#FF0000");
        ColorProfile second = classifier.classify("#FF0000");
        assertEquals(first.season(), second.season());
        assertEquals(first.confidence(), second.confidence(), 0.001);
    }

    @Test
    void blackClassifies() {
        ColorProfile profile = classifier.classify("#000000");
        assertNotNull(profile);
        assertNotNull(profile.lab());
    }

    @Test
    void whiteClassifies() {
        ColorProfile profile = classifier.classify("#FFFFFF");
        assertNotNull(profile);
        assertNotNull(profile.lab());
    }

    @Test
    void nullHexThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> classifier.classify(null));
    }

    @Test
    void emptyHexThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> classifier.classify(""));
    }

    @Test
    void darkColorHasDarkDepth() {
        ColorProfile profile = classifier.classify("#0A0A0A");
        assertEquals(ColorDepth.DARK, profile.depth());
    }

    @Test
    void mediumColorHasMediumDepth() {
        ColorProfile profile = classifier.classify("#505050");
        assertEquals(ColorDepth.MEDIUM, profile.depth());
    }

    @Test
    void lightColorHasLightDepth() {
        ColorProfile profile = classifier.classify("#CCCCCC");
        assertEquals(ColorDepth.LIGHT, profile.depth());
    }

    @Test
    void depthBoundariesAreCorrect() {
        ColorProfile dark = classifier.classify("#3B3B3B");
        assertEquals(ColorDepth.DARK, dark.depth());

        ColorProfile mediumLow = classifier.classify("#3C3C3C");
        assertEquals(ColorDepth.MEDIUM, mediumLow.depth());

        ColorProfile mediumHigh = classifier.classify("#757575");
        assertEquals(ColorDepth.MEDIUM, mediumHigh.depth());

        ColorProfile light = classifier.classify("#787878");
        assertEquals(ColorDepth.LIGHT, light.depth());
    }
}
