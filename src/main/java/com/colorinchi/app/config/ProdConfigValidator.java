package com.colorinchi.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("prod")
public class ProdConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(ProdConfigValidator.class);

    @Bean
    ApplicationRunner prodConfigCheck(WardrobeProperties wardrobeProperties, AdminProperties adminProperties) {
        return args -> {
            log.info("Running prod configuration safety checks");

            if (adminProperties.token() == null || adminProperties.token().isBlank()) {
                log.warn("ADMIN_TOKEN is not configured. Admin endpoints (/api/admin/**, /admin/**) are closed.");
            }
        };
    }
}
