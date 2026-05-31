package com.colorinchi.app.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chat.retention")
public record ChatRetentionProperties(
        Path uploadDirectory,
        int analyticsEventsDays,
        int sessionInactiveDays,
        boolean orphanUploadCleanup
) {
    public ChatRetentionProperties {
        if (uploadDirectory == null) uploadDirectory = Path.of("uploads");
        if (analyticsEventsDays <= 0) analyticsEventsDays = 90;
        if (sessionInactiveDays <= 0) sessionInactiveDays = 180;
    }
}
