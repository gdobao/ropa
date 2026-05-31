package com.colorinchi.app.controller;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.colorinchi.app.service.CurrentOwnerAccessor;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for HTTP rate limiting on the chat send-message endpoint.
 *
 * <p>Uses the real {@code RateLimitingInterceptor} with a mock
 * {@code CurrentOwnerAccessor} so all requests count against the same owner.
 * The production config sets {@code chat-per-owner.capacity=10}; after 10
 * requests the 11th must return 429 Too Many Requests.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ChatRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentOwnerAccessor currentOwnerAccessor;

    private final UUID sessionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID ownerId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
    }

    @Test
    void rateLimitBlocksAfterTenRequests() throws Exception {
        when(currentOwnerAccessor.getCurrentOwnerId())
                .thenReturn(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        // Send 10 rapid requests — the interceptor allows them through.
        // The controller returns 400 because the session doesn't exist,
        // which is the expected behavior when the interceptor passes.
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"message " + i + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        // The 11th request should be rejected with 429
        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"message 10\"}"))
                .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @Test
    void differentOwnersHaveSeparateRateLimits() throws Exception {
        UUID ownerA = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID ownerB = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        // Owner A sends 10 requests, hitting their limit
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerA);
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"msg\"}"))
                    .andExpect(status().isBadRequest());
        }
        // Owner A's 11th request is blocked
        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"blocked\"}"))
                .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()));

        // Owner B should still be allowed (separate counter)
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerB);
        mockMvc.perform(post("/api/chat/sessions/{sessionId}/messages", sessionId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"owner b first\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void streamEndpointIsRateLimitedOnRealPath() throws Exception {
        UUID runId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        when(currentOwnerAccessor.getCurrentOwnerId())
                .thenReturn(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/chat/stream/{runId}", runId)
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.70");
                                return request;
                            }))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/chat/stream/{runId}", runId)
                        .with(request -> {
                            request.setRemoteAddr("10.0.0.70");
                            return request;
                        }))
                .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @Test
    void feedbackEndpointUsesCurrentMessagePathForRateLimiting() throws Exception {
        UUID messageId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        when(currentOwnerAccessor.getCurrentOwnerId())
                .thenReturn(UUID.fromString("33333333-3333-3333-3333-333333333333"));

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/chat/messages/{messageId}/feedback", messageId).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rating\":\"up\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/chat/messages/{messageId}/feedback", messageId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":\"up\"}"))
                .andExpect(status().is(HttpStatus.TOO_MANY_REQUESTS.value()));
    }
}
