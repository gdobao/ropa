package com.colorinchi.app.service.analytics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Lightweight operational metrics for the chat feature.
 *
 * <p>Uses {@link ConcurrentHashMap} with {@link AtomicLong} counters — no
 * external micrometer dependency. Exposes a snapshot for the
 * {@code /api/admin/metrics} endpoint.
 */
@Service
public class ChatMetricsService {

    private static final Logger log = LoggerFactory.getLogger(ChatMetricsService.class);

    private final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    // --- Well-known metric keys ---

    public static final String SESSIONS_CREATED = "sessions.created";
    public static final String MESSAGES_PROCESSED = "messages.processed";
    public static final String STREAMS_COMPLETED = "streams.completed";
    public static final String STREAMS_FAILED = "streams.failed";
    public static final String STREAMS_DISCONNECTED = "streams.disconnected";
    public static final String POLICY_BLOCKS = "policy.blocks";
    public static final String POLICY_FLAGS = "policy.flags";
    public static final String POLICY_ALLOWS = "policy.allows";
    public static final String TOKENS_TOTAL = "tokens.total";
    public static final String TOKENS_COUNT = "tokens.count"; // number of responses for avg
    public static final String LATENCY_TOTAL_MS = "latency.total_ms";
    public static final String LATENCY_COUNT = "latency.count";
    public static final String ERRORS_GENERIC = "errors.generic";
    public static final String RUNS_STARTED = "runs.started";
    public static final String RUNS_COMPLETED = "runs.completed";
    public static final String RUNS_FAILED = "runs.failed";

    public ChatMetricsService() {
        // Pre-initialize so the snapshot always shows 0 for expected keys
        for (String key : List.of(
                SESSIONS_CREATED, MESSAGES_PROCESSED,
                STREAMS_COMPLETED, STREAMS_FAILED, STREAMS_DISCONNECTED,
                POLICY_BLOCKS, POLICY_FLAGS, POLICY_ALLOWS,
                TOKENS_TOTAL, TOKENS_COUNT,
                LATENCY_TOTAL_MS, LATENCY_COUNT,
                ERRORS_GENERIC,
                RUNS_STARTED, RUNS_COMPLETED, RUNS_FAILED)) {
            counters.put(key, new AtomicLong(0));
        }
    }

    /** Increment a named counter by 1. */
    public void increment(String name) {
        add(name, 1);
    }

    /** Add a delta to a named counter. */
    public void add(String name, long delta) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).addAndGet(delta);
    }

    /** Record a complete response for average-token tracking. */
    public void recordTokens(int tokens) {
        add(TOKENS_TOTAL, tokens);
        increment(TOKENS_COUNT);
    }

    /** Record stream latency in milliseconds. */
    public void recordLatency(long latencyMs) {
        add(LATENCY_TOTAL_MS, latencyMs);
        increment(LATENCY_COUNT);
    }

    /**
     * Return an immutable snapshot of all current counters suitable for
     * JSON serialisation.
     */
    public Map<String, Long> getSnapshot() {
        Map<String, Long> snapshot = new java.util.LinkedHashMap<>();
        counters.forEach((key, value) -> snapshot.put(key, value.get()));
        // Compute derived values
        long tokenCount = snapshot.getOrDefault(TOKENS_COUNT, 0L);
        long latencyCount = snapshot.getOrDefault(LATENCY_COUNT, 0L);
        if (tokenCount > 0) {
            snapshot.put("tokens.avg_per_response", snapshot.getOrDefault(TOKENS_TOTAL, 0L) / tokenCount);
        }
        if (latencyCount > 0) {
            snapshot.put("latency.avg_ms", snapshot.getOrDefault(LATENCY_TOTAL_MS, 0L) / latencyCount);
        }
        return Collections.unmodifiableMap(snapshot);
    }
}
