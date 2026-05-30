package com.colorinchi.app.config;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WardrobePropertiesTest {

    @Test
    void holdsAllValues() {
        var props = new WardrobeProperties(List.of("Camisa", "Pantalón"), List.of("Lunes", "Martes"), 5, 30, 3);
        assertThat(props.categories()).containsExactly("Camisa", "Pantalón");
        assertThat(props.days()).containsExactly("Lunes", "Martes");
        assertThat(props.colorLimit()).isEqualTo(5);
        assertThat(props.maxGarmentsInSummary()).isEqualTo(30);
        assertThat(props.upcomingDaysToInclude()).isEqualTo(3);
    }
}
