package com.colorinchi.app.service;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.colorinchi.app.config.RateLimitingInterceptor;
import com.colorinchi.app.model.ChatFeedback;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatRun;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.repository.ChatFeedbackRepository;
import com.colorinchi.app.repository.ChatMessageRepository;
import com.colorinchi.app.repository.ChatRunRepository;
import com.colorinchi.app.repository.ChatSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true")
class ChatEndToEndIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRunRepository chatRunRepository;

    @Autowired
    private ChatFeedbackRepository chatFeedbackRepository;

    @MockitoBean
    private StreamingChatClient streamingChatClient;

    @MockitoBean
    private CurrentOwnerAccessor currentOwnerAccessor;

    @MockitoBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID otherOwnerId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        when(streamingChatClient.stream(any(), any())).thenReturn(Flux.empty());
        when(streamingChatClient.stream(any(), any(), any(), any())).thenReturn(Flux.empty());
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private UUID extractUuid(String json, String field) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return UUID.fromString(node.get(field).asText());
    }

    @Test
    void fullPipelineCreateSessionSendMessageAndSubmitFeedback() throws Exception {
        // ---- Step 1: Create session ----
        String sessionJson = mockMvc.perform(post("/api/chat/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Mi consulta de moda\",\"model\":\"deepseek-v4-flash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Mi consulta de moda"))
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn().getResponse().getContentAsString();

        UUID sessionUuid = extractUuid(sessionJson, "id");

        // Verify session persisted with correct owner
        ChatSession savedSession = chatSessionRepository.findById(sessionUuid).orElseThrow();
        assertThat(savedSession.getOwnerId()).isEqualTo(ownerId);
        assertThat(savedSession.getTitle()).isEqualTo("Mi consulta de moda");
        assertThat(savedSession.getModel()).isEqualTo("deepseek-v4-flash");
        assertThat(savedSession.getStatus()).isEqualTo("active");
        assertThat(savedSession.isArchived()).isFalse();
        assertThat(savedSession.getCreatedAt()).isNotNull();
        assertThat(savedSession.getUpdatedAt()).isNotNull();

        // ---- Step 2: Send message ----
        String msgJson = mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionUuid).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola, necesito ayuda con mi guardarropa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNotEmpty())
                .andExpect(jsonPath("$.blocked").value(false))
                .andReturn().getResponse().getContentAsString();

        UUID runUuid = extractUuid(msgJson, "runId");

        // ---- Step 3: Verify run created ----
        ChatRun run = chatRunRepository.findById(runUuid).orElseThrow();
        assertThat(run.getSessionId()).isEqualTo(sessionUuid);
        assertThat(run.getOwnerId()).isEqualTo(ownerId);
        assertThat(run.getModelRequested()).isEqualTo("deepseek-v4-flash");
        assertThat(run.getStatus()).isIn("pending", "running");
        assertThat(run.getCreatedAt()).isNotNull();

        // ---- Step 4: Verify message persisted ----
        List<ChatMessage> messages = chatMessageRepository
                .findAllBySessionIdAndOwnerIdOrderByCreatedAtAsc(sessionUuid, ownerId);
        assertThat(messages).hasSize(1);
        ChatMessage message = messages.get(0);
        assertThat(message.getSessionId()).isEqualTo(sessionUuid);
        assertThat(message.getOwnerId()).isEqualTo(ownerId);
        assertThat(message.getRole()).isEqualTo("user");
        assertThat(message.getContent()).isEqualTo("Hola, necesito ayuda con mi guardarropa");

        // ---- Step 5: Submit feedback ----
        mockMvc.perform(post("/api/chat/messages/{messageId}/feedback", message.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"up\",\"comment\":\"Muy útil\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        // ---- Step 6: Verify feedback persisted ----
        List<ChatFeedback> feedbacks = chatFeedbackRepository.findAllByMessageIdAndOwnerId(message.getId(), ownerId);
        assertThat(feedbacks).hasSize(1);
        ChatFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getMessageId()).isEqualTo(message.getId());
        assertThat(feedback.getRunId()).isNull();
        assertThat(feedback.getSessionId()).isEqualTo(sessionUuid);
        assertThat(feedback.getOwnerId()).isEqualTo(ownerId);
        assertThat(feedback.getRating()).isEqualTo("up");
        assertThat(feedback.getComment()).isEqualTo("Muy útil");
    }

    @Test
    void ownerScopingIsEnforced() throws Exception {
        // Create a session as ownerId
        String sessionJson = mockMvc.perform(post("/api/chat/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"My Session\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID sessionUuid = extractUuid(sessionJson, "id");

        // Switch to other owner
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(otherOwnerId);

        // Other owner cannot send a message (session not found for this owner)
        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionUuid).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"should not work\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));

        // Other owner's session list should be empty
        assertThat(chatSessionRepository.findAllByOwnerIdAndSurfaceOrderByUpdatedAtDesc(otherOwnerId, ChatSurface.MAIN_CHAT))
                .isEmpty();
    }
}
