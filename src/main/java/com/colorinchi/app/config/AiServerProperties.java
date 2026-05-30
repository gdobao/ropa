package com.colorinchi.app.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiServerProperties(
        String baseUrl,
        String chatPath,
        String model,
        String apiKey,
        int maxTokens,
        boolean enabled,
        Duration connectTimeout,
        Duration readTimeout,
        List<AiModelConfig> models) {
}
