package com.colorinchi.app.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingInterceptorTest {

    @Test
    void exceptionIsRuntimeException() {
        assertThat(new RateLimitExceededException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void exceptionCarriesMessage() {
        var ex = new RateLimitExceededException("test message");
        assertThat(ex.getMessage()).isEqualTo("test message");
    }
}
