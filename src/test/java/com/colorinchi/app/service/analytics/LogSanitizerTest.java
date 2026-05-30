package com.colorinchi.app.service.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void redactsOwnerIdInLogLine() {
        String input = "Processing request for ownerId=123e4567-e89b-12d3-a456-426614174000";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).isEqualTo("Processing request for ownerId=[REDACTED]");
    }

    @Test
    void redactsOwnerIdWithColon() {
        String input = "owner_id:123e4567-e89b-12d3-a456-426614174000 completed";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).isEqualTo("owner_id:[REDACTED] completed");
    }

    @Test
    void redactsOwnerIdWithEquals() {
        String input = "owner=123e4567-e89b-12d3-a456-426614174000";
        String result = LogSanitizer.sanitize(input);
        // "owner=" matches both OWNER_PATTERN and COOKIE_PATTERN
        assertThat(result).doesNotContain("123e4567");
    }

    @Test
    void redactsContentField() {
        String input = "Saving message content='Hola, qué tal?'";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).contains("content=[REDACTED]");
    }

    @Test
    void redactsContentFieldWithDoubleQuotes() {
        String input = "User said content=\"Hola, qué tal?\"";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).contains("content=[REDACTED]");
    }

    @Test
    void redactsMessageField() {
        String input = "message='this is a private message'";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).contains("message=[REDACTED]");
    }

    @Test
    void redactsPromptField() {
        String input = "AI prompt='You are a fashion assistant...'";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).contains("prompt=[REDACTED]");
    }

    @Test
    void redactsWardrobeContext() {
        String input = "wardrobeContext='{\"tops\":[\"red shirt\"]}'";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).contains("wardrobeContext=[REDACTED]");
    }

    @Test
    void preservesRunIds() {
        String input = "Run 123e4567-e89b-12d3-a456-426614174000 completed";
        String result = LogSanitizer.sanitize(input);
        // UUIDs not preceded by owner/content keyword pass through
        assertThat(result).contains("123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    void preservesEventTypes() {
        String input = "Event type: RUN_COMPLETED for session abc-123";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).isEqualTo(input);
    }

    @Test
    void preservesLatencyAndTokenCounts() {
        String input = "Latency: 1234ms, tokens: 567";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).isEqualTo(input);
    }

    @Test
    void preservesErrorTypes() {
        String input = "Error type: TimeoutException occurred";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).isEqualTo(input);
    }

    @Test
    void handlesNullInput() {
        assertThat(LogSanitizer.sanitize(null)).isNull();
    }

    @Test
    void handlesEmptyInput() {
        assertThat(LogSanitizer.sanitize("")).isEmpty();
    }
}
