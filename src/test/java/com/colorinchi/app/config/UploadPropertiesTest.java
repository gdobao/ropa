package com.colorinchi.app.config;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

class UploadPropertiesTest {

    @Test
    void holdsAllValues() {
        var props = new UploadProperties(
                Path.of("/tmp/uploads"),
                DataSize.ofMegabytes(8),
                List.of("image/jpeg", "image/png"),
                0,
                0,
                0
        );
        assertThat(props.directory()).isEqualTo(Path.of("/tmp/uploads"));
        assertThat(props.maxSize()).isEqualTo(DataSize.ofMegabytes(8));
        assertThat(props.allowedContentTypes()).containsExactly("image/jpeg", "image/png");
        assertThat(props.maxWidth()).isEqualTo(UploadProperties.DEFAULT_MAX_WIDTH);
        assertThat(props.maxHeight()).isEqualTo(UploadProperties.DEFAULT_MAX_HEIGHT);
        assertThat(props.maxPixels()).isEqualTo(UploadProperties.DEFAULT_MAX_PIXELS);
    }

    @Test
    void keepsConfiguredDimensionLimits() {
        var props = new UploadProperties(
                Path.of("/tmp/uploads"),
                DataSize.ofMegabytes(8),
                List.of("image/jpeg"),
                4096,
                3000,
                12_000_000L
        );

        assertThat(props.maxWidth()).isEqualTo(4096);
        assertThat(props.maxHeight()).isEqualTo(3000);
        assertThat(props.maxPixels()).isEqualTo(12_000_000L);
    }

    @Test
    void fallsBackToDefaultDimensionLimitsWhenConfiguredWithInvalidValues() {
        var props = new UploadProperties(
                Path.of("/tmp/uploads"),
                DataSize.ofMegabytes(8),
                List.of("image/jpeg"),
                0,
                -1,
                0
        );

        assertThat(props.maxWidth()).isEqualTo(UploadProperties.DEFAULT_MAX_WIDTH);
        assertThat(props.maxHeight()).isEqualTo(UploadProperties.DEFAULT_MAX_HEIGHT);
        assertThat(props.maxPixels()).isEqualTo(UploadProperties.DEFAULT_MAX_PIXELS);
    }
}
