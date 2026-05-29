package com.colorinchi.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app.wardrobe")
public record WardrobeProperties(
    List<String> categories,
    List<String> days,
    int colorLimit
) {}
