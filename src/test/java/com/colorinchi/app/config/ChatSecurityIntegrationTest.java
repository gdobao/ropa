package com.colorinchi.app.config;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void chatMessagePostRequiresCsrfToken() throws Exception {
        UUID sessionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"hola\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatStreamGetDoesNotRequireCsrfToken() throws Exception {
        UUID runId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        mockMvc.perform(get("/api/chat/stream/{runId}", runId))
                .andExpect(status().isOk());
    }

    @Test
    void companionMessagePostRequiresCsrfToken() throws Exception {
        UUID sessionId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        mockMvc.perform(post("/api/companion/sessions/{sessionId}/messages", sessionId)
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"hola\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void companionStreamGetDoesNotRequireCsrfToken() throws Exception {
        UUID runId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        mockMvc.perform(get("/api/companion/stream/{runId}", runId))
                .andExpect(status().isOk());
    }

    @Test
    void adminApiRequiresAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApiAllowsConfiguredAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/metrics")
                        .header("X-Admin-Token", "test-admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void adminViewRequiresAdminToken() throws Exception {
        mockMvc.perform(get("/admin/chat-metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApiRejectsWrongAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/metrics")
                        .header("X-Admin-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void companionSessionCreateRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/companion/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatSessionCreateRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/chat/sessions")
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"Test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void garmentDeleteRequiresCsrfToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/wardrobe/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void weeklyPlanAssignRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/weekly-plan/assign")
                        .param("garmentId", "1")
                        .param("dayOfWeek", "Lunes"))
                .andExpect(status().isForbidden());
    }
}
