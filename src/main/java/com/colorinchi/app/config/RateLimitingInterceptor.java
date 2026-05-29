package com.colorinchi.app.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitProperties properties;
    private final Cache<String, AtomicInteger> cache;

    public RateLimitingInterceptor(RateLimitProperties properties) {
        this.properties = properties;

        long maxRefillMinutes = Math.max(
                properties.analyze().refillMinutes(),
                properties.recommendation().refillMinutes()
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

        RateLimitProperties.EndpointConfig config = "analyze".equals(endpointKey)
                ? properties.analyze()
                : properties.recommendation();

        String clientIp = request.getRemoteAddr();

        AtomicInteger counter = cache.get(clientIp, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > config.capacity()) {
            throw new RateLimitExceededException(
                    "Demasiadas solicitudes. Esperá " + config.refillMinutes()
                            + " minutos antes de intentar de nuevo."
            );
        }

        return true;
    }

    private String resolveEndpointKey(String path, String method) {
        if ("/wardrobe/analyze".equals(path) && "POST".equalsIgnoreCase(method)) {
            return "analyze";
        }
        if ("/recommendation".equals(path) && "GET".equalsIgnoreCase(method)) {
            return "recommendation";
        }
        return null;
    }
}
