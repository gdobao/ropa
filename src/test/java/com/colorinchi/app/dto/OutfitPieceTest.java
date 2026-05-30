package com.colorinchi.app.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OutfitPieceTest {

    @ParameterizedTest
    @CsvSource({
        "Accesorio, 0",
        "Otro, 0",
        "Chaqueta, 1",
        "Abrigo, 1",
        "Camisa, 2",
        "Top, 2",
        "Sudadera, 2",
        "Vestido, 2",
        "Pantalón, 3",
        "Falda, 3",
        "Zapatos, 4",
        "Gorra, 99",
        ", 99"
    })
    void zoneForKnownAndUnknown(String category, int expectedZone) {
        assertThat(OutfitPiece.zoneFor(category)).isEqualTo(expectedZone);
    }

    @ParameterizedTest
    @CsvSource({
        "#FFFFFF, true",
        "#E8E8E8, true",
        "#000000, false",
        "#1A1A2E, false",
        "#FF0000, false",
        "#C0C0C0, true",
        "'', false",
        ", false",
        "invalid, false",
        "abc, false"
    })
    void isLightTextDetectsBrightness(String hex, boolean expected) {
        assertThat(OutfitPiece.isLightText(hex)).isEqualTo(expected);
    }

    @Test
    void constructorAndAccessors() {
        var piece = new OutfitPiece("Camisa", "Blanco", "#FFFFFF", 2, true);
        assertThat(piece.category()).isEqualTo("Camisa");
        assertThat(piece.colorName()).isEqualTo("Blanco");
        assertThat(piece.colorHex()).isEqualTo("#FFFFFF");
        assertThat(piece.bodyZone()).isEqualTo(2);
        assertThat(piece.lightText()).isTrue();
    }
}
