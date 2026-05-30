package com.colorinchi.app.dto.chat;

import java.util.UUID;

public record StreamChunk(
    String type,
    String content,
    UUID messageId
) {
    public static StreamChunk chunk(String content) {
        return new StreamChunk("chunk", content, null);
    }

    public static StreamChunk done(UUID messageId) {
        return new StreamChunk("done", null, messageId);
    }

    public static StreamChunk error(String message) {
        return new StreamChunk("error", message, null);
    }

    public static StreamChunk policyStop(String refusalMessage) {
        return new StreamChunk("policy", refusalMessage, null);
    }
}
