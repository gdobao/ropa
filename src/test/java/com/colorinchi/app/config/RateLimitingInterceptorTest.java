package com.colorinchi.app.config;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.colorinchi.app.service.CurrentOwnerAccessor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitingInterceptorTest {

    private RateLimitProperties properties;
    private RateLimitingInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private CurrentOwnerAccessor currentOwnerAccessor;

    private final UUID testOwnerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        var analyze = new RateLimitProperties.EndpointConfig(2, 1);
        var recommend = new RateLimitProperties.EndpointConfig(3, 5);
        var chat = new RateLimitProperties.EndpointConfig(10, 1);
        var chatPerOwner = new RateLimitProperties.EndpointConfig(5, 1);
        properties = new RateLimitProperties(analyze, recommend, chat, chatPerOwner);
        currentOwnerAccessor = mock(CurrentOwnerAccessor.class);
        interceptor = new RateLimitingInterceptor(properties, currentOwnerAccessor);
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
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(testOwnerId);

        boolean result = interceptor.preHandle(request, response, null);

        assertThat(result).isTrue();
    }

    @Test
    void throwsWhenChatExceedsCapacity() {
        when(request.getRequestURI()).thenReturn("/api/chat/stream");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.10");
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(testOwnerId);

        // Chat global limit is 10, but per-owner limit is 5, so the per-owner fires first
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }

        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Demasiadas");
    }

    @Test
    void differentOwnersHaveSeparatePerOwnerLimits() {
        when(request.getRequestURI()).thenReturn("/api/chat/stream");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.20");

        // First owner hits the per-owner limit
        UUID ownerA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerA);
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }
        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class);

        // Second owner has a separate counter
        UUID ownerB = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerB);
        assertThat(interceptor.preHandle(request, response, null)).isTrue();
    }

    @Test
    void perOwnerLimitFallsBackToIpWhenOwnerUnavailable() {
        when(request.getRequestURI()).thenReturn("/api/chat/stream");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.30");
        when(currentOwnerAccessor.getCurrentOwnerId()).thenThrow(new IllegalStateException("Owner context unavailable"));

        // Should fall back to IP-based key
        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }
        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void ownerKeyIncludesOwnerId() {
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(testOwnerId);

        String key = interceptor.resolveOwnerKey(request);
        assertThat(key).isEqualTo("00000000-0000-0000-0000-000000000001:chat");
    }

    @Test
    void ownerKeyFallsBackToIpOnException() {
        when(currentOwnerAccessor.getCurrentOwnerId()).thenThrow(new IllegalStateException("Owner context unavailable"));
        when(request.getRemoteAddr()).thenReturn("192.168.1.99");

        String key = interceptor.resolveOwnerKey(request);
        assertThat(key).isEqualTo("192.168.1.99:chat");
    }

    @Test
    void configForKeyReturnsChatPerOwner() {
        var config = interceptor.configForKey("chat-per-owner");
        assertThat(config.capacity()).isEqualTo(5);
        assertThat(config.refillMinutes()).isEqualTo(1);
    }

    @Test
    void allowsChatSessionsPostWithinLimit() {
        when(request.getRequestURI()).thenReturn("/api/chat/sessions/");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.40");
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(testOwnerId);

        boolean result = interceptor.preHandle(request, response, null);
        assertThat(result).isTrue();
    }

    @Test
    void chatSessionsPostRespectsPerOwnerLimit() {
        when(request.getRequestURI()).thenReturn("/api/chat/sessions/");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("10.0.0.50");
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(testOwnerId);

        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }

        assertThatThrownBy(() -> interceptor.preHandle(request, response, null))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
