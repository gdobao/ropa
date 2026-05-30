package com.colorinchi.app.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebClientConfigTest {

    private final WebClientConfig config = new WebClientConfig();

    @Test
    void aiWebClientUsesDefaultsWhenTimeoutsAreNull() {
        var props = new AiServerProperties(
                "http://localhost:8080", "/chat", "gpt-4", null,
                2000, true, null, null, null
        );
        var client = config.aiWebClient(props);
        assertThat(client).isNotNull();
    }

    @Test
    void aiWebClientUsesProvidedTimeouts() {
        var props = new AiServerProperties(
                "http://localhost:8080", "/chat", "gpt-4", "sk-123",
                2000, true, Duration.ofSeconds(10), Duration.ofSeconds(30),
                null
        );
        var client = config.aiWebClient(props);
        assertThat(client).isNotNull();
    }

    @Test
    void aiWebClientWithEmptyApiKey() {
        var props = new AiServerProperties(
                "http://localhost:8080", "/chat", "gpt-4", "",
                2000, true, Duration.ofSeconds(5), Duration.ofSeconds(20),
                null
        );
        var client = config.aiWebClient(props);
        assertThat(client).isNotNull();
    }
}
