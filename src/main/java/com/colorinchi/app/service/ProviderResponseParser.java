package com.colorinchi.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

/**
 * Parses streaming SSE response chunks from the AI provider
 * into text content deltas.
 *
 * <p>Handles both raw SSE lines (with {@code data: } prefix, as produced
 * by manual line splitting) and pre-decoded SSE payloads (as produced by
 * Spring WebFlux's {@code ServerSentEventHttpMessageReader} which
 * automatically strips the prefix from {@code text/event-stream} responses).
 *
 * <p>Expected SSE format (OpenAI-compatible):
 * <pre>
 * data: {"choices":[{"delta":{"content":"Hello"},"index":0}]}
 * data: {"choices":[{"delta":{"content":" world"},"index":0}]}
 * data: [DONE]
 * </pre>
 */
@Component
public class ProviderResponseParser {

    private static final Logger log = LoggerFactory.getLogger(ProviderResponseParser.class);
    private static final String DATA_PREFIX = "data: ";
    private static final String DONE_MARKER = "[DONE]";

    private final ObjectMapper objectMapper;

    public ProviderResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse individual SSE lines / payloads into a flux of text content deltas.
     * <p>
     * Each input entry is expected to be either:
     * <ul>
     *   <li>A complete SSE line such as {@code data: {"choices":...}} (raw)</li>
     *   <li>A pre-decoded payload such as {@code {"choices":...}} (from Spring's SSE reader)</li>
     *   <li>The {@code [DONE]} marker</li>
     * </ul>
     * Non-data lines and the {@code [DONE]} marker are silently filtered out.
     *
     * @param lines raw lines or pre-decoded SSE payloads
     * @return a flux of non-empty text content deltas
     */
    public Flux<String> parseLines(Flux<String> lines) {
        return lines
                .filter(line -> line != null && !line.isBlank())
                .filter(this::isDataLineOrPayload)
                .map(this::stripDataPrefix)
                .filter(data -> !DONE_MARKER.equals(data))
                .map(this::extractContent)
                .filter(content -> !content.isEmpty());
    }

    /**
     * Check if a non-blank line is a data line or valid JSON payload.
     * Accepts lines starting with {@code data: } as well as lines that
     * look like JSON objects (pre-decoded SSE).
     */
    private boolean isDataLineOrPayload(String line) {
        return line.startsWith(DATA_PREFIX) || line.startsWith("{") || DONE_MARKER.equals(line.trim());
    }

    /**
     * Strip the {@code data: } prefix if present; return the line as-is otherwise.
     */
    private String stripDataPrefix(String line) {
        if (line.startsWith(DATA_PREFIX)) {
            return line.substring(DATA_PREFIX.length()).trim();
        }
        return line.trim();
    }

    /**
     * Extract the text content delta from a single SSE data JSON payload.
     *
     * @param data the JSON payload (with or without {@code data: } prefix)
     * @return the text delta, or empty string if none is present
     */
    String extractContent(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode delta = root.path("choices").path(0).path("delta");
            String content = delta.path("content").asText(null);
            return content != null ? content : "";
        } catch (Exception e) {
            log.warn("Failed to parse SSE chunk: {}", e.getMessage());
            return "";
        }
    }
}
