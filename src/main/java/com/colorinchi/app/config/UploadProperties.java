package com.colorinchi.app.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        Path directory,
        DataSize maxSize,
        List<String> allowedContentTypes) {
}
