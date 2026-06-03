package com.colorinchi.app.controller;

import java.nio.file.Path;
import java.time.OffsetDateTime;

import org.springframework.util.unit.DataSize;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.config.RateLimitingInterceptor;
import com.colorinchi.app.config.UploadProperties;
import org.springframework.test.util.ReflectionTestUtils;
import com.colorinchi.app.dto.chat.ChatFeedbackRequest;
import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.dto.chat.PolicyDecision;
import com.colorinchi.app.dto.chat.WardrobeContext;
import com.colorinchi.app.model.ChatFeedback;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatRun;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.service.AnonymousOwnerService;
import com.colorinchi.app.service.ChatConversationOrchestrator;
import com.colorinchi.app.service.ChatFeedbackService;
import com.colorinchi.app.service.ChatMessageService;
import com.colorinchi.app.service.ChatRunService;
import com.colorinchi.app.service.ChatSessionService;
import com.colorinchi.app.service.ModelRouter;
import com.colorinchi.app.service.StreamingChatClient;
import com.colorinchi.app.service.WardrobeContextAssembler;

import reactor.core.publisher.Flux;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ChatApiControllerTest.TestConfig.class)
class ChatApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatSessionService chatSessionService;

    @MockitoBean
    private ChatMessageService chatMessageService;

    @MockitoBean
    private ChatRunService chatRunService;

    @MockitoBean
    private ChatFeedbackService chatFeedbackService;

    @MockitoBean
    private ChatConversationOrchestrator chatConversationOrchestrator;

    @MockitoBean
    private ModelRouter modelRouter;

    @MockitoBean
    private StreamingChatClient streamingChatClient;

    @MockitoBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    @MockitoBean
    private AnonymousOwnerService anonymousOwnerService;

    private UUID sessionId;
    private UUID messageId;
    private UUID runId;
    private ChatSession sampleSession;
    private ChatMessage sampleMessage;
    private ChatRun sampleRun;
    private AiModelConfig defaultModel;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        runId = UUID.randomUUID();

        sampleSession = new ChatSession();
        sampleSession.setId(sessionId);
        sampleSession.setTitle("Test Chat");
        sampleSession.setModel("deepseek-v4-flash");
        sampleSession.setStatus("active");
        sampleSession.setSurface(ChatSurface.MAIN_CHAT);

        sampleMessage = new ChatMessage();
        sampleMessage.setId(messageId);
        sampleMessage.setSessionId(sessionId);
        sampleMessage.setRole("user");
        sampleMessage.setContent("Hola");

        sampleRun = new ChatRun();
        sampleRun.setId(runId);
        sampleRun.setSessionId(sessionId);
        sampleRun.setModelRequested("deepseek-v4-flash");
        sampleRun.setStatus("running");

        defaultModel = new AiModelConfig();
        defaultModel.setId("deepseek-v4-flash");
        defaultModel.setName("DeepSeek V4 Flash");
        defaultModel.setDefault(true);

        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(modelRouter.resolve((String) any())).thenReturn(defaultModel);
        when(modelRouter.resolve(eq("deepseek-v4-flash"))).thenReturn(defaultModel);
        when(modelRouter.getAvailableModels()).thenReturn(List.of(defaultModel));
        when(modelRouter.findDefault()).thenReturn(defaultModel);
    }

    // ---- Create session ----

    @Test
    void createSessionReturnsCreatedSession() throws Exception {
        when(chatSessionService.create(any(CreateSessionRequest.class))).thenReturn(sampleSession);

        mockMvc.perform(post("/api/chat/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Chat\",\"model\":\"deepseek-v4-flash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.title").value("Test Chat"))
                .andExpect(jsonPath("$.model").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void createSessionWithEmptyBodyReturnsSession() throws Exception {
        when(chatSessionService.create(any(CreateSessionRequest.class))).thenReturn(sampleSession);

        mockMvc.perform(post("/api/chat/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()));
    }

    // ---- List sessions ----

    @Test
    void listSessionsReturnsEmptyList() throws Exception {
        when(chatSessionService.listByOwner()).thenReturn(List.of());

        mockMvc.perform(get("/api/chat/sessions").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listSessionsWithData() throws Exception {
        when(chatSessionService.listByOwner()).thenReturn(List.of(sampleSession));

        mockMvc.perform(get("/api/chat/sessions").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].title").value("Test Chat"))
                .andExpect(jsonPath("$[0].model").value("deepseek-v4-flash"));
    }

    @Test
    void listSessionsDoesNotExposeCompanionSession() throws Exception {
        when(chatSessionService.listByOwner()).thenReturn(List.of(sampleSession));

        mockMvc.perform(get("/api/chat/sessions").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()));
    }

    @Test
    void updateTitleWithNullJsonBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/chat/sessions/{sessionId}/title", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.message").value("Title is required"));
    }

    // ---- List messages ----

    @Test
    void listMessagesReturnsEmptyForNewSession() throws Exception {
        when(chatMessageService.listBySession(sessionId)).thenReturn(List.of());

        mockMvc.perform(get("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void listMessagesWithData() throws Exception {
        when(chatMessageService.listBySession(sessionId)).thenReturn(List.of(sampleMessage));

        mockMvc.perform(get("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[0].content").value("Hola"));
    }

    // ---- Send message ----

    @Test
    void sendMessageCreatesRunAndReturnsRunId() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.MAIN_CHAT), eq(sessionId), any()))
                .thenReturn(com.colorinchi.app.dto.chat.ChatSendResponse.streaming(runId, sessionId));

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola, necesito ayuda\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.blocked").value(false));
    }

    @Test
    void sendMessageBlockedByPolicy() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.MAIN_CHAT), eq(sessionId), any()))
                .thenReturn(com.colorinchi.app.dto.chat.ChatSendResponse.blocked(sessionId, "No puedo elegir un outfit por vos"));

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Elegime un outfit\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.refusalMessage").value("No puedo elegir un outfit por vos"));
    }

    @Test
    void sendMessageWithEmptyContentReturnsError() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.MAIN_CHAT), eq(sessionId), any()))
                .thenThrow(new ChatConversationOrchestrator.EmptyChatMessageException());

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void sendMessageWithTooLongContentReturnsError() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.MAIN_CHAT), eq(sessionId), any()))
                .thenThrow(new ChatConversationOrchestrator.ChatMessageTooLongException());

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"too long\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }

    // ---- Feedback ----

    @Test
    void submitFeedbackReturnsOk() throws Exception {
        when(chatMessageService.getById(any(UUID.class))).thenReturn(sampleMessage);
        when(chatSessionService.getById(ChatSurface.MAIN_CHAT, sessionId)).thenReturn(sampleSession);
        when(chatFeedbackService.create(any(), any(), any(), any(ChatFeedbackRequest.class)))
                .thenReturn(new ChatFeedback());

        mockMvc.perform(post("/api/chat/messages/{messageId}/feedback", messageId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"up\",\"comment\":\"Great advice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(chatFeedbackService).create(eq(messageId), isNull(), eq(sessionId), any(ChatFeedbackRequest.class));
    }

    @Test
    void submitInvalidFeedbackReturnsValidationError() throws Exception {
        when(chatMessageService.getById(any(UUID.class))).thenReturn(sampleMessage);
        when(chatSessionService.getById(ChatSurface.MAIN_CHAT, sessionId)).thenReturn(sampleSession);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Rating must be 'up' or 'down'"))
                .when(chatFeedbackService).create(any(), any(), any(), any(ChatFeedbackRequest.class));

        mockMvc.perform(post("/api/chat/messages/{messageId}/feedback", messageId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }

    // ---- Delete session ----

    @Test
    void deleteSessionReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/chat/sessions/{sessionId}", sessionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        verify(chatSessionService).delete(sessionId);
    }

    @Test
    void deleteNonExistentSessionReturnsError() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Chat session not found"))
                .when(chatSessionService).delete(sessionId);

        mockMvc.perform(delete("/api/chat/sessions/{sessionId}", sessionId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    // ---- Available models ----

    @Test
    void availableModelsReturnsList() throws Exception {
        AiModelConfig model2 = new AiModelConfig();
        model2.setId("gemma4");
        model2.setName("Gemma 4");
        model2.setDefault(false);

        when(modelRouter.getAvailableModels()).thenReturn(List.of(defaultModel, model2));

        mockMvc.perform(get("/api/chat/models").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("deepseek-v4-flash"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].id").value("gemma4"))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    // ---- List messages for non-existent session ----

    @Test
    void listMessagesForNonExistentSessionReturnsError() throws Exception {
        when(chatSessionService.getById(ChatSurface.MAIN_CHAT, sessionId))
                .thenThrow(new IllegalArgumentException("Chat session not found"));

        mockMvc.perform(get("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void sendMessageRejectsCompanionSessionId() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.MAIN_CHAT), eq(sessionId), any()))
                .thenThrow(new IllegalArgumentException("Chat session not found"));

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    // ---- Send message with invalid model ----

    @Test
    void sendMessageWithInvalidModelReturnsError() throws Exception {
        when(chatConversationOrchestrator.sendMessage(eq(ChatSurface.MAIN_CHAT), eq(sessionId), any()))
                .thenThrow(new IllegalArgumentException("Modelo no soportado: bad-model"));

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\",\"model\":\"bad-model\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_model"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        WardrobeContext dummyWardrobeContext() {
            return new WardrobeContext(0L, List.of(), List.of(), List.of(), java.util.Map.of(), 0L, 0L, 0L, null, List.of(), List.of(), java.util.Map.of());
        }
        @Bean
        UploadProperties uploadProperties() {
            return new UploadProperties(Path.of("/tmp"), DataSize.ofMegabytes(8), List.of("image/jpeg"), 6000, 6000, 24_000_000L);
        }
    }
}
