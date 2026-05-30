package com.colorinchi.app.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.dto.chat.ChatFeedbackRequest;
import com.colorinchi.app.dto.chat.ChatMessageResponse;
import com.colorinchi.app.dto.chat.ChatRunResponse;
import com.colorinchi.app.dto.chat.ChatSendResponse;
import com.colorinchi.app.dto.chat.ChatSessionResponse;
import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.dto.chat.ErrorResponse;
import com.colorinchi.app.dto.chat.PolicyDecision;
import com.colorinchi.app.dto.chat.StreamChunk;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatRun;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.service.ChatFeedbackService;
import com.colorinchi.app.service.ChatMessageService;
import com.colorinchi.app.service.ChatPolicyService;
import com.colorinchi.app.service.ChatPromptFactory;
import com.colorinchi.app.service.ChatRunService;
import com.colorinchi.app.service.ChatSessionService;
import com.colorinchi.app.service.ModelRouter;
import com.colorinchi.app.service.StreamingChatClient;

import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private static final Logger log = LoggerFactory.getLogger(ChatApiController.class);
    private static final long SSE_TIMEOUT = 300_000L; // 5 minutes

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatRunService chatRunService;
    private final ChatFeedbackService chatFeedbackService;
    private final ChatPolicyService chatPolicyService;
    private final ChatPromptFactory chatPromptFactory;
    private final ModelRouter modelRouter;
    private final StreamingChatClient streamingChatClient;

    public ChatApiController(
            ChatSessionService chatSessionService,
            ChatMessageService chatMessageService,
            ChatRunService chatRunService,
            ChatFeedbackService chatFeedbackService,
            ChatPolicyService chatPolicyService,
            ChatPromptFactory chatPromptFactory,
            ModelRouter modelRouter,
            StreamingChatClient streamingChatClient) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.chatRunService = chatRunService;
        this.chatFeedbackService = chatFeedbackService;
        this.chatPolicyService = chatPolicyService;
        this.chatPromptFactory = chatPromptFactory;
        this.modelRouter = modelRouter;
        this.streamingChatClient = streamingChatClient;
    }

    // ---- Sessions ----

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request) {
        try {
            ChatSession session = chatSessionService.create(request);
            ChatSessionResponse response = ChatSessionResponse.from(
                    session.getId(), session.getTitle(), session.getModel(),
                    session.getStatus(), session.getCreatedAt(), session.getUpdatedAt());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create session", e);
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("create_failed", e.getMessage()));
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions() {
        try {
            List<ChatSessionResponse> result = chatSessionService.listByOwner().stream()
                    .map(s -> ChatSessionResponse.from(
                            s.getId(), s.getTitle(), s.getModel(),
                            s.getStatus(), s.getCreatedAt(), s.getUpdatedAt()))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to list sessions", e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("list_failed", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> listMessages(@PathVariable UUID sessionId) {
        MDC.put("sessionId", sessionId.toString());
        try {
            List<ChatMessageResponse> result = chatMessageService.listBySession(sessionId).stream()
                    .map(m -> new ChatMessageResponse(
                            m.getId(), m.getSessionId(), m.getRole(),
                            m.getContent(), m.getCreatedAt()))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to list messages for session {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("list_failed", e.getMessage()));
        } finally {
            MDC.remove("sessionId");
        }
    }

    // ---- Send message (creates run, returns runId for streaming) ----

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> body) {

        MDC.put("sessionId", sessionId.toString());
        try {
            ChatSession session = chatSessionService.getById(sessionId);
            String content = body.getOrDefault("content", "").trim();
            if (content.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("validation_error", "El mensaje no puede estar vacío"));
            }

            String requestedModel = body.getOrDefault("model", session.getModel());
            AiModelConfig modelConfig;
            try {
                modelConfig = modelRouter.resolve(requestedModel);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("invalid_model", e.getMessage()));
            }

            // Policy evaluation
            PolicyDecision decision = chatPolicyService.evaluate(content);
            if (!decision.isAllowed()) {
                if (decision.decision() == PolicyDecision.Decision.BLOCK) {
                    log.info("Policy BLOCK for session {}: {}", sessionId, decision.reason());
                    return ResponseEntity.ok(ChatSendResponse.blocked(sessionId, decision.refusalMessage()));
                }
                // FLAG — allow but log
                log.info("Policy FLAG for session {}: {}", sessionId, decision.reason());
            }

            // Persist user message
            ChatMessage userMessage = chatMessageService.create(sessionId, "user", content, 0);

            // Create and start run
            ChatRun run = chatRunService.create(sessionId, modelConfig.getId());
            chatRunService.started(run.getId());

            log.debug("Created run {} for session {} with model {}", run.getId(), sessionId, modelConfig.getId());

            return ResponseEntity.ok(ChatSendResponse.streaming(run.getId(), sessionId));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("not_found", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to send message in session {}", sessionId, e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("send_failed", e.getMessage()));
        } finally {
            MDC.remove("sessionId");
        }
    }

    // ---- SSE Streaming endpoint ----

    @GetMapping("/stream/{runId}")
    public SseEmitter streamRun(@PathVariable UUID runId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        StringBuilder accumulated = new StringBuilder();

        ChatRun run;
        ChatSession session;
        List<ChatMessage> previousMessages;

        try {
            run = chatRunService.getById(runId);
            session = chatSessionService.getById(run.getSessionId());
            previousMessages = chatMessageService.listBySession(run.getSessionId());
        } catch (Exception e) {
            log.error("Failed to initialize stream for run {}", runId, e);
            SseEmitter errorEmitter = new SseEmitter(0L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data(StreamChunk.error("No se pudo iniciar el stream: " + e.getMessage())));
            } catch (Exception ex) {
                // ignore
            }
            errorEmitter.complete();
            return errorEmitter;
        }

        // Build messages list for the AI
        List<Map<String, String>> aiMessages = new ArrayList<>();

        // 1. System prompt with wardrobe context
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", chatPromptFactory.buildSystemPrompt());
        aiMessages.add(systemMsg);

        // 2. All previous messages (excluding system, which we just added)
        for (ChatMessage msg : previousMessages) {
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole().equals("user") ? "user" : "assistant");
            m.put("content", msg.getContent());
            aiMessages.add(m);
        }

        log.debug("Starting AI stream for run {} with model {}", runId, run.getModelRequested());

        // Subscribe to the reactive Flux and feed into the SseEmitter
        Disposable subscription = streamingChatClient
                .stream(run.getModelRequested(), aiMessages)
                .publishOn(Schedulers.boundedElastic())
                .subscribe(
                    chunk -> {
                        try {
                            accumulated.append(chunk);
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(StreamChunk.chunk(chunk)));
                        } catch (Exception e) {
                            log.warn("Error sending SSE chunk for run {}: {}", runId, e.getMessage());
                        }
                    },
                    error -> {
                        log.error("Stream error for run {}: {}", runId, error.getMessage());
                        try {
                            chatRunService.fail(runId, error.getMessage());
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(StreamChunk.error(error.getMessage())));
                        } catch (Exception e) {
                            log.warn("Error sending SSE error for run {}", runId);
                        }
                        emitter.complete();
                    },
                    () -> {
                        try {
                            // Persist the full assistant message
                            String fullContent = accumulated.toString();
                            int tokens = fullContent.length() / 4; // rough estimate
                            ChatMessage assistantMsg = chatMessageService.create(
                                    run.getSessionId(), "assistant", fullContent, tokens);
                            chatRunService.complete(runId, run.getModelRequested(), tokens);

                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(StreamChunk.done(assistantMsg.getId())));
                        } catch (Exception e) {
                            log.warn("Error completing stream for run {}: {}", runId, e.getMessage());
                        }
                        emitter.complete();
                    });

        // Clean up subscription on emitter completion/timeout
        emitter.onCompletion(() -> {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
        });
        emitter.onTimeout(() -> {
            if (!subscription.isDisposed()) {
                subscription.dispose();
            }
            try {
                chatRunService.fail(runId, "Stream timeout");
            } catch (Exception e) {
                log.warn("Error marking run {} as failed on timeout", runId);
            }
        });

        return emitter;
    }

    // ---- Feedback ----

    @PostMapping("/runs/{runId}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable UUID runId,
            @RequestBody ChatFeedbackRequest request) {
        try {
            ChatRun run = chatRunService.getById(runId);
            chatFeedbackService.create(runId, run.getSessionId(), request);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("not_found", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to submit feedback for run {}", runId, e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("feedback_failed", e.getMessage()));
        }
    }

    // ---- Delete session ----

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable UUID sessionId) {
        try {
            chatSessionService.delete(sessionId);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("not_found", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete session {}", sessionId, e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("delete_failed", e.getMessage()));
        }
    }

    // ---- Available models ----

    @GetMapping("/models")
    public ResponseEntity<?> availableModels() {
        try {
            List<Map<String, Object>> models = modelRouter.getAvailableModels().stream()
                    .map(m -> {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("id", m.getId());
                        entry.put("name", m.getName() != null ? m.getName() : m.getId());
                        entry.put("isDefault", m.isDefault());
                        return entry;
                    })
                    .toList();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Failed to list models", e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("models_failed", e.getMessage()));
        }
    }
}
