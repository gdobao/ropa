package com.colorinchi.app.dto.chat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatRunResponse(
    UUID id,
    UUID sessionId,
    String modelRequested,
    String modelResolved,
    String status,
    OffsetDateTime startedAt,
    OffsetDateTime completedAt,
    int totalTokens,
    String errorMessage
) {

    public static ChatRunResponse fromModel(
            UUID id, UUID sessionId,
            String modelRequested, String modelResolved,
            String status,
            OffsetDateTime startedAt, OffsetDateTime completedAt,
            int totalTokens, String errorMessage) {
        return new ChatRunResponse(id, sessionId, modelRequested, modelResolved,
                status, startedAt, completedAt, totalTokens, errorMessage);
    }
}
