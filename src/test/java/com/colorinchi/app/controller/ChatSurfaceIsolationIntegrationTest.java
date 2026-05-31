package com.colorinchi.app.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.colorinchi.app.config.RateLimitingInterceptor;
import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.repository.AnonymousOwnerRepository;
import com.colorinchi.app.service.CurrentOwnerAccessor;
import com.colorinchi.app.service.StreamingChatClient;

import reactor.core.publisher.Flux;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ChatSurfaceIsolationIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnonymousOwnerRepository anonymousOwnerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentOwnerAccessor currentOwnerAccessor;

    @MockitoBean
    private StreamingChatClient streamingChatClient;

    @MockitoBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    @BeforeEach
    void setUp() {
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(OWNER_ID);
        when(streamingChatClient.stream(any(), any())).thenReturn(Flux.empty());
        when(streamingChatClient.stream(any(), any(), any(), any())).thenReturn(Flux.empty());
        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        if (anonymousOwnerRepository.findById(OWNER_ID).isEmpty()) {
            AnonymousOwner owner = new AnonymousOwner();
            owner.setId(OWNER_ID);
            owner.setBootstrap(false);
            anonymousOwnerRepository.save(owner);
        }
    }

    @Test
    void companionSessionDoesNotAppearInMainChatList() throws Exception {
        UUID mainSessionId = UUID.randomUUID();
        UUID companionSessionId = UUID.randomUUID();
        insertSession(mainSessionId, ChatSurface.MAIN_CHAT, "Main session");
        insertSession(companionSessionId, ChatSurface.COMPANION, "Companion session");

        mockMvc.perform(get("/api/chat/sessions").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(mainSessionId.toString()))
                .andExpect(jsonPath("$[0].title").value("Main session"));
    }

    @Test
    void mainChatEndpointsRejectCompanionSessionId() throws Exception {
        UUID companionSessionId = UUID.randomUUID();
        UUID companionMessageId = UUID.randomUUID();
        insertSession(companionSessionId, ChatSurface.COMPANION, "Companion session");
        insertMessage(companionMessageId, companionSessionId, "assistant", "Secret companion thread");

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", companionSessionId).with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));

        mockMvc.perform(get("/api/chat/sessions/{sessionId}/messages", companionSessionId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    private void insertSession(UUID sessionId, ChatSurface surface, String title) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO chat_sessions (id, owner_id, title, model, status, created_at, updated_at, archived, surface) VALUES (?, ?, ?, ?, ?, ?, ?, false, ?)",
                sessionId,
                OWNER_ID,
                title,
                "deepseek-v4-flash",
                "active",
                now,
                now,
                surface.name());
    }

    private void insertMessage(UUID messageId, UUID sessionId, String role, String content) {
        jdbcTemplate.update(
                "INSERT INTO chat_messages (id, session_id, owner_id, role, content, tokens, created_at) VALUES (?, ?, ?, ?, ?, 0, ?)",
                messageId,
                sessionId,
                OWNER_ID,
                role,
                content,
                OffsetDateTime.now());
    }
}
