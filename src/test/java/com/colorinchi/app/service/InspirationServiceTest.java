package com.colorinchi.app.service;

import org.junit.jupiter.api.Test;

import com.colorinchi.app.dto.InspirationLook;

import static org.assertj.core.api.Assertions.assertThat;

class InspirationServiceTest {

    private final InspirationService service = new InspirationService();

    @Test
    void getAllReturnsSixLooks() {
        var looks = service.getAll();
        assertThat(looks).hasSize(6);
    }

    @Test
    void getAllReturnsLooksWithAllFieldsPopulated() {
        var looks = service.getAll();
        assertThat(looks).allSatisfy(look -> {
            assertThat(look.id()).isPositive();
            assertThat(look.name()).isNotBlank();
            assertThat(look.description()).isNotBlank();
            assertThat(look.tags()).isNotEmpty();
            assertThat(look.vibe()).isNotBlank();
            assertThat(look.garmentTypes()).isNotEmpty();
            assertThat(look.color1()).isNotBlank();
            assertThat(look.color2()).isNotBlank();
            assertThat(look.color3()).isNotBlank();
        });
    }

    @Test
    void getLookByNameReturnsCorrectLook() {
        var look = service.getAll().stream()
                .filter(l -> "Cápsula oficina".equals(l.name()))
                .findFirst()
                .orElse(null);

        assertThat(look).isNotNull();
        assertThat(look.id()).isEqualTo(1);
        assertThat(look.tags()).contains("Minimal", "Oficina");
        assertThat(look.vibe()).isEqualTo("Oficina");
        assertThat(look.garmentTypes()).containsExactly("Top", "Pantalón", "Chaqueta", "Zapatos");
    }

    @Test
    void getLookByNameWithNonExistentNameReturnsNull() {
        var look = service.getAll().stream()
                .filter(l -> "No existe".equals(l.name()))
                .findFirst()
                .orElse(null);

        assertThat(look).isNull();
    }

    @Test
    void getAllTagsReturnsAllTags() {
        var tags = service.getAllTags();
        assertThat(tags).contains(
                "Minimal", "Oficina", "Elegante", "Cena",
                "Casual", "Finde", "Invierno", "Capas", "Sport");
    }

    @Test
    void lookNamesAreUnique() {
        var looks = service.getAll();
        var names = looks.stream().map(InspirationLook::name).toList();
        assertThat(names).doesNotHaveDuplicates();
    }
}
