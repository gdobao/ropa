package com.colorinchi.app.config;

import com.colorinchi.app.service.CurrentOwnerAccessor;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import com.colorinchi.app.service.CurrentOwnerAccessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebMvcConfigTest {

    @Test
    void constructorStoresProperties() {
        var uploadProps = new UploadProperties(Path.of("/tmp"), DataSize.ofMegabytes(8), List.of("image/jpeg"), 6000, 6000, 24_000_000L);
        var rateLimitProps = new RateLimitProperties(
                new RateLimitProperties.EndpointConfig(5, 1),
                new RateLimitProperties.EndpointConfig(10, 5),
                new RateLimitProperties.EndpointConfig(30, 1),
                new RateLimitProperties.EndpointConfig(10, 5)
        );
        var interceptor = new RateLimitingInterceptor(rateLimitProps, mock(CurrentOwnerAccessor.class));
        var config = new WebMvcConfig(uploadProps, interceptor);

        assertThat(config).isNotNull();
    }
}
