package com.colorinchi.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    EndpointConfig analyze,
    EndpointConfig recommendation,
    EndpointConfig chat
) {
    public record EndpointConfig(int capacity, int refillMinutes) {}
}
