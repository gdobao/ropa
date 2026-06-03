package com.colorinchi.app.controller;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.colorinchi.app.dto.chat.ChatFeedbackRequest;
import com.colorinchi.app.dto.chat.ChatMessageResponse;
import com.colorinchi.app.dto.chat.ChatSessionResponse;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.dto.chat.ErrorResponse;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.service.ChatConversationOrchestrator;
import com.colorinchi.app.service.ChatFeedbackService;
import com.colorinchi.app.service.ChatMessageService;
import com.colorinchi.app.service.ChatSessionService;
import com.colorinchi.app.service.CompanionTipService;

@RestController
@RequestMapping("/api/companion")
public class CompanionChatApiController {

    private static final Logger log = LoggerFactory.getLogger(CompanionChatApiController.class);

    private final ChatSessionService chatSessionService;
    private final ChatMessageService chatMessageService;
    private final ChatConversationOrchestrator chatConversationOrchestrator;
    private final CompanionTipService companionTipService;
    private final ChatFeedbackService chatFeedbackService;

    public CompanionChatApiController(
            ChatSessionService chatSessionService,
            ChatMessageService chatMessageService,
            ChatConversationOrchestrator chatConversationOrchestrator,
            CompanionTipService companionTipService,
            ChatFeedbackService chatFeedbackService) {
        this.chatSessionService = chatSessionService;
        this.chatMessageService = chatMessageService;
        this.chatConversationOrchestrator = chatConversationOrchestrator;
        this.companionTipService = companionTipService;
        this.chatFeedbackService = chatFeedbackService;
    }

    @GetMapping("/context")
    public ResponseEntity<?> context() {
        return companionContext();
    }

    @GetMapping("/tips")
    public ResponseEntity<?> tips() {
        return companionContext();
    }

    private ResponseEntity<?> companionContext() {
        try {
            return ResponseEntity.ok(companionTipService.assemble());
        } catch (Exception e) {
            log.error("Failed to build companion context", e);
            return ResponseEntity.internalServerError().body(ErrorResponse.of("context_failed", "Error al obtener el contexto"));
        }
    }

