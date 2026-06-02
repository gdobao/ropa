package com.colorinchi.app.service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.colorinchi.app.config.RateLimitProperties;
import com.colorinchi.app.dto.chat.PolicyDecision;
import com.colorinchi.app.service.ChatIntentClassifier.Intent;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;
import com.colorinchi.app.service.analytics.LogSanitizer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Service
public class ChatPolicyService {

    private static final Logger log = LoggerFactory.getLogger(ChatPolicyService.class);

    private static final List<String> REFUSAL_MESSAGES = List.of(
        "Entiendo que quieras un look armado, pero mi rol es ayudarte a entender mejor tu guardarropa, "
        + "no elegir por ti. ¿Te cuento qué combinaciones de colores funcionan con lo que tienes?",

        "Prefiero darte ideas y sugerencias en vez de elegir un outfit directamente. "
        + "Dime qué tipo de look buscas (formal, casual, para una ocasión) y te comento "
        + "qué opciones de tu armario podrían funcionar.",

        "Elegir un outfit definitivo no es mi función: la decisión final siempre es tuya. "
        + "Lo que sí puedo hacer es contarte cómo combinar ciertas prendas, "
        + "qué colores armonizan mejor o qué opciones tienes para una ocasión específica."
    );

    private final ChatIntentClassifier intentClassifier;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final ChatAnalyticsService chatAnalyticsService;
    private final ChatMetricsService chatMetricsService;
    private final RateLimitProperties rateLimitProperties;
    private final Cache<UUID, WindowCounter> rateCounters;

    public ChatPolicyService(ChatIntentClassifier intentClassifier,
                              CurrentOwnerAccessor currentOwnerAccessor,
                              ChatAnalyticsService chatAnalyticsService,
                              ChatMetricsService chatMetricsService,
                              RateLimitProperties rateLimitProperties) {
        this.intentClassifier = intentClassifier;
        this.currentOwnerAccessor = currentOwnerAccessor;
        this.chatAnalyticsService = chatAnalyticsService;
        this.chatMetricsService = chatMetricsService;
        this.rateLimitProperties = rateLimitProperties;
        this.rateCounters = Caffeine.newBuilder()
                .expireAfterWrite(rateLimitProperties.chatPerOwner().refillMinutes(), TimeUnit.MINUTES)
                .build();
    }

    public PolicyDecision evaluate(String message) {
        // Intent check
        Intent intent = intentClassifier.classify(message);
        if (intent == Intent.OUTFIT_REQUEST) {
            String refusal = REFUSAL_MESSAGES.get((int) (System.nanoTime() % REFUSAL_MESSAGES.size()));
            log.info(LogSanitizer.sanitize("Policy BLOCK for outfit_request"));
            PolicyDecision decision = PolicyDecision.block(
                "outfit_request: el mensaje solicita elegir un outfit definitivo",
                refusal);
            recordPolicy(decision);
            return decision;
        }

        // Rate limit check
        UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();
        PolicyDecision rateDecision = checkRateLimit(ownerId);
        if (!rateDecision.isAllowed()) {
            recordPolicy(rateDecision);
            return rateDecision;
        }

        // Flag style requests for extra caution
        if (intent == Intent.STYLING_ADVICE || intent == Intent.COLOR_ADVICE) {
            PolicyDecision decision = PolicyDecision.flag("intent: " + intent.name().toLowerCase());
            recordPolicy(decision);
            return decision;
        }

        PolicyDecision decision = PolicyDecision.allow("intent: " + intent.name().toLowerCase());
        recordPolicy(decision);
        return decision;
    }

    private void recordPolicy(PolicyDecision decision) {
        try {
            UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();
            chatAnalyticsService.recordPolicyEvent(ownerId, decision);
            switch (decision.decision()) {
                case BLOCK -> chatMetricsService.increment(ChatMetricsService.POLICY_BLOCKS);
                case FLAG -> chatMetricsService.increment(ChatMetricsService.POLICY_FLAGS);
                case ALLOW -> chatMetricsService.increment(ChatMetricsService.POLICY_ALLOWS);
            }
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record policy analytics"), e);
        }
    }

    public PolicyDecision checkRateLimit(UUID ownerId) {
        long now = System.nanoTime();
        RateLimitProperties.EndpointConfig config = rateLimitProperties.chatPerOwner();
        WindowCounter counter = rateCounters.get(ownerId, k -> new WindowCounter(now));
        counter.evictExpired(now, TimeUnit.MINUTES.toNanos(config.refillMinutes()));

        int current = counter.increment();
        if (current > config.capacity()) {
            log.warn("Rate limit exceeded for owner {}: {} messages in window", ownerId, current);
            return PolicyDecision.block(
                "rate_limit: demasiados mensajes en la ventana de " + config.refillMinutes() + " minuto(s)",
                "Estás enviando muchos mensajes. Espera un momento antes de continuar.");
        }

        return PolicyDecision.allow("rate_ok");
    }

    private static class WindowCounter {
        private final AtomicInteger count;
        private volatile long windowStartNanos;

        WindowCounter(long now) {
            this.count = new AtomicInteger(0);
            this.windowStartNanos = now;
        }

        int increment() {
            return count.incrementAndGet();
        }

        void evictExpired(long now, long windowNanos) {
            if (now - windowStartNanos > windowNanos) {
                windowStartNanos = now;
                count.set(0);
            }
        }
    }
}
