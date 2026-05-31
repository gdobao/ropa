package com.colorinchi.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String token) {

    public boolean enabled() {
        return token != null && !token.isBlank();
    }
}
