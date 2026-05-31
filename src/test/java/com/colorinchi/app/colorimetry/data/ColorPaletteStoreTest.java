package com.colorinchi.app.colorimetry.data;

import static org.junit.jupiter.api.Assertions.*;

import com.colorinchi.app.colorimetry.model.ColorSeason;
import com.colorinchi.app.colorimetry.model.NamedColor;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColorPaletteStoreTest {

    private ColorPaletteStore store;

    @BeforeEach
    void setUp() {
        store = new ColorPaletteStore();
    }

    // ---------------------------------------------------------------
    // Palette structure
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("palette structure")
    class PaletteStructureTests {

        @Test
        @DisplayName("SPRING has ~25 colors and is not empty")
        void springNotEmpty() {
            assertNotNull(store.getPalette(ColorSeason.SPRING));
            assertFalse(store.getPalette(ColorSeason.SPRING).isEmpty());
            assertEquals(25, store.getPalette(ColorSeason.SPRING).size());
        }

        @Test
        @DisplayName("SUMMER has ~25 colors and is not empty")
        void summerNotEmpty() {
            assertNotNull(store.getPalette(ColorSeason.SUMMER));
            assertFalse(store.getPalette(ColorSeason.SUMMER).isEmpty());
            assertEquals(25, store.getPalette(ColorSeason.SUMMER).size());
        }

        @Test
        @DisplayName("AUTUMN has ~25 colors and is not empty")
        void autumnNotEmpty() {
            assertNotNull(store.getPalette(ColorSeason.AUTUMN));
            assertFalse(store.getPalette(ColorSeason.AUTUMN).isEmpty());
            assertEquals(25, store.getPalette(ColorSeason.AUTUMN).size());
        }

        @Test
        @DisplayName("WINTER has ~25 colors and is not empty")
        void winterNotEmpty() {
            assertNotNull(store.getPalette(ColorSeason.WINTER));
            assertFalse(store.getPalette(ColorSeason.WINTER).isEmpty());
            assertEquals(25, store.getPalette(ColorSeason.WINTER).size());
        }
    }

    // ---------------------------------------------------------------
    // getAllPalettes
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAllPalettes")
    class GetAllPalettesTests {

        @Test
        @DisplayName("returns all 4 seasons")
        void returnsAllFourSeasons() {
            Map<ColorSeason, List<NamedColor>> palettes = store.getAllPalettes();

            assertEquals(4, palettes.size());
            assertTrue(palettes.containsKey(ColorSeason.SPRING));
            assertTrue(palettes.containsKey(ColorSeason.SUMMER));
            assertTrue(palettes.containsKey(ColorSeason.AUTUMN));
            assertTrue(palettes.containsKey(ColorSeason.WINTER));
        }

        @Test
        @DisplayName("total palette count is roughly 100")
        void totalCountIsRoughly100() {
            Map<ColorSeason, List<NamedColor>> palettes = store.getAllPalettes();

            int total = palettes.values().stream()
                    .mapToInt(List::size)
                    .sum();

            assertTrue(total > 90, "total should be at least 90, got " + total);
            assertTrue(total < 110, "total should be at most 110, got " + total);
        }
    }

    // ---------------------------------------------------------------
    // getPalette — correct typing
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getPalette")
    class GetPaletteTests {

        @Test
        @DisplayName("SPRING palette contains NamedColor elements with correct season")
        void springPaletteIsCorrectlyTyped() {
            List<NamedColor> spring = store.getPalette(ColorSeason.SPRING);

            NamedColor first = spring.getFirst();
            assertInstanceOf(NamedColor.class, first);

            for (NamedColor color : spring) {
                assertEquals(ColorSeason.SPRING, color.season(),
                        "every SPRING color should have season = SPRING");
            }
        }

        @Test
        @DisplayName("SUMMER palette has correct season on every entry")
        void summerPaletteHasCorrectSeason() {
            List<NamedColor> summer = store.getPalette(ColorSeason.SUMMER);

            for (NamedColor color : summer) {
                assertEquals(ColorSeason.SUMMER, color.season());
            }
        }

        @Test
        @DisplayName("AUTUMN palette has correct season on every entry")
        void autumnPaletteHasCorrectSeason() {
            List<NamedColor> autumn = store.getPalette(ColorSeason.AUTUMN);

            for (NamedColor color : autumn) {
                assertEquals(ColorSeason.AUTUMN, color.season());
            }
        }

        @Test
        @DisplayName("WINTER palette has correct season on every entry")
        void winterPaletteHasCorrectSeason() {
            List<NamedColor> winter = store.getPalette(ColorSeason.WINTER);

            for (NamedColor color : winter) {
                assertEquals(ColorSeason.WINTER, color.season());
            }
        }
    }

    // ---------------------------------------------------------------
    // isNeutral
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("isNeutral")
    class IsNeutralTests {

        @Test
        @DisplayName("#000000 (negro) is neutral")
        void blackIsNeutral() {
            assertTrue(store.isNeutral("#000000"));
        }

        @Test
        @DisplayName("#FFFFFF (blanco) is neutral")
        void whiteIsNeutral() {
            assertTrue(store.isNeutral("#FFFFFF"));
        }

        @Test
        @DisplayName("#FF0000 (rojo) is NOT neutral")
        void redIsNotNeutral() {
            assertFalse(store.isNeutral("#FF0000"));
        }

        @Test
        @DisplayName("multiple known neutrals return true")
        void knownNeutrals() {
            assertAll(
                    () -> assertTrue(store.isNeutral("#000000"), "negro"),
                    () -> assertTrue(store.isNeutral("#FFFFFF"), "blanco"),
                    () -> assertTrue(store.isNeutral("#808080"), "gris"),
                    () -> assertTrue(store.isNeutral("#F5F5DC"), "beige"),
                    () -> assertTrue(store.isNeutral("#C19A6B"), "camel")
            );
        }

        @Test
        @DisplayName("vivid colours are not neutral")
        void vividColoursAreNotNeutral() {
            assertAll(
                    () -> assertFalse(store.isNeutral("#FF0000"), "rojo"),
                    () -> assertFalse(store.isNeutral("#00FF00"), "verde lima"),
                    () -> assertFalse(store.isNeutral("#0000FF"), "azul"),
                    () -> assertFalse(store.isNeutral("#FF00FF"), "fucsia")
            );
        }
    }

    // ---------------------------------------------------------------
    // classifyByNearestNeighbor
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("classifyByNearestNeighbor")
    class ClassifyByNearestNeighborTests {

        @Test
        @DisplayName("calling twice with same hex returns the same result")
        void isConsistent() {
            ColorSeason first = store.classifyByNearestNeighbor("#87CEEB");
            ColorSeason second = store.classifyByNearestNeighbor("#87CEEB");

            assertNotNull(first);
            assertEquals(first, second);
        }

        @Test
        @DisplayName("#FF7F50 (coral) is classified as SPRING")
        void coralIsSpring() {
            ColorSeason result = store.classifyByNearestNeighbor("#FF7F50");
            assertEquals(ColorSeason.SPRING, result);
        }

        @Test
        @DisplayName("#FFDB58 (mostaza) is classified as AUTUMN")
        void mustardIsAutumn() {
            ColorSeason result = store.classifyByNearestNeighbor("#FFDB58");
            assertEquals(ColorSeason.AUTUMN, result);
        }

        @Test
        @DisplayName("#002FA7 (azul klein) is classified as WINTER")
        void kleinBlueIsWinter() {
            ColorSeason result = store.classifyByNearestNeighbor("#002FA7");
            assertEquals(ColorSeason.WINTER, result);
        }

        @Test
        @DisplayName("#F4C2C2 (rosa empolvado) is classified as SUMMER")
        void dustyRoseIsSummer() {
            ColorSeason result = store.classifyByNearestNeighbor("#F4C2C2");
            assertEquals(ColorSeason.SUMMER, result);
        }

        @Test
        @DisplayName("#808080 (gris) returns null — classified as neutral-only")
        void greyIsNeutralSoSeasonIsNull() {
            // #808080 is only in NEUTRALS, not in any seasonal palette
            ColorSeason result = store.classifyByNearestNeighbor("#808080");
            assertNull(result, "grey should be closer to neutral than to any season");
        }

        @Test
        @DisplayName("call with many colours does not throw")
        void manyColoursDoNotThrow() {
            String[] colours = {
                    "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF",
                    "#00FFFF", "#000000", "#FFFFFF", "#808080", "#C0C0C0",
                    "#800000", "#808000", "#008000", "#800080", "#008080",
                    "#000080", "#FF7F50", "#FFDAB9", "#FFE135", "#FFFDD0"
            };
            for (String hex : colours) {
                assertDoesNotThrow(() -> store.classifyByNearestNeighbor(hex));
            }
        }
    }
}
