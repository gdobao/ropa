package com.colorinchi.app.colorimetry.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColorSpaceConverterTest {

    private static final double DELTA = 0.01;

    // ---------------------------------------------------------------
    // hexToRgb
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("hexToRgb")
    class HexToRgbTests {

        @Test
        @DisplayName("#FF0000 → R=255, G=0, B=0")
        void red() {
            double[] rgb = ColorSpaceConverter.hexToRgb("#FF0000");
            assertAll(
                    () -> assertEquals(255, rgb[0], DELTA),
                    () -> assertEquals(0, rgb[1], DELTA),
                    () -> assertEquals(0, rgb[2], DELTA));
        }

        @Test
        @DisplayName("#000000 → R=0, G=0, B=0")
        void black() {
            double[] rgb = ColorSpaceConverter.hexToRgb("#000000");
            assertAll(
                    () -> assertEquals(0, rgb[0], DELTA),
                    () -> assertEquals(0, rgb[1], DELTA),
                    () -> assertEquals(0, rgb[2], DELTA));
        }

        @Test
        @DisplayName("#FFFFFF → R=255, G=255, B=255")
        void white() {
            double[] rgb = ColorSpaceConverter.hexToRgb("#FFFFFF");
            assertAll(
                    () -> assertEquals(255, rgb[0], DELTA),
                    () -> assertEquals(255, rgb[1], DELTA),
                    () -> assertEquals(255, rgb[2], DELTA));
        }

        @Test
        @DisplayName("#0000FF → R=0, G=0, B=255")
        void blue() {
            double[] rgb = ColorSpaceConverter.hexToRgb("#0000FF");
            assertAll(
                    () -> assertEquals(0, rgb[0], DELTA),
                    () -> assertEquals(0, rgb[1], DELTA),
                    () -> assertEquals(255, rgb[2], DELTA));
        }

        @Test
        @DisplayName("without # prefix (FF0000) → R=255, G=0, B=0")
        void withoutHash() {
            double[] rgb = ColorSpaceConverter.hexToRgb("FF0000");
            assertAll(
                    () -> assertEquals(255, rgb[0], DELTA),
                    () -> assertEquals(0, rgb[1], DELTA),
                    () -> assertEquals(0, rgb[2], DELTA));
        }

        @Test
        @DisplayName("lowercase (#ff0000) should work")
        void lowercase() {
            double[] rgb = ColorSpaceConverter.hexToRgb("#ff0000");
            assertAll(
                    () -> assertEquals(255, rgb[0], DELTA),
                    () -> assertEquals(0, rgb[1], DELTA),
                    () -> assertEquals(0, rgb[2], DELTA));
        }
    }

    // ---------------------------------------------------------------
    // hexToRgb — error cases
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("hexToRgb error cases")
    class HexToRgbErrorTests {

        @Test
        @DisplayName("null throws NullPointerException")
        void nullHex() {
            assertThrows(NullPointerException.class,
                    () -> ColorSpaceConverter.hexToRgb(null));
        }

        @Test
        @DisplayName("empty string throws IllegalArgumentException")
        void emptyHex() {
            assertThrows(IllegalArgumentException.class,
                    () -> ColorSpaceConverter.hexToRgb(""));
        }

        @Test
        @DisplayName("invalid hex throws IllegalArgumentException")
        void invalidHex() {
            assertThrows(IllegalArgumentException.class,
                    () -> ColorSpaceConverter.hexToRgb("#GGGGGG"));
        }

        @Test
        @DisplayName("short hex (#FFF) throws IllegalArgumentException")
        void shortHex() {
            assertThrows(IllegalArgumentException.class,
                    () -> ColorSpaceConverter.hexToRgb("#FFF"));
        }
    }

    // ---------------------------------------------------------------
    // rgbToXyz
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("rgbToXyz")
    class RgbToXyzTests {

        @Test
        @DisplayName("black (0,0,0) → (0,0,0)")
        void black() {
            double[] xyz = ColorSpaceConverter.rgbToXyz(0, 0, 0);
            assertAll(
                    () -> assertEquals(0, xyz[0], DELTA),
                    () -> assertEquals(0, xyz[1], DELTA),
                    () -> assertEquals(0, xyz[2], DELTA));
        }

        @Test
        @DisplayName("white (255,255,255) → (95.047, 100.0, 108.883)")
        void white() {
            double[] xyz = ColorSpaceConverter.rgbToXyz(255, 255, 255);
            assertAll(
                    () -> assertEquals(95.047, xyz[0], DELTA),
                    () -> assertEquals(100.0, xyz[1], DELTA),
                    () -> assertEquals(108.883, xyz[2], DELTA));
        }
    }

    // ---------------------------------------------------------------
    // hexToLab
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("hexToLab")
    class HexToLabTests {

        @Test
        @DisplayName("#000000 → L≈0, a≈0, b≈0")
        void black() {
            double[] lab = ColorSpaceConverter.hexToLab("#000000");
            assertAll(
                    () -> assertEquals(0, lab[0], DELTA),
                    () -> assertEquals(0, lab[1], DELTA),
                    () -> assertEquals(0, lab[2], DELTA));
        }

        @Test
        @DisplayName("#FFFFFF → L≈100, a≈0, b≈0")
        void white() {
            double[] lab = ColorSpaceConverter.hexToLab("#FFFFFF");
            assertAll(
                    () -> assertEquals(100, lab[0], DELTA),
                    () -> assertEquals(0, lab[1], DELTA),
                    () -> assertEquals(0, lab[2], DELTA));
        }
    }

    // ---------------------------------------------------------------
    // hexToHsl
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("hexToHsl")
    class HexToHslTests {

        @Test
        @DisplayName("#FF0000 → H=0, S=100, L=50")
        void red() {
            double[] hsl = ColorSpaceConverter.hexToHsl("#FF0000");
            assertAll(
                    () -> assertEquals(0, hsl[0], DELTA),
                    () -> assertEquals(100, hsl[1], DELTA),
                    () -> assertEquals(50, hsl[2], DELTA));
        }

        @Test
        @DisplayName("#00FF00 → H=120, S=100, L=50")
        void green() {
            double[] hsl = ColorSpaceConverter.hexToHsl("#00FF00");
            assertAll(
                    () -> assertEquals(120, hsl[0], DELTA),
                    () -> assertEquals(100, hsl[1], DELTA),
                    () -> assertEquals(50, hsl[2], DELTA));
        }
    }

    // ---------------------------------------------------------------
    // deltaE2000
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("deltaE2000")
    class DeltaE2000Tests {

        @Test
        @DisplayName("same color → 0")
        void sameColor() {
            double[] lab = {50.0, 10.0, -10.0};
            assertEquals(0, ColorSpaceConverter.deltaE2000(lab, lab), DELTA);
        }

        @Test
        @DisplayName("black vs white → ≈100")
        void blackVsWhite() {
            double[] black = ColorSpaceConverter.hexToLab("#000000");
            double[] white = ColorSpaceConverter.hexToLab("#FFFFFF");
            double de = ColorSpaceConverter.deltaE2000(black, white);
            assertEquals(100, de, 5.0);
        }

        @Test
        @DisplayName("red vs pink → non-zero, less than 100")
        void redVsPink() {
            double[] red = ColorSpaceConverter.hexToLab("#FF0000");
            double[] pink = ColorSpaceConverter.hexToLab("#FFC0CB");
            double de = ColorSpaceConverter.deltaE2000(red, pink);
            assertAll(
                    () -> assertTrue(de > 0, "ΔE00 should be > 0 for different colours"),
                    () -> assertTrue(de < 100, "ΔE00 should be < 100 for similar colours"));
        }

        @Test
        @DisplayName("known pair → ≈3.511")
        void knownPair() {
            double[] lab1 = {50, 2.6772, -79.7751};
            double[] lab2 = {50, 0, -82.7485};
            double de = ColorSpaceConverter.deltaE2000(lab1, lab2);
            assertEquals(3.511, de, 0.01);
        }
    }
}
