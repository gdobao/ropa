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
                List.of("image/jpeg", "image/png")
        );
        assertThat(props.directory()).isEqualTo(Path.of("/tmp/uploads"));
        assertThat(props.maxSize()).isEqualTo(DataSize.ofMegabytes(8));
        assertThat(props.allowedContentTypes()).containsExactly("image/jpeg", "image/png");
    }
}
