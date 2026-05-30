package com.colorinchi.app.dto.chat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponse(
    UUID id,
    UUID sessionId,
    String role,
    String content,
    OffsetDateTime createdAt
) {}
