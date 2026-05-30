package com.colorinchi.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitingInterceptorTest {

    private RateLimitProperties properties;
    private RateLimitingInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        var analyze = new RateLimitProperties.EndpointConfig(2, 1);
        var recommend = new RateLimitProperties.EndpointConfig(3, 5);
        var chat = new RateLimitProperties.EndpointConfig(10, 1);
        properties = new RateLimitProperties(analyze, recommend, chat);
        interceptor = new RateLimitingInterceptor(properties);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void allowsUnknownEndpoint() {
        when(request.getRequestURI()).thenReturn("/some/other");
        when(request.getMethod()).thenReturn("GET");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    void allowsAnalyzeWithinCapacity() {
        when(request.getRequestURI()).thenReturn("/wardrobe/analyze");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    void allowsRecommendationWithinCapacity() {
        when(request.getRequestURI()).thenReturn("/recommendation");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    void throwsWhenAnalyzeExceedsCapacity() {
        when(request.getRequestURI()).thenReturn("/wardrobe/analyze");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        interceptor.preHandle(request, response, null);
        interceptor.preHandle(request, response, null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Demasiadas");
    }

    @Test
    void throwsWhenRecommendationExceedsCapacity() {
        when(request.getRequestURI()).thenReturn("/recommendation");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        interceptor.preHandle(request, response, null);
        interceptor.preHandle(request, response, null);
        interceptor.preHandle(request, response, null);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Demasiadas");
    }

    @Test
    void differentIpsHaveSeparateCounters() {
        when(request.getRequestURI()).thenReturn("/wardrobe/analyze");
        when(request.getMethod()).thenReturn("POST");

        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        interceptor.preHandle(request, response, null);
        interceptor.preHandle(request, response, null);
        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class);

        when(request.getRemoteAddr()).thenReturn("10.0.0.4");
        assertThat(interceptor.preHandle(request, response, null)).isTrue();
    }

    @Test
    void exceptionIsRuntimeException() {
        assertThat(new RateLimitExceededException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void exceptionCarriesMessage() {
        var ex = new RateLimitExceededException("test message");
        assertThat(ex.getMessage()).isEqualTo("test message");
    }

    @Test
    void allowsChatWithinCapacity() {
        when(request.getRequestURI()).thenReturn("/api/chat/stream");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    void throwsWhenChatExceedsCapacity() {
        when(request.getRequestURI()).thenReturn("/api/chat/stream");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.10");

        for (int i = 0; i < 10; i++) {
            interceptor.preHandle(request, response, null);
        }

        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Demasiadas");
    }
}
