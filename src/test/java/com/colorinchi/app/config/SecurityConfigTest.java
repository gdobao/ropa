package com.colorinchi.app.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void configIsNotNull() {
        assertThat(config).isNotNull();
    }
}
