package com.colorinchi.app.service;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colorinchi.app.model.ChatSurface;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatConversationOrchestratorTest {

    @Mock
    private ChatSessionService chatSessionService;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private ChatRunService chatRunService;
    @Mock
    private ChatPromptFactory chatPromptFactory;
    @Mock
    private ModelRouter modelRouter;
    @Mock
    private StreamingChatClient streamingChatClient;
    @Mock
    private ChatPolicyService chatPolicyService;
    @Mock
    private ChatStreamPersistenceService chatStreamPersistenceService;

    @Test
    void sendMessageWithNullBodyThrowsEmptyChatMessageException() {
        // ChatPolicyService is not mockable via @Mock, but these null/blank
        // tests never reach it — the guard throws before any service is called.
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), null))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void sendMessageWithNullContentThrowsEmptyChatMessageException() {
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService);

        Map<String, String> body = new java.util.HashMap<>();
        body.put("content", null);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), body))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void sendMessageWithBlankContentThrowsEmptyChatMessageException() {
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), Map.of("content", "   ")))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void sendMessageWithEmptyStringContentThrowsEmptyChatMessageException() {
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), Map.of("content", "")))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void sendMessageWithMissingContentKeyThrowsEmptyChatMessageException() {
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), Map.of()))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void streamRunReturnsErrorEmitterOnInitializationFailure() {
        UUID runId = UUID.randomUUID();
        when(chatRunService.getById(runId)).thenThrow(new IllegalArgumentException("Run not found"));

        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                chatPolicyService, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService);

        SseEmitter emitter = orchestrator.streamRun(ChatSurface.COMPANION, runId);

        // The errorEmitter sends a "stream-error" event and completes immediately.
        // Verify the method handles the exception gracefully (returns an emitter
        // instead of propagating the exception).
        assertThat(emitter).isNotNull();
    }
}
