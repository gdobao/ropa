package com.colorinchi.app.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colorinchi.app.model.ChatSurface;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Flux;

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
    @Mock
    private ChatResponseValidator chatResponseValidator;

    @Test
    void sendMessageWithNullBodyThrowsEmptyChatMessageException() {
        // ChatPolicyService is not mockable via @Mock, but these null/blank
        // tests never reach it — the guard throws before any service is called.
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService, chatResponseValidator);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), null))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void sendMessageWithNullContentThrowsEmptyChatMessageException() {
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService, chatResponseValidator);

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
                streamingChatClient, chatStreamPersistenceService, chatResponseValidator);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), Map.of("content", "   ")))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void sendMessageWithEmptyStringContentThrowsEmptyChatMessageException() {
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService, chatResponseValidator);

        assertThatThrownBy(() ->
                orchestrator.sendMessage(ChatSurface.COMPANION, UUID.randomUUID(), Map.of("content", "")))
                .isInstanceOf(ChatConversationOrchestrator.EmptyChatMessageException.class);
    }

    @Test
    void sendMessageWithMissingContentKeyThrowsEmptyChatMessageException() {
        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                null, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService, chatResponseValidator);

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
                streamingChatClient, chatStreamPersistenceService, chatResponseValidator);

        SseEmitter emitter = orchestrator.streamRun(ChatSurface.COMPANION, runId);

        // The errorEmitter sends a "stream-error" event and completes immediately.
        // Verify the method handles the exception gracefully (returns an emitter
        // instead of propagating the exception).
        assertThat(emitter).isNotNull();
    }

    @Test
    void streamRunMarksRunFailedWhenAssistantPersistenceFails() throws Exception {
        UUID runId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        var run = mock(com.colorinchi.app.model.ChatRun.class);
        when(run.getSessionId()).thenReturn(sessionId);
        when(run.getOwnerId()).thenReturn(ownerId);
        when(run.getModelRequested()).thenReturn("qwen3.6");
        when(run.getStatus()).thenReturn("running");

        when(chatRunService.getById(runId)).thenReturn(run);
        when(chatRunService.markStreaming(runId, ownerId)).thenReturn(true);
        when(chatSessionService.getById(ChatSurface.COMPANION, sessionId))
                .thenReturn(mock(com.colorinchi.app.model.ChatSession.class));
        when(chatMessageService.listBySession(sessionId)).thenReturn(java.util.List.of());
        when(chatPromptFactory.buildSystemPrompt(ChatSurface.COMPANION)).thenReturn("sys");
        when(streamingChatClient.stream(anyString(), anyList(), eq(runId), eq(ownerId)))
                .thenReturn(Flux.just("ok"));
        when(chatResponseValidator.validate("ok"))
                .thenReturn(new com.colorinchi.app.dto.chat.ValidationResult(true, java.util.List.of()));
        when(chatStreamPersistenceService.persistAssistantMessageAndCompleteRun(
                eq(sessionId), eq(ownerId), eq(runId), eq("ok"), eq("qwen3.6"), anyInt(), eq(ChatSurface.COMPANION)))
                .thenThrow(new RuntimeException("db down"));

        var orchestrator = new ChatConversationOrchestrator(
                chatSessionService, chatMessageService, chatRunService,
                chatPolicyService, chatPromptFactory, modelRouter,
                streamingChatClient, chatStreamPersistenceService, chatResponseValidator);

        SseEmitter emitter = orchestrator.streamRun(ChatSurface.COMPANION, runId);
        assertThat(emitter).isNotNull();

        CountDownLatch latch = new CountDownLatch(1);
        emitter.onCompletion(latch::countDown);
        latch.await(1, TimeUnit.SECONDS);

        verify(chatStreamPersistenceService, timeout(1000)).failRun(
                eq(runId), eq(ownerId), anyString(), eq(ChatSurface.COMPANION));
    }
}
