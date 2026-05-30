package com.colorinchi.app.dto.chat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatSessionResponse(
    UUID id,
    String title,
    String model,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static ChatSessionResponse from(UUID id, String title, String model, String status,
                                           OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new ChatSessionResponse(id, title, model, status, createdAt, updatedAt);
    }
}
