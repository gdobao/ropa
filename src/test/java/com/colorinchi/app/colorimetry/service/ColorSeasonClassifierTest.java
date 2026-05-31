package com.colorinchi.app.colorimetry.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.colorinchi.app.colorimetry.config.ColorimetryProperties;
import com.colorinchi.app.colorimetry.data.ColorPaletteStore;
import com.colorinchi.app.colorimetry.model.ColorProfile;
import com.colorinchi.app.colorimetry.model.ColorSeason;

class ColorSeasonClassifierTest {

    private ColorSeasonClassifier classifier;

    @BeforeEach
    void setUp() {
        ColorPaletteStore paletteStore = new ColorPaletteStore();
        ColorimetryProperties props = ColorimetryProperties.defaults();
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
        assertThrows(RuntimeException.class, () -> classifier.classify(null));
    }

    @Test
    void emptyHexThrowsException() {
        assertThrows(RuntimeException.class, () -> classifier.classify(""));
    }
}
