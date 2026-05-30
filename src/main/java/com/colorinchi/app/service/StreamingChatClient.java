package com.colorinchi.app.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.colorinchi.app.config.AiServerProperties;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatEventType;
import com.colorinchi.app.service.analytics.ChatMetricsService;
import com.colorinchi.app.service.analytics.LogSanitizer;

import reactor.core.publisher.Flux;

/**
 * AI streaming gateway that uses WebClient to POST to the AI provider
 * with SSE streaming enabled and returns text content as a {@code Flux<String>}.
 *
 * <p>Integrates with the existing chat persistence layer via
 * {@link ChatRunService} and {@link ChatMessageService}.
 */
@Service
public class StreamingChatClient {

    private static final Logger log = LoggerFactory.getLogger(StreamingChatClient.class);

    private final WebClient aiWebClient;
    private final AiServerProperties properties;
    private final ProviderRequestFactory requestFactory;
    private final ProviderResponseParser responseParser;
    private final ChatAnalyticsService chatAnalyticsService;
    private final ChatMetricsService chatMetricsService;

    public StreamingChatClient(
            WebClient aiWebClient,
            AiServerProperties properties,
            ProviderRequestFactory requestFactory,
            ProviderResponseParser responseParser,
            ChatAnalyticsService chatAnalyticsService,
            ChatMetricsService chatMetricsService) {
        this.aiWebClient = aiWebClient;
        this.properties = properties;
        this.requestFactory = requestFactory;
        this.responseParser = responseParser;
        this.chatAnalyticsService = chatAnalyticsService;
        this.chatMetricsService = chatMetricsService;
    }

    /**
     * Stream a chat completion from the AI provider.
     *
     * @param model    the provider API model name (already resolved)
     * @param messages list of message maps ({@code role}, {@code content})
     * @return a flux of text content deltas as they arrive from the provider
     */
    public Flux<String> stream(String model, List<Map<String, String>> messages) {
        if (!properties.enabled()) {
            log.debug("AI streaming is disabled, returning empty flux");
            return Flux.empty();
        }

        Map<String, Object> requestBody = requestFactory.createChatRequest(
                model, messages, properties.maxTokens());

        log.debug("Streaming chat request to {} with model {}", properties.chatPath(), model);

        Flux<String> rawEvents = aiWebClient.post()
                .uri(properties.chatPath())
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class);

        // Spring's ServerSentEventHttpMessageReader strips the "data: " prefix
        // for text/event-stream responses. The parser handles both pre-decoded
        // JSON payloads and raw SSE lines.
        return responseParser.parseLines(rawEvents);
    }

    /**
     * Stream with analytics instrumentation. Records start/completed/disconnected
     * events and updates operational metrics.
     *
     * @param model    the provider API model name
     * @param messages list of message maps ({@code role}, {@code content})
     * @param runId    the run ID for correlation
     * @param ownerId  the owner for event attribution
     * @return a flux of text content deltas with analytics hooks
     */
    public Flux<String> stream(String model, List<Map<String, String>> messages, UUID runId, UUID ownerId) {
        Flux<String> base = stream(model, messages);

        chatAnalyticsService.recordEvent(ownerId, ChatEventType.STREAM_STARTED,
                Map.of("runId", runId.toString(), "model", model));
        chatAnalyticsService.recordStreamStart(runId);

        return base
                .doOnComplete(() -> {
                    long latencyMs = chatAnalyticsService.trackLatency(runId);
                    chatAnalyticsService.recordEvent(ownerId, ChatEventType.STREAM_COMPLETED,
                            Map.of("runId", runId.toString(), "latencyMs", latencyMs));
                    if (latencyMs >= 0) {
                        chatMetricsService.recordLatency(latencyMs);
                    }
                    chatMetricsService.increment(ChatMetricsService.STREAMS_COMPLETED);
                    log.debug(LogSanitizer.sanitize("Stream completed for run {}"), runId);
                })
                .doOnError(error -> {
                    chatAnalyticsService.recordEvent(ownerId, ChatEventType.STREAM_DISCONNECTED,
                            Map.of("runId", runId.toString(),
                                   "errorType", error.getClass().getSimpleName()));
                    chatMetricsService.increment(ChatMetricsService.STREAMS_DISCONNECTED);
                    log.warn(LogSanitizer.sanitize("Stream error for run {}: {}"),
                            runId, error.getClass().getSimpleName());
                })
                .doOnCancel(() -> {
                    chatAnalyticsService.recordEvent(ownerId, ChatEventType.STREAM_DISCONNECTED,
                            Map.of("runId", runId.toString(), "reason", "cancelled"));
                    chatMetricsService.increment(ChatMetricsService.STREAMS_DISCONNECTED);
                    log.debug(LogSanitizer.sanitize("Stream cancelled for run {}"), runId);
                });
    }
}
