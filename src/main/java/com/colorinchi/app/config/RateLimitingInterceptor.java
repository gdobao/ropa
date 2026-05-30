package com.colorinchi.app.config;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.colorinchi.app.service.CurrentOwnerAccessor;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

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
            checkLimit(request, properties.chat(), request.getRemoteAddr());
            // Then check per-owner limit
            checkPerOwnerLimit(request);
        } else if ("chat-per-owner".equals(endpointKey)) {
            checkPerOwnerLimit(request);
        } else {
            checkLimit(request, configForKey(endpointKey), request.getRemoteAddr());
        }

        return true;
    }

    private void checkLimit(HttpServletRequest request, RateLimitProperties.EndpointConfig config, String cacheKey) {
        AtomicInteger counter = cache.get(cacheKey, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > config.capacity()) {
            throw new RateLimitExceededException(
                    "Demasiadas solicitudes. Esperá " + config.refillMinutes()
                            + " minutos antes de intentar de nuevo."
            );
        }
    }

    private void checkPerOwnerLimit(HttpServletRequest request) {
        String ownerKey;
        try {
            UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();
            ownerKey = ownerId.toString() + ":chat";
        } catch (IllegalStateException e) {
            // Fallback to IP-based key when owner context is unavailable
            ownerKey = request.getRemoteAddr() + ":chat";
        }
        checkLimit(request, properties.chatPerOwner(), ownerKey);
    }

    // visible for testing
    String resolveOwnerKey(HttpServletRequest request) {
        try {
            UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();
            return ownerId.toString() + ":chat";
        } catch (IllegalStateException e) {
            return request.getRemoteAddr() + ":chat";
        }
    }

    private String resolveEndpointKey(String path, String method) {
        if ("/wardrobe/analyze".equals(path) && "POST".equalsIgnoreCase(method)) {
            return "analyze";
        }
        if ("/recommendation".equals(path) && "GET".equalsIgnoreCase(method)) {
            return "recommendation";
        }
        if (path.startsWith("/api/chat/stream/") && "GET".equalsIgnoreCase(method)) {
            return "chat";
        }
        // Per-owner rate limited endpoints
        if (path.startsWith("/api/chat/sessions/") && "POST".equalsIgnoreCase(method)) {
            return "chat-per-owner";
        }
        if (path.startsWith("/api/chat/runs/") && "POST".equalsIgnoreCase(method)) {
            return "chat-per-owner";
        }
        return null;
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
