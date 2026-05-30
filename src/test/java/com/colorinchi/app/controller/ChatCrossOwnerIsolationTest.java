package com.colorinchi.app.controller;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.colorinchi.app.config.RateLimitingInterceptor;
import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.repository.AnonymousOwnerRepository;
import com.colorinchi.app.service.CurrentOwnerAccessor;
import com.colorinchi.app.service.StreamingChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for cross-owner data isolation in the chat feature.
 *
 * <p>Owner A creates a session; Owner B must not be able to access it.
 * Each owner can only see their own data. Pre-seeds anonymous owners
 * in the database to satisfy foreign key constraints.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ChatCrossOwnerIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnonymousOwnerRepository anonymousOwnerRepository;

    @MockitoBean
    private CurrentOwnerAccessor currentOwnerAccessor;

    @MockitoBean
    private StreamingChatClient streamingChatClient;

    @MockitoBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    private final UUID ownerA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID ownerB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        when(streamingChatClient.stream(any(), any())).thenReturn(Flux.empty());
        when(streamingChatClient.stream(any(), any(), any(), any())).thenReturn(Flux.empty());
        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        // Pre-seed owners so FK constraints are satisfied when services persist entities
        seedOwner(ownerA);
        seedOwner(ownerB);
    }

    private void seedOwner(UUID id) {
        if (anonymousOwnerRepository.findById(id).isEmpty()) {
            AnonymousOwner owner = new AnonymousOwner();
            owner.setId(id);
            owner.setBootstrap(false);
            anonymousOwnerRepository.save(owner);
        }
    }

    private UUID createSessionAs(UUID owner) throws Exception {
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(owner);

        String json = mockMvc.perform(post("/api/chat/sessions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Session for " + owner + "\",\"model\":\"deepseek-v4-flash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(json).get("id").asText());
    }

    @Test
    void ownerBCannotAccessOwnerASession() throws Exception {
        UUID sessionA = createSessionAs(ownerA);

        // Owner B tries to send a message to Owner A's session → should fail
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerB);

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void ownerBCanCreateTheirOwnSession() throws Exception {
        UUID sessionA = createSessionAs(ownerA);

        // Owner B creates their own session
        UUID sessionB = createSessionAs(ownerB);

        // Sessions must be different
        assertThat(sessionA).isNotEqualTo(sessionB);

        // Owner B can send a message to their own session
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerB);

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionB).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello from B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNotEmpty())
                .andExpect(jsonPath("$.blocked").value(false));

        // Owner B's message list for their own session returns data
        mockMvc.perform(get("/api/chat/sessions/{sessionId}/messages", sessionB).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello from B"));
    }

    @Test
    void ownerAStillHasAccessAfterOwnerBInteracts() throws Exception {
        UUID sessionA = createSessionAs(ownerA);

        // Owner B creates their own session and interacts
        UUID sessionB = createSessionAs(ownerB);

        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerB);
        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionB).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"B's message\"}"))
                .andExpect(status().isOk());

        // Switch back to Owner A — their session must still be accessible
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerA);

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionA).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"A's message after B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNotEmpty())
                .andExpect(jsonPath("$.blocked").value(false));
    }
}
