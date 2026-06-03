package com.colorinchi.app.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        Path directory,
        DataSize maxSize,
        List<String> allowedContentTypes,
        int maxWidth,
        int maxHeight,
        long maxPixels) {

    public static final int DEFAULT_MAX_WIDTH = 6000;
    public static final int DEFAULT_MAX_HEIGHT = 6000;
    public static final long DEFAULT_MAX_PIXELS = 24_000_000L;

    public UploadProperties {
        if (maxWidth <= 0) maxWidth = DEFAULT_MAX_WIDTH;
        if (maxHeight <= 0) maxHeight = DEFAULT_MAX_HEIGHT;
        if (maxPixels <= 0) maxPixels = DEFAULT_MAX_PIXELS;
    }
}
