package com.colorinchi.app.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatSurface;

@Service
public class ChatStreamPersistenceService {

    private final ChatMessageService chatMessageService;
    private final ChatRunService chatRunService;

    public ChatStreamPersistenceService(ChatMessageService chatMessageService, ChatRunService chatRunService) {
        this.chatMessageService = chatMessageService;
        this.chatRunService = chatRunService;
    }

    @Transactional
    public ChatMessage persistAssistantMessageAndCompleteRun(UUID sessionId, UUID ownerId, UUID runId,
            String content, String modelResolved, int tokens, ChatSurface surface) {
        ChatMessage msg = chatMessageService.create(sessionId, ownerId, "assistant", content, tokens, surface);
        chatRunService.complete(runId, ownerId, modelResolved, tokens, surface);
        return msg;
    }

    @Transactional
    public void failRun(UUID runId, UUID ownerId, String errorMessage, ChatSurface surface) {
        chatRunService.fail(runId, ownerId, errorMessage, surface);
    }
}
