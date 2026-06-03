package com.colorinchi.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.colorinchi.app.dto.chat.ChatFeedbackRequest;
import com.colorinchi.app.dto.chat.ChatMessageResponse;
import com.colorinchi.app.dto.chat.ChatSessionResponse;
import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.dto.chat.ErrorResponse;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.service.ChatConversationOrchestrator;
import com.colorinchi.app.service.ChatFeedbackService;
import com.colorinchi.app.service.ChatMessageService;
import com.colorinchi.app.service.ChatSessionService;
import com.colorinchi.app.service.ModelRouter;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private static final Logger log = LoggerFactory.getLogger(ChatApiController.class);

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatFeedbackService chatFeedbackService;
    private final ChatConversationOrchestrator chatConversationOrchestrator;
    private final ModelRouter modelRouter;

    public ChatApiController(
            ChatSessionService chatSessionService,
            ChatMessageService chatMessageService,
            ChatFeedbackService chatFeedbackService,
            ChatConversationOrchestrator chatConversationOrchestrator,
            ModelRouter modelRouter) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.chatFeedbackService = chatFeedbackService;
        this.chatConversationOrchestrator = chatConversationOrchestrator;
        this.modelRouter = modelRouter;
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
                    .body(ErrorResponse.of("create_failed", "No se pudo crear la sesión"));
        }
    }

    @PatchMapping("/sessions/{id}/title")
    public ResponseEntity<?> updateTitle(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String title = body == null ? null : body.get("title");
            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("invalid_request", "Title is required"));
            }
            ChatSession session = chatSessionService.updateTitle(id, title);
            ChatSessionResponse response = ChatSessionResponse.from(
                    session.getId(), session.getTitle(), session.getModel(),
                    session.getStatus(), session.getCreatedAt(), session.getUpdatedAt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            if (isInvalidTitle(e)) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("invalid_request", "Title is too long"));
            }
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("not_found", "Sesión no encontrada"));
        } catch (Exception e) {
            log.error("Failed to update title for session {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of("update_failed", "Failed to update session title"));
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
                    .body(ErrorResponse.of("list_failed", "Error al listar las sesiones"));
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> listMessages(@PathVariable UUID sessionId) {
        MDC.put("sessionId", sessionId.toString());
        try {
            // Enforce main-chat surface ownership before exposing any thread history.
            chatSessionService.getById(ChatSurface.MAIN_CHAT, sessionId);
            List<ChatMessageResponse> result = chatMessageService.listBySession(sessionId).stream()
                    .map(m -> new ChatMessageResponse(
                            m.getId(), m.getSessionId(), m.getRole(),
                            m.getContent(), m.getCreatedAt()))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("not_found", "Sesión no encontrada"));
        } catch (Exception e) {
            log.error("Failed to list messages for session {}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("list_failed", "Error al listar los mensajes"));
        } finally {
            MDC.remove("sessionId");
        }
    }

    // ---- Send message (creates run, returns runId for streaming) ----

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) Map<String, String> body) {

        MDC.put("sessionId", sessionId.toString());
        try {
            if (body == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("validation_error", "El cuerpo de la solicitud es obligatorio"));
            }
            return ResponseEntity.ok(chatConversationOrchestrator.sendMessage(ChatSurface.MAIN_CHAT, sessionId, body));
        } catch (ChatConversationOrchestrator.EmptyChatMessageException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("validation_error", "El mensaje no puede estar vacío"));
        } catch (ChatConversationOrchestrator.ChatMessageTooLongException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("validation_error", "El mensaje es demasiado largo"));
        } catch (IllegalArgumentException e) {
            String error = e.getMessage() != null && e.getMessage().startsWith("Modelo no soportado")
                    ? "invalid_model"
                    : "not_found";
            String message = "invalid_model".equals(error) ? "Modelo no soportado" : "Sesión no encontrada";
            return ResponseEntity.badRequest().body(ErrorResponse.of(error, message));
        } catch (Exception e) {
            log.error("Failed to send message in session {}", sessionId, e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("send_failed", "Error al enviar el mensaje"));
        } finally {
            MDC.remove("sessionId");
        }
    }

    // ---- SSE Streaming endpoint ----

    @GetMapping("/stream/{runId}")
    public SseEmitter streamRun(@PathVariable UUID runId) {
        return chatConversationOrchestrator.streamRun(ChatSurface.MAIN_CHAT, runId);
    }

    // ---- Feedback ----

    @PostMapping("/messages/{messageId}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable UUID messageId,
            @RequestBody ChatFeedbackRequest request) {
        try {
            ChatMessage msg = chatMessageService.getById(messageId);
            // SURFACE CHECK: verify the message's session is MAIN_CHAT
            chatSessionService.getById(ChatSurface.MAIN_CHAT, msg.getSessionId());
            chatFeedbackService.create(messageId, msg.getRunId(), msg.getSessionId(), request);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            if (isInvalidFeedback(e)) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("validation_error", "Valoración no válida"));
            }
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("not_found", "Message not found"));
        } catch (Exception e) {
            log.error("Failed to submit feedback for message {}", messageId, e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("feedback_failed", "Error al procesar la valoración"));
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
                    .body(ErrorResponse.of("not_found", "Sesión no encontrada"));
        } catch (Exception e) {
            log.error("Failed to delete session {}", sessionId, e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("delete_failed", "Error al eliminar la sesión"));
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
                    .body(ErrorResponse.of("models_failed", "Error al listar los modelos"));
        }
    }

    private boolean isInvalidTitle(IllegalArgumentException e) {
        return e.getMessage() != null && e.getMessage().contains("title");
    }

    private boolean isInvalidFeedback(IllegalArgumentException e) {
        return e.getMessage() != null
                && (e.getMessage().startsWith("Rating") || e.getMessage().startsWith("Comment"));
    }
}
