package com.colorinchi.app.service.analytics;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.colorinchi.app.dto.chat.PolicyDecision;
import com.colorinchi.app.model.ChatAnalyticsEvent;
import com.colorinchi.app.repository.ChatAnalyticsEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Fire-and-forget analytics event recorder with batch persistence.
 *
 * <p>Events are buffered in a thread-safe queue and flushed to the database
 * when either:
 * <ul>
 *   <li>The buffer reaches {@value #BATCH_SIZE} events (size trigger), or</li>
 *   <li>{@value #FLUSH_INTERVAL_MS} ms have elapsed since the last flush (time trigger).</li>
 * </ul>
 *
 * <p>All database writes run on the {@code analyticsTaskExecutor} via
 * {@code @Async}. Failures are logged and silently swallowed — analytics
 * must never block the request path.
 */
@Service
public class ChatAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(ChatAnalyticsService.class);
    private static final int BATCH_SIZE = 10;
    private static final long FLUSH_INTERVAL_MS = 30_000L;

    private final ChatAnalyticsEventRepository repository;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<ChatAnalyticsEvent> buffer;

    /** Tracks stream start times per run ID for latency computation. */
    private final ConcurrentHashMap<UUID, OffsetDateTime> streamStartTimes;

    public ChatAnalyticsService(ChatAnalyticsEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.buffer = new ConcurrentLinkedQueue<>();
        this.streamStartTimes = new ConcurrentHashMap<>();
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Record a raw analytics event. Fire-and-forget — inserts into buffer
     * and triggers an async flush if the batch-size threshold is reached.
     */
    public void recordEvent(UUID ownerId, ChatEventType eventType, Map<String, Object> eventData) {
        String jsonData;
        try {
            jsonData = eventData != null ? objectMapper.writeValueAsString(eventData) : "{}";
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise analytics event data for {}; storing empty", eventType, e);
            jsonData = "{}";
        }

        ChatAnalyticsEvent event = new ChatAnalyticsEvent();
        event.setId(UUID.randomUUID());
        event.setOwnerId(ownerId);
        event.setEventType(eventType.name());
        event.setEventData(jsonData);
        event.setCreatedAt(OffsetDateTime.now());

        buffer.add(event);

        if (buffer.size() >= BATCH_SIZE) {
            flushAsync();
        }
    }

    /** Convenience: record stream-run metrics as a RUN_COMPLETED event. */
    public void recordStreamMetrics(UUID ownerId, UUID runId, long latencyMs, int tokensUsed, String modelResolved) {
        recordEvent(ownerId, ChatEventType.RUN_COMPLETED, Map.of(
                "runId", runId.toString(),
                "latencyMs", latencyMs,
                "tokensUsed", tokensUsed,
                "model", modelResolved != null ? modelResolved : "unknown"
        ));
    }

    /** Convenience: record a policy decision event (BLOCK, FLAG, or RATE_LIMIT). */
    public void recordPolicyEvent(UUID ownerId, PolicyDecision decision) {
        ChatEventType type;
        if (decision.decision() == PolicyDecision.Decision.BLOCK) {
            type = decision.reason() != null && decision.reason().startsWith("rate_limit")
                    ? ChatEventType.RATE_LIMIT_HIT
                    : ChatEventType.POLICY_BLOCK;
        } else if (decision.decision() == PolicyDecision.Decision.FLAG) {
            type = ChatEventType.POLICY_FLAG;
        } else {
            return; // ALLOW — no analytics event
        }
        recordEvent(ownerId, type, Map.of("reason", decision.reason() != null ? decision.reason() : ""));
    }

    /** Convenience: record feedback submission. */
    public void recordFeedback(UUID ownerId, UUID runId, String rating) {
        recordEvent(ownerId, ChatEventType.FEEDBACK_SUBMITTED, Map.of(
                "runId", runId.toString(),
                "rating", rating
        ));
    }

    /**
     * Record a stream start time for later latency tracking.
     * Call this when a stream begins, then call {@link #trackLatency(UUID)}
     * when it completes.
     */
    public void recordStreamStart(UUID runId) {
        streamStartTimes.put(runId, OffsetDateTime.now());
    }

    /**
     * Track and record latency for a completed stream. Computes the delta
     * from the previously recorded start time (via {@link #recordStreamStart}),
     * records it as event data, and returns the computed ms.
     *
     * @return latency in milliseconds, or -1 if no start time was recorded
     */
    public long trackLatency(UUID runId) {
        OffsetDateTime start = streamStartTimes.remove(runId);
        if (start == null) {
            log.warn("No start time recorded for run {}", runId);
            return -1L;
        }
        long ms = ChronoUnit.MILLIS.between(start, OffsetDateTime.now());
        log.debug(LogSanitizer.sanitize("Stream latency for run {}: {} ms"), runId, ms);
        return ms;
    }

    // ---------------------------------------------------------------
    // Batch flush
    // ---------------------------------------------------------------

    @Async("analyticsTaskExecutor")
    public void flushAsync() {
        flush();
    }

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    void scheduledFlush() {
        if (!buffer.isEmpty()) {
            log.debug("Scheduled flush draining {} events", buffer.size());
            flush();
        }
    }

    void flush() {
        List<ChatAnalyticsEvent> batch = new ArrayList<>();
        ChatAnalyticsEvent event;
        while ((event = buffer.poll()) != null) {
            batch.add(event);
        }
        if (batch.isEmpty()) {
            return;
        }
        try {
            repository.saveAll(batch);
            log.debug("Flushed {} analytics events", batch.size());
        } catch (Exception e) {
            log.warn("Failed to persist {} analytics events; events dropped", batch.size(), e);
        }
    }

    // ---------------------------------------------------------------
    // Test support
    // ---------------------------------------------------------------

    int bufferSize() {
        return buffer.size();
    }

    void enqueue(ChatAnalyticsEvent event) {
        buffer.add(event);
    }
}
