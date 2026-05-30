package com.colorinchi.app.config;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

class WebMvcConfigTest {

    @Test
    void constructorStoresProperties() {
        var uploadProps = new UploadProperties(Path.of("/tmp"), DataSize.ofMegabytes(8), List.of("image/jpeg"));
        var rateLimitProps = new RateLimitProperties(
                new RateLimitProperties.EndpointConfig(5, 1),
                new RateLimitProperties.EndpointConfig(10, 5),
                new RateLimitProperties.EndpointConfig(30, 1)
        );
        var interceptor = new RateLimitingInterceptor(rateLimitProps);
        var config = new WebMvcConfig(uploadProps, interceptor);

        assertThat(config).isNotNull();
    }
}
