package com.colorinchi.app.service.analytics;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMetricsServiceTest {

    private ChatMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new ChatMetricsService();
    }

    @Test
    void snapshotStartsWithZeroCounters() {
        Map<String, Long> snapshot = metricsService.getSnapshot();
        assertThat(snapshot).containsEntry(ChatMetricsService.SESSIONS_CREATED, 0L);
        assertThat(snapshot).containsEntry(ChatMetricsService.MESSAGES_PROCESSED, 0L);
        assertThat(snapshot).containsEntry(ChatMetricsService.POLICY_ALLOWS, 0L);
        assertThat(snapshot).containsEntry(ChatMetricsService.RUNS_STARTED, 0L);
    }

    @Test
    void incrementIncreasesCounter() {
        metricsService.increment(ChatMetricsService.SESSIONS_CREATED);
        metricsService.increment(ChatMetricsService.SESSIONS_CREATED);
        assertThat(metricsService.getSnapshot())
                .containsEntry(ChatMetricsService.SESSIONS_CREATED, 2L);
    }

    @Test
    void addIncreasesCounterByDelta() {
        metricsService.add(ChatMetricsService.TOKENS_TOTAL, 150);
        metricsService.add(ChatMetricsService.TOKENS_TOTAL, 50);
        assertThat(metricsService.getSnapshot())
                .containsEntry(ChatMetricsService.TOKENS_TOTAL, 200L);
    }

    @Test
    void recordTokensUpdatesTotalAndCount() {
        metricsService.recordTokens(100);
        metricsService.recordTokens(200);
        Map<String, Long> snapshot = metricsService.getSnapshot();
        assertThat(snapshot).containsEntry(ChatMetricsService.TOKENS_TOTAL, 300L);
        assertThat(snapshot).containsEntry(ChatMetricsService.TOKENS_COUNT, 2L);
    }

    @Test
    void recordLatencyUpdatesTotalAndCount() {
        metricsService.recordLatency(500);
        metricsService.recordLatency(1500);
        Map<String, Long> snapshot = metricsService.getSnapshot();
        assertThat(snapshot).containsEntry(ChatMetricsService.LATENCY_TOTAL_MS, 2000L);
        assertThat(snapshot).containsEntry(ChatMetricsService.LATENCY_COUNT, 2L);
    }

    @Test
    void snapshotIncludesComputedAverages() {
        metricsService.recordTokens(300);
        metricsService.recordTokens(500);
        metricsService.recordLatency(1000);
        metricsService.recordLatency(3000);

        Map<String, Long> snapshot = metricsService.getSnapshot();
        assertThat(snapshot).containsEntry("tokens.avg_per_response", 400L);
        assertThat(snapshot).containsEntry("latency.avg_ms", 2000L);
    }

    @Test
    void snapshotDoesNotIncludeAveragesWhenNoData() {
        Map<String, Long> snapshot = metricsService.getSnapshot();
        assertThat(snapshot).doesNotContainKey("tokens.avg_per_response");
        assertThat(snapshot).doesNotContainKey("latency.avg_ms");
    }

    @Test
    void separateCountersAreIndependent() {
        metricsService.increment(ChatMetricsService.SESSIONS_CREATED);
        metricsService.increment(ChatMetricsService.RUNS_FAILED);
        metricsService.increment(ChatMetricsService.STREAMS_DISCONNECTED);

        Map<String, Long> snapshot = metricsService.getSnapshot();
        assertThat(snapshot).containsEntry(ChatMetricsService.SESSIONS_CREATED, 1L);
        assertThat(snapshot).containsEntry(ChatMetricsService.RUNS_FAILED, 1L);
        assertThat(snapshot).containsEntry(ChatMetricsService.STREAMS_DISCONNECTED, 1L);
    }
}
