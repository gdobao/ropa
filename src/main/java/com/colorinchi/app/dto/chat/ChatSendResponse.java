package com.colorinchi.app.dto.chat;

import java.util.UUID;

public record ChatSendResponse(
    UUID runId,
    UUID sessionId,
    boolean blocked,
    String refusalMessage
) {
    public static ChatSendResponse streaming(UUID runId, UUID sessionId) {
        return new ChatSendResponse(runId, sessionId, false, null);
    }

    public static ChatSendResponse blocked(UUID sessionId, String refusalMessage) {
        return new ChatSendResponse(null, sessionId, true, refusalMessage);
    }
}
