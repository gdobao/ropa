package com.colorinchi.app.config;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.servlet.HandlerInterceptor;

import com.colorinchi.app.service.CurrentOwnerAccessor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private static final PathPattern CHAT_STREAM_PATTERN = PathPatternParser.defaultInstance.parse("/api/chat/stream/{runId}");
    private static final PathPattern COMPANION_STREAM_PATTERN = PathPatternParser.defaultInstance.parse("/api/companion/stream/{runId}");
    private static final PathPattern CHAT_CREATE_SESSION_PATTERN = PathPatternParser.defaultInstance.parse("/api/chat/sessions");
    private static final PathPattern COMPANION_CREATE_SESSION_PATTERN = PathPatternParser.defaultInstance.parse("/api/companion/sessions");
    private static final PathPattern CHAT_UPDATE_TITLE_PATTERN = PathPatternParser.defaultInstance.parse("/api/chat/sessions/{sessionId}/title");
    private static final PathPattern CHAT_DELETE_SESSION_PATTERN = PathPatternParser.defaultInstance.parse("/api/chat/sessions/{sessionId}");
    private static final PathPattern COMPANION_UPDATE_SESSION_PATTERN = PathPatternParser.defaultInstance.parse("/api/companion/sessions/{sessionId}");
    private static final PathPattern CHAT_SEND_MESSAGE_PATTERN = PathPatternParser.defaultInstance.parse("/api/chat/sessions/{sessionId}/messages");
    private static final PathPattern COMPANION_SEND_MESSAGE_PATTERN = PathPatternParser.defaultInstance.parse("/api/companion/sessions/{sessionId}/messages");
    private static final PathPattern CHAT_MESSAGE_FEEDBACK_PATTERN = PathPatternParser.defaultInstance.parse("/api/chat/messages/{messageId}/feedback");
    private static final PathPattern COMPANION_MESSAGE_FEEDBACK_PATTERN = PathPatternParser.defaultInstance.parse("/api/companion/messages/{messageId}/feedback");
    private static final PathPattern COMPANION_SESSION_MESSAGE_FEEDBACK_PATTERN = PathPatternParser.defaultInstance.parse("/api/companion/sessions/{sessionId}/messages/{messageId}/feedback");

    private final RateLimitProperties properties;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final Cache<String, AtomicInteger> cache;

    public RateLimitingInterceptor(RateLimitProperties properties, CurrentOwnerAccessor currentOwnerAccessor) {
        this.properties = properties;
        this.currentOwnerAccessor = currentOwnerAccessor;

        long maxRefillMinutes = Math.max(
                properties.analyze().refillMinutes(),
                Math.max(properties.recommendation().refillMinutes(),
                        Math.max(properties.chat().refillMinutes(),
                                properties.chatPerOwner().refillMinutes()))
        );
        long expireMinutes = Math.max(maxRefillMinutes, 60);

        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String endpointKey = resolveEndpointKey(request.getRequestURI(), request.getMethod());
        if (endpointKey == null) {
            return true;
        }

        if ("chat".equals(endpointKey)) {
            // Check global IP-based limit first
            checkLimit(request, properties.chat(), endpointKey + ":" + request.getRemoteAddr());
            // Then check per-owner limit
            checkPerOwnerLimit(request);
        } else if ("chat-per-owner".equals(endpointKey)) {
            checkPerOwnerLimit(request);
        } else {
            checkLimit(request, configForKey(endpointKey), endpointKey + ":" + request.getRemoteAddr());
        }

        return true;
    }

    private void checkLimit(HttpServletRequest request, RateLimitProperties.EndpointConfig config, String cacheKey) {
        AtomicInteger counter = cache.get(cacheKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > config.capacity()) {
            throw new RateLimitExceededException(
                    "Demasiadas solicitudes. Espera " + config.refillMinutes()
                            + " minutos antes de intentarlo de nuevo."
            );
        }
    }

    private void checkPerOwnerLimit(HttpServletRequest request) {
        String ownerKey;
        try {
            UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();
            ownerKey = ownerId + ":chat-per-owner";
        } catch (IllegalStateException e) {
            // Fallback to IP-based key when owner context is unavailable
            ownerKey = "chat-per-owner:" + request.getRemoteAddr();
        }
        checkLimit(request, properties.chatPerOwner(), ownerKey);
    }

    // visible for testing
    String resolveOwnerKey(HttpServletRequest request) {
        try {
            UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();
            return ownerId + ":chat-per-owner";
        } catch (IllegalStateException e) {
            return "chat-per-owner:" + request.getRemoteAddr();
        }
    }

    private String resolveEndpointKey(String path, String method) {
        if ("/wardrobe/analyze".equals(path) && "POST".equalsIgnoreCase(method)) {
            return "analyze";
        }
        if ("/recommendation".equals(path) && "GET".equalsIgnoreCase(method)) {
            return "recommendation";
        }
        PathContainer parsedPath = PathContainer.parsePath(path);
        if ("GET".equalsIgnoreCase(method)
                && (CHAT_STREAM_PATTERN.matches(parsedPath) || COMPANION_STREAM_PATTERN.matches(parsedPath))) {
            return "chat";
        }
        if (!isLimitedWriteMethod(method)) {
            return null;
        }
        if (CHAT_CREATE_SESSION_PATTERN.matches(parsedPath)
                || COMPANION_CREATE_SESSION_PATTERN.matches(parsedPath)
                || CHAT_UPDATE_TITLE_PATTERN.matches(parsedPath)
                || CHAT_DELETE_SESSION_PATTERN.matches(parsedPath)
                || COMPANION_UPDATE_SESSION_PATTERN.matches(parsedPath)
                || CHAT_SEND_MESSAGE_PATTERN.matches(parsedPath)
                || COMPANION_SEND_MESSAGE_PATTERN.matches(parsedPath)
                || CHAT_MESSAGE_FEEDBACK_PATTERN.matches(parsedPath)
                || COMPANION_MESSAGE_FEEDBACK_PATTERN.matches(parsedPath)
                || COMPANION_SESSION_MESSAGE_FEEDBACK_PATTERN.matches(parsedPath)) {
            return "chat-per-owner";
        }
        return null;
    }

    private boolean isLimitedWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    // visible for testing
    RateLimitProperties.EndpointConfig configForKey(String key) {
        return switch (key) {
            case "analyze" -> properties.analyze();
            case "recommendation" -> properties.recommendation();
            case "chat" -> properties.chat();
            case "chat-per-owner" -> properties.chatPerOwner();
            default -> throw new IllegalArgumentException("Unknown key: " + key);
        };
    }
}
