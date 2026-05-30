package com.colorinchi.app.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiServerPropertiesValidator implements ApplicationRunner {

    private final AiServerProperties properties;

    public AiServerPropertiesValidator(AiServerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("APP_AI_API_KEY is required when app.ai.enabled=true");
        }
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new IllegalStateException("app.ai.base-url is required when app.ai.enabled=true");
        }
        if (!StringUtils.hasText(properties.chatPath())) {
            throw new IllegalStateException("app.ai.chat-path is required when app.ai.enabled=true");
        }
        if (!StringUtils.hasText(properties.model())) {
            throw new IllegalStateException("app.ai.model is required when app.ai.enabled=true");
        }

        // Validate the model catalog when configured
        if (properties.models() != null && !properties.models().isEmpty()) {
            boolean atLeastOneDefault = properties.models().stream()
                    .anyMatch(AiModelConfig::isDefault);
            long defaultCount = properties.models().stream()
                    .filter(AiModelConfig::isDefault)
                    .count();
            if (!atLeastOneDefault) {
                throw new IllegalStateException(
                        "At least one model must be marked as default in app.ai.models");
            }
            if (defaultCount > 1) {
                throw new IllegalStateException(
                        "Only one model can be marked as default in app.ai.models");
            }
        }
    }
}
