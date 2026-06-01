package com.colorinchi.app.config;

import java.net.URI;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Exposes readiness and liveness health groups via Spring Boot Actuator.
 *
 * <ul>
 *   <li>{@code /actuator/health/liveness} — Liveness state (auto-configured by probes)</li>
 *   <li>{@code /actuator/health/readiness} — Readiness state (auto-configured by probes)</li>
 *   <li>{@code /actuator/health} — Aggregate with DB and AI provider indicators</li>
 * </ul>
 *
 * <p>DataSource health is auto-configured by {@link org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator}.
 * This config adds a custom indicator for the AI provider reachability.
 */
@Configuration(proxyBeanMethods = false)
public class HealthConfig {

    private static final Logger log = LoggerFactory.getLogger(HealthConfig.class);

    private final AiServerProperties aiProperties;
    private final WebClient.Builder webClientBuilder;

    public HealthConfig(AiServerProperties aiProperties, WebClient.Builder webClientBuilder) {
        this.aiProperties = aiProperties;
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Custom health indicator that pings the AI provider base URL to verify
     * reachability. Reports {@code UP} if the provider responds (any status),
     * {@code DOWN} if the connection fails, and {@code UNKNOWN} if AI is disabled.
     */
    @Bean
    public HealthIndicator aiProviderHealthIndicator() {
        return () -> {
            if (!aiProperties.enabled()) {
                return Health.unknown().withDetail("reason", "AI provider is disabled via config").build();
            }

            try {
                WebClient client = webClientBuilder
                        .baseUrl(aiProperties.baseUrl())
                        .build();

                String baseUrl = aiProperties.baseUrl();
                log.debug("Checking AI provider health at {}", baseUrl);

                HttpStatusCode status = client
                        .get()
                        .uri(URI.create(baseUrl))
                        .exchangeToMono(response -> response.releaseBody()
                                .thenReturn(response.statusCode()))
                        .block(Duration.ofSeconds(5));

                log.debug("AI provider at {} is reachable with status {}", baseUrl, status);
                return Health.up()
                        .withDetail("baseUrl", baseUrl)
                        .withDetail("enabled", true)
                        .withDetail("status", status != null ? status.value() : "unknown")
                        .build();
            } catch (Exception e) {
                log.warn("AI provider health check failed: {}", e.getMessage());
                return Health.down(e)
                        .withDetail("baseUrl", aiProperties.baseUrl())
                        .withDetail("enabled", aiProperties.enabled())
                        .build();
            }
        };
    }
}
