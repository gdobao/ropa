package com.colorinchi.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app.wardrobe")
public record WardrobeProperties(
    List<String> categories,
    List<String> days,
    int colorLimit,
    int maxGarmentsInSummary,
    int upcomingDaysToInclude
) {
    public WardrobeProperties {
        if (maxGarmentsInSummary <= 0) maxGarmentsInSummary = 30;
        if (upcomingDaysToInclude <= 0) upcomingDaysToInclude = 3;
        if (colorLimit <= 0) colorLimit = 5;
    }
}
