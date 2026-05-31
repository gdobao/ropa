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
import com.colorinchi.app.repository.ChatSessionRepository;
import com.colorinchi.app.service.CurrentOwnerAccessor;
import com.colorinchi.app.service.StreamingChatClient;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CompanionChatSurfaceIntegrationTest {

    private static final UUID OWNER_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnonymousOwnerRepository anonymousOwnerRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

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
    void companionEndpointsPersistAndListOnlyCompanionSessions() throws Exception {
        mockMvc.perform(post("/api/companion/sessions").with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Global companion\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Global companion"));

        assertThat(chatSessionRepository.findAllByOwnerIdAndSurfaceAndArchivedFalseOrderByUpdatedAtDesc(OWNER_ID, ChatSurface.COMPANION))
                .hasSize(1)
                .allMatch(session -> session.getSurface() == ChatSurface.COMPANION);

        mockMvc.perform(get("/api/companion/sessions").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Global companion"));

        mockMvc.perform(get("/api/chat/sessions").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void companionEndpointsRejectMainChatSessionId() throws Exception {
        UUID mainSessionId = UUID.randomUUID();
        insertSession(mainSessionId, ChatSurface.MAIN_CHAT, "Main session");

        mockMvc.perform(get("/api/companion/sessions/{sessionId}/messages", mainSessionId).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", mainSessionId).with(csrf())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola\"}"))
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
}
