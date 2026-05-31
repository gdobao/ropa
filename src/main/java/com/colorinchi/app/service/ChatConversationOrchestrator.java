package com.colorinchi.app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.dto.chat.ChatSendResponse;
import com.colorinchi.app.dto.chat.PolicyDecision;
import com.colorinchi.app.dto.chat.StreamChunk;
import com.colorinchi.app.dto.chat.ValidationResult;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatRun;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.model.ChatSurface;

import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatConversationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ChatConversationOrchestrator.class);
    private static final long SSE_TIMEOUT = 300_000L;

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatRunService chatRunService;
    private final ChatPolicyService chatPolicyService;
    private final ChatPromptFactory chatPromptFactory;
    private final ModelRouter modelRouter;
    private final StreamingChatClient streamingChatClient;
    private final ChatStreamPersistenceService chatStreamPersistenceService;
    private final ChatResponseValidator chatResponseValidator;

    public ChatConversationOrchestrator(
            ChatSessionService chatSessionService,
            ChatMessageService chatMessageService,
            ChatRunService chatRunService,
            ChatPolicyService chatPolicyService,
            ChatPromptFactory chatPromptFactory,
            ModelRouter modelRouter,
            StreamingChatClient streamingChatClient,
            ChatStreamPersistenceService chatStreamPersistenceService,
            ChatResponseValidator chatResponseValidator) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.chatRunService = chatRunService;
        this.chatPolicyService = chatPolicyService;
        this.chatPromptFactory = chatPromptFactory;
        this.modelRouter = modelRouter;
        this.streamingChatClient = streamingChatClient;
        this.chatStreamPersistenceService = chatStreamPersistenceService;
        this.chatResponseValidator = chatResponseValidator;
    }

    @Transactional
    public ChatSendResponse sendMessage(ChatSurface surface, UUID sessionId, Map<String, String> body) {
        if (body == null) throw new EmptyChatMessageException();
        String raw = body.get("content");
        String content = raw != null ? raw.trim() : "";
        if (content.isBlank()) {
            throw new EmptyChatMessageException();
        }
        ChatSession session = chatSessionService.getById(surface, sessionId);

        String requestedModel;
        if (surface == ChatSurface.COMPANION) {
            requestedModel = "gemma4";
        } else {
            requestedModel = body.getOrDefault("model", session.getModel());
        }
        AiModelConfig modelConfig = modelRouter.resolve(requestedModel);

        PolicyDecision decision = chatPolicyService.evaluate(content);
        if (!decision.isAllowed()) {
            if (decision.decision() == PolicyDecision.Decision.BLOCK) {
                log.info("Policy BLOCK for {} session {}: {}", surface, sessionId, decision.reason());
                return ChatSendResponse.blocked(sessionId, decision.refusalMessage());
            }
            log.info("Policy FLAG for {} session {}: {}", surface, sessionId, decision.reason());
        }

        chatMessageService.create(sessionId, "user", content, 0, surface);
        ChatRun run = chatRunService.create(sessionId, modelConfig.getId());
        chatRunService.started(run.getId());

        log.debug("Created {} run {} for session {} with model {}", surface, run.getId(), sessionId, modelConfig.getId());
        return ChatSendResponse.streaming(run.getId(), sessionId);
    }

    public SseEmitter streamRun(ChatSurface surface, UUID runId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        StringBuilder accumulated = new StringBuilder();

        ChatRun run;
        List<ChatMessage> previousMessages;

        try {
            run = chatRunService.getById(runId);
            chatSessionService.getById(surface, run.getSessionId());
            previousMessages = chatMessageService.listBySession(run.getSessionId());
        } catch (Exception e) {
            log.error("Failed to initialize {} stream for run {}", surface, runId, e);
            return errorEmitter("No se pudo iniciar el stream");
        }

        if (!"running".equals(run.getStatus())) {
            log.warn("Re-stream attempt for {} run {} which is in state {}", surface, runId, run.getStatus());
            return errorEmitter("La respuesta ya no está disponible");
        }

        if (!chatRunService.markStreaming(runId, run.getOwnerId())) {
            log.warn("Duplicate stream attempt for {} run {}", surface, runId);
            return errorEmitter("Ya hay un stream activo para esta consulta");
        }

        List<Map<String, String>> aiMessages = buildAiMessages(surface, previousMessages);

        UUID capturedOwnerId = run.getOwnerId();
        UUID sessionId = run.getSessionId();
        AtomicBoolean resolved = new AtomicBoolean(false);

        Disposable subscription = streamingChatClient
                .stream(run.getModelRequested(), aiMessages, runId, capturedOwnerId)
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                        chunk -> {
                            try {
                                accumulated.append(chunk);
                                emitter.send(SseEmitter.event()
                                        .name("chunk")
                                        .data(StreamChunk.chunk(chunk)));
                            } catch (Exception e) {
                                log.warn("Error sending {} SSE chunk for run {}: {}", surface, runId, e.getMessage());
                            }
                        },
                        error -> {
                            if (!resolved.compareAndSet(false, true)) return;
                            log.error("{} stream error for run {}: {}", surface, runId, error.getMessage());
                            try {
                                chatStreamPersistenceService.failRun(runId, capturedOwnerId, error.getMessage(), surface);
                                emitter.send(SseEmitter.event()
                                        .name("stream-error")
                                        .data(StreamChunk.error("Error al procesar la respuesta")));
                            } catch (Exception e) {
                                log.warn("Error sending {} SSE error for run {}", surface, runId);
                            }
                            emitter.complete();
                        },
                        () -> {
                            if (!resolved.compareAndSet(false, true)) return;
                            try {
                                String fullContent = accumulated.toString();
                                ValidationResult validation = chatResponseValidator.validate(fullContent);
                                if (!validation.isValid()) {
                                    chatStreamPersistenceService.failRun(
                                            runId, capturedOwnerId,
                                            "Invalid AI response: " + String.join("; ", validation.warnings()),
                                            surface);
                                    emitter.send(SseEmitter.event()
                                            .name("stream-error")
                                            .data(StreamChunk.error("La respuesta no cumplió las reglas del asistente.")));
                                    emitter.complete();
                                    return;
                                }
                                int tokens = fullContent.length() / 4;
                                ChatMessage assistantMsg = chatStreamPersistenceService
                                        .persistAssistantMessageAndCompleteRun(
                                                sessionId, capturedOwnerId, runId, fullContent,
                                                run.getModelRequested(), tokens, surface);

                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(StreamChunk.done(assistantMsg.getId())));
                            } catch (Exception e) {
                                log.warn("Error completing {} stream for run {}: {}", surface, runId, e.getMessage());
                                try {
                                    chatStreamPersistenceService.failRun(
                                            runId, capturedOwnerId,
                                            "Assistant persistence failed: " + e.getMessage(),
                                            surface);
                                    emitter.send(SseEmitter.event()
                                            .name("stream-error")
                                            .data(StreamChunk.error("Error al guardar la respuesta del asistente")));
                                } catch (Exception failException) {
                                    log.warn("Error marking {} run {} as failed after persistence failure", surface, runId);
                                }
                            }
                            emitter.complete();
                        });

        emitter.onCompletion(() -> {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
        });
        emitter.onTimeout(() -> {
            if (!resolved.compareAndSet(false, true)) return;
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
            try {
                emitter.send(SseEmitter.event()
                        .name("stream-error")
                        .data(StreamChunk.error("El ayudante tardó demasiado en responder")));
            } catch (Exception ignored) {
            }
            try {
                chatStreamPersistenceService.failRun(runId, capturedOwnerId, "Stream timeout", surface);
            } catch (Exception e) {
                log.warn("Error marking {} run {} as failed on timeout", surface, runId);
            }
            emitter.complete();
        });

        return emitter;
    }

    private List<Map<String, String>> buildAiMessages(ChatSurface surface, List<ChatMessage> previousMessages) {
        List<Map<String, String>> aiMessages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", chatPromptFactory.buildSystemPrompt(surface));
        aiMessages.add(systemMsg);

        for (ChatMessage msg : previousMessages) {
            Map<String, String> mapped = new HashMap<>();
            mapped.put("role", msg.getRole().equals("user") ? "user" : "assistant");
            mapped.put("content", msg.getContent());
            aiMessages.add(mapped);
        }

        return aiMessages;
    }

    private SseEmitter errorEmitter(String message) {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event()
                    .name("stream-error")
                    .data(StreamChunk.error(message)));
        } catch (Exception ignored) {
            // ignore
        }
        emitter.complete();
        return emitter;
    }

    public static class EmptyChatMessageException extends RuntimeException {
        public EmptyChatMessageException() {
            super("El mensaje no puede estar vacío");
        }
    }
}