    @PatchMapping("/sessions/{sessionId}")
    public ResponseEntity<?> updateTitle(@PathVariable UUID sessionId, @RequestBody(required = false) Map<String, String> body) {
        try {
            String title = body == null ? null : body.get("title");
            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("invalid_request", "Title is required"));
            }
            var session = chatSessionService.updateTitle(ChatSurface.COMPANION, sessionId, title);
            return ResponseEntity.ok(ChatSessionResponse.from(
                    session.getId(), session.getTitle(), session.getModel(),
                    session.getStatus(), session.getCreatedAt(), session.getUpdatedAt()));
        } catch (IllegalArgumentException e) {
            if (isInvalidTitle(e)) {
                return ResponseEntity.badRequest().body(ErrorResponse.of("invalid_request", "Title is too long"));
            }
            return ResponseEntity.badRequest().body(ErrorResponse.of("not_found", "Sesión no encontrada"));
        } catch (Exception e) {
            log.error("Failed to update companion session title {}", sessionId, e);
            return ResponseEntity.internalServerError().body(ErrorResponse.of("update_failed", "Error al renombrar la sesión"));
        }
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody CreateSessionRequest request) {
        try {
            var session = chatSessionService.create(request, ChatSurface.COMPANION);
            return ResponseEntity.ok(ChatSessionResponse.from(
                    session.getId(), session.getTitle(), session.getModel(),
                    session.getStatus(), session.getCreatedAt(), session.getUpdatedAt()));
        } catch (Exception e) {
            log.error("Failed to create companion session", e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("create_failed", "No se pudo crear la sesión"));
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions() {
        try {
            List<ChatSessionResponse> result = chatSessionService.listByOwner(ChatSurface.COMPANION).stream()
                    .map(s -> ChatSessionResponse.from(
                            s.getId(), s.getTitle(), s.getModel(),
                            s.getStatus(), s.getCreatedAt(), s.getUpdatedAt()))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to list companion sessions", e);
            return ResponseEntity.internalServerError().body(ErrorResponse.of("list_failed", "Error al listar las sesiones"));
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> listMessages(@PathVariable UUID sessionId) {
        MDC.put("sessionId", sessionId.toString());
        try {
            chatSessionService.getById(ChatSurface.COMPANION, sessionId);
            List<ChatMessageResponse> result = chatMessageService.listBySession(sessionId).stream()
                    .map(m -> new ChatMessageResponse(
                            m.getId(), m.getSessionId(), m.getRole(),
                            m.getContent(), m.getCreatedAt()))
                    .toList();
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of("not_found", "Sesión no encontrada"));
        } catch (Exception e) {
            log.error("Failed to list messages for companion session {}", sessionId, e);
            return ResponseEntity.badRequest().body(ErrorResponse.of("list_failed", "Error al listar los mensajes"));
        } finally {
            MDC.remove("sessionId");
        }
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable UUID sessionId, @RequestBody(required = false) Map<String, String> body) {
        MDC.put("sessionId", sessionId.toString());
        try {
            if (body == null) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("validation_error", "El cuerpo de la solicitud es obligatorio"));
            }
            return ResponseEntity.ok(chatConversationOrchestrator.sendMessage(ChatSurface.COMPANION, sessionId, body));
        } catch (ChatConversationOrchestrator.EmptyChatMessageException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of("validation_error", "El mensaje no puede estar vacío"));
        } catch (ChatConversationOrchestrator.ChatMessageTooLongException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of("validation_error", "El mensaje es demasiado largo"));
        } catch (IllegalArgumentException e) {
            String error = e.getMessage() != null && e.getMessage().startsWith("Modelo no soportado")
                    ? "invalid_model"
                    : "not_found";
            String message = "invalid_model".equals(error) ? "Modelo no soportado" : "Sesión no encontrada";
            return ResponseEntity.badRequest().body(ErrorResponse.of(error, message));
        } catch (Exception e) {
            log.error("Failed to send message in companion session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of("send_failed", "Error al enviar el mensaje"));
        } finally {
            MDC.remove("sessionId");
        }
    }

    @GetMapping("/stream/{runId}")
    public SseEmitter streamRun(@PathVariable UUID runId) {
        return chatConversationOrchestrator.streamRun(ChatSurface.COMPANION, runId);
    }

    // ---- Feedback ----

    @PostMapping("/messages/{messageId}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable UUID messageId,
            @RequestBody ChatFeedbackRequest request) {
        return submitFeedbackForSession(null, messageId, request);
    }

    @PostMapping("/sessions/{sessionId}/messages/{messageId}/feedback")
    public ResponseEntity<?> submitFeedbackForSession(
            @PathVariable UUID sessionId,
            @PathVariable UUID messageId,
            @RequestBody ChatFeedbackRequest request) {
        MDC.put("sessionId", "feedback:" + messageId);
        try {
            ChatMessage msg = chatMessageService.getById(messageId);
            if (sessionId != null && !sessionId.equals(msg.getSessionId())) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.of("not_found", "Message not found"));
            }
            chatSessionService.getById(ChatSurface.COMPANION, msg.getSessionId());
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
            log.error("Failed to submit companion feedback for message {}", messageId, e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.of("feedback_failed", "Error al procesar la valoración"));
        } finally {
            MDC.remove("sessionId");
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable UUID sessionId) {
        try {
            chatSessionService.delete(ChatSurface.COMPANION, sessionId);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of("not_found", "Sesión no encontrada"));
        } catch (Exception e) {
            log.error("Failed to delete companion session {}", sessionId, e);
            return ResponseEntity.internalServerError().body(ErrorResponse.of("delete_failed", "Error al eliminar la sesión"));
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
