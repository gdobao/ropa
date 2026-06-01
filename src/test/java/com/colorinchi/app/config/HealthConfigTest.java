package com.colorinchi.app.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.reactive.function.client.WebClient;

import com.github.tomakehurst.wiremock.WireMockServer;

class HealthConfigTest {

    private WireMockServer wireMockServer;

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void aiProviderHealthIsUpForAnyHttpResponseStatus() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        wireMockServer.stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(401)));

        Health health = healthIndicator(true, wireMockServer.baseUrl()).health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("status", 401);
        assertThat(health.getDetails()).containsEntry("enabled", true);
    }

    @Test
    void aiProviderHealthIsUnknownWhenAiIsDisabled() {
        Health health = healthIndicator(false, "http://localhost:1").health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("reason", "AI provider is disabled via config");
    }

    private HealthIndicator healthIndicator(boolean enabled, String baseUrl) {
        AiServerProperties properties = new AiServerProperties(
                baseUrl,
                "/v1/chat/completions",
                "qwen3.6",
                "test-api-key",
                500,
                enabled,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                List.of());
        return new HealthConfig(properties, WebClient.builder()).aiProviderHealthIndicator();
    }
}
