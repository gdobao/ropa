package com.colorinchi.app.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.unit.DataSize;

import com.colorinchi.app.config.RateLimitingInterceptor;
import com.colorinchi.app.config.UploadProperties;
import com.colorinchi.app.dto.chat.ChatFeedbackRequest;
import com.colorinchi.app.dto.chat.CompanionTipContext;
import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.model.ChatFeedback;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.service.AnonymousOwnerService;
import com.colorinchi.app.service.ChatConversationOrchestrator;
import com.colorinchi.app.service.ChatFeedbackService;
import com.colorinchi.app.service.ChatMessageService;
import com.colorinchi.app.service.ChatSessionService;
import com.colorinchi.app.service.CompanionTipService;
import com.colorinchi.app.service.WardrobeContextAssembler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanionChatApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CompanionChatApiControllerTest.TestConfig.class)
class CompanionChatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatSessionService chatSessionService;

    @MockitoBean
    private ChatMessageService chatMessageService;

    @MockitoBean
    private ChatConversationOrchestrator chatConversationOrchestrator;

    @MockitoBean
    private CompanionTipService companionTipService;

    @MockitoBean
    private ChatFeedbackService chatFeedbackService;

    @MockitoBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    @MockitoBean
    private AnonymousOwnerService anonymousOwnerService;

    private UUID sessionId;
    private UUID runId;
    private ChatSession companionSession;
    private ChatMessage message;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        runId = UUID.randomUUID();

        companionSession = new ChatSession();
        companionSession.setId(sessionId);
        companionSession.setTitle("Companion thread");
        companionSession.setModel("deepseek-v4-flash");
        companionSession.setStatus("active");
        companionSession.setSurface(ChatSurface.COMPANION);

        message = new ChatMessage();
        message.setId(UUID.randomUUID());
        message.setSessionId(sessionId);
        message.setRole("assistant");
        message.setContent("Hola desde companion");

        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void createSessionUsesCompanionSurface() throws Exception {
        when(chatSessionService.create(any(CreateSessionRequest.class), eq(ChatSurface.COMPANION))).thenReturn(companionSession);

        mockMvc.perform(post("/api/companion/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Companion thread\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.title").value("Companion thread"));
    }

    @Test
    void contextReturnsCompanionTips() throws Exception {
        when(companionTipService.assemble())
                .thenReturn(new CompanionTipContext("Armario con 4 prendas.", List.of("Sumá contraste suave.")));

        mockMvc.perform(get("/api/companion/context").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Armario con 4 prendas."))
                .andExpect(jsonPath("$.tips[0]").value("Sumá contraste suave."));
    }

    @Test
    void listSessionsReturnsCompanionThreads() throws Exception {
        when(chatSessionService.listByOwner(ChatSurface.COMPANION)).thenReturn(List.of(companionSession));

        mockMvc.perform(get("/api/companion/sessions").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].title").value("Companion thread"));
    }

    @Test
    void listMessagesChecksCompanionSurface() throws Exception {
        when(chatSessionService.getById(ChatSurface.COMPANION, sessionId)).thenReturn(companionSession);
        when(chatMessageService.listBySession(sessionId)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/companion/sessions/{sessionId}/messages", sessionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hola desde companion"));
    }

    @Test
    void sendMessageUsesCompanionOrchestrator() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.COMPANION), eq(sessionId), any()))
                .thenReturn(com.colorinchi.app.dto.chat.ChatSendResponse.streaming(runId, sessionId));

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.blocked").value(false));
    }

    @Test
    void sendMessageEmptyContentReturnsValidationError() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.COMPANION), eq(sessionId), any()))
                .thenThrow(new ChatConversationOrchestrator.EmptyChatMessageException());

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void sendMessageNullContentReturnsValidationError() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.COMPANION), eq(sessionId), any()))
                .thenThrow(new ChatConversationOrchestrator.EmptyChatMessageException());

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void sendMessageMissingContentReturnsValidationError() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.COMPANION), eq(sessionId), any()))
                .thenThrow(new ChatConversationOrchestrator.EmptyChatMessageException());

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void deleteSessionUsesCompanionSurface() throws Exception {
        mockMvc.perform(delete("/api/companion/sessions/{sessionId}", sessionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ok\"}"));

        verify(chatSessionService).delete(ChatSurface.COMPANION, sessionId);
    }

    // ---- Stream ----

    @Test
    void streamRunReturnsOk() throws Exception {
        SseEmitter mockEmitter = new SseEmitter(300000L);
        when(chatConversationOrchestrator.streamRun(eq(ChatSurface.COMPANION), eq(runId)))
                .thenReturn(mockEmitter);

        mockMvc.perform(get("/api/companion/stream/{runId}", runId))
                .andExpect(status().isOk());
    }

    @Test
    void streamRunWhenErrorReturnsServerError() throws Exception {
        when(chatConversationOrchestrator.streamRun(eq(ChatSurface.COMPANION), eq(runId)))
                .thenThrow(new IllegalArgumentException("Chat run not found"));

        mockMvc.perform(get("/api/companion/stream/{runId}", runId))
                .andExpect(status().is4xxClientError());
    }

    // ---- Feedback ----

    @Test
    void submitFeedbackReturnsOk() throws Exception {
        when(chatMessageService.getById(any(UUID.class))).thenReturn(message);
        when(chatSessionService.getById(ChatSurface.COMPANION, sessionId)).thenReturn(companionSession);
        when(chatFeedbackService.create(any(), any(), any(), any(ChatFeedbackRequest.class)))
                .thenReturn(new ChatFeedback());

        mockMvc.perform(post("/api/companion/messages/{messageId}/feedback", message.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"up\",\"comment\":\"Great\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ---- Error paths ----

    @Test
    void contextFailsReturnsContextFailedError() throws Exception {
        when(companionTipService.assemble())
                .thenThrow(new RuntimeException("DB connection failed"));

        mockMvc.perform(get("/api/companion/context").with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("context_failed"));
    }

    @Test
    void sendMessageWithInvalidModelReturnsError() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.COMPANION), eq(sessionId), any()))
                .thenThrow(new IllegalArgumentException("Modelo no soportado: bad-model"));

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\",\"model\":\"bad-model\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_model"));
    }

    @Test
    void sendMessageWithNonExistentSessionReturnsNotFound() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.COMPANION), eq(sessionId), any()))
                .thenThrow(new IllegalArgumentException("Chat session not found"));

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        WardrobeContextAssembler wardrobeContextAssembler() {
            return org.mockito.Mockito.mock(WardrobeContextAssembler.class);
        }

        @Bean
        UploadProperties uploadProperties() {
            return new UploadProperties(Path.of("/tmp"), DataSize.ofMegabytes(8), List.of("image/jpeg"));
        }
    }
}
