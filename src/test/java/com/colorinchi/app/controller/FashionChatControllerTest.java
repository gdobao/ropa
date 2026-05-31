package com.colorinchi.app.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.util.unit.DataSize;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.config.RateLimitingInterceptor;
import com.colorinchi.app.config.UploadProperties;
import com.colorinchi.app.dto.chat.CategoryInfo;
import com.colorinchi.app.dto.chat.ColorInfo;
import com.colorinchi.app.dto.chat.MaterialInfo;
import com.colorinchi.app.dto.chat.WardrobeContext;
import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.service.AnonymousOwnerService;
import com.colorinchi.app.service.ChatMessageService;
import com.colorinchi.app.service.ChatSessionService;
import com.colorinchi.app.service.ModelRouter;
import com.colorinchi.app.service.WardrobeContextAssembler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(FashionChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FashionChatControllerTest.TestConfig.class)
class FashionChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatSessionService chatSessionService;

    @MockitoBean
    private ChatMessageService chatMessageService;

    @MockitoBean
    private ModelRouter modelRouter;

    @MockitoBean
    private WardrobeContextAssembler wardrobeContextAssembler;

    @MockitoBean
    private RateLimitingInterceptor rateLimitingInterceptor;

    @MockitoBean
    private AnonymousOwnerService anonymousOwnerService;

    private UUID sessionId;
    private ChatSession sampleSession;
    private ChatMessage sampleMessage;
    private WardrobeContext sampleContext;
    private AiModelConfig defaultModel;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();

        sampleSession = new ChatSession();
        sampleSession.setId(sessionId);
        sampleSession.setTitle("Test Chat");
        sampleSession.setModel("deepseek-v4-flash");
        sampleSession.setStatus("active");
        sampleSession.setSurface(ChatSurface.MAIN_CHAT);

        sampleMessage = new ChatMessage();
        sampleMessage.setId(UUID.randomUUID());
        sampleMessage.setSessionId(sessionId);
        sampleMessage.setRole("user");
        sampleMessage.setContent("Hola, necesito ayuda con mi look");

        sampleContext = new WardrobeContext(
            10L,
            List.of(new CategoryInfo("Top", 5L)),
            List.of(new ColorInfo("Rojo", "#FF0000", 3L, null)),
            List.of(new MaterialInfo("Algodon", 4L)),
            Map.of("Verano", 6L),
            2L, 3L, 5L, null, List.of(), List.of(), Map.of());

        defaultModel = new AiModelConfig();
        defaultModel.setId("deepseek-v4-flash");
        defaultModel.setName("DeepSeek V4 Flash");
        defaultModel.setDefault(true);

        when(rateLimitingInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(wardrobeContextAssembler.assemble()).thenReturn(sampleContext);
        when(modelRouter.getAvailableModels()).thenReturn(List.of(defaultModel));
        when(modelRouter.findDefault()).thenReturn(defaultModel);
    }

    @Test
    void chatIndexShowsEmptyState() throws Exception {
        when(chatSessionService.listByOwner()).thenReturn(List.of());

        mockMvc.perform(get("/chat").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attributeExists("sessions"))
                .andExpect(model().attributeExists("models"))
                .andExpect(model().attributeExists("wardrobeContext"))
                .andExpect(model().attribute("activeNav", "chat"));
    }

    @Test
    void chatIndexWithSessions() throws Exception {
        when(chatSessionService.listByOwner()).thenReturn(List.of(sampleSession));

        mockMvc.perform(get("/chat").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attributeExists("sessions"))
                .andExpect(model().attribute("activeNav", "chat"));
    }

    @Test
    void chatThreadShowsMessages() throws Exception {
        when(chatSessionService.listByOwner()).thenReturn(List.of(sampleSession));
        when(chatSessionService.getById(sessionId)).thenReturn(sampleSession);
        when(chatMessageService.listBySession(sessionId)).thenReturn(List.of(sampleMessage));

        mockMvc.perform(get("/chat/{sessionId}", sessionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attributeExists("currentSession"))
                .andExpect(model().attributeExists("messages"))
                .andExpect(model().attributeExists("sessions"))
                .andExpect(model().attribute("activeNav", "chat"));
    }

    @Test
    void chatThreadWithoutMessages() throws Exception {
        when(chatSessionService.listByOwner()).thenReturn(List.of(sampleSession));
        when(chatSessionService.getById(sessionId)).thenReturn(sampleSession);
        when(chatMessageService.listBySession(sessionId)).thenReturn(List.of());

        mockMvc.perform(get("/chat/{sessionId}", sessionId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("chat"))
                .andExpect(model().attributeExists("currentSession"))
                .andExpect(model().attribute("messages", List.of()));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        WardrobeContext dummyWardrobeContext() {
            return new WardrobeContext(0L, List.of(), List.of(), List.of(), Map.of(), 0L, 0L, 0L, null, List.of(), List.of(), Map.of());
        }
        @Bean
        UploadProperties uploadProperties() {
            return new UploadProperties(Path.of("/tmp"), DataSize.ofMegabytes(8), List.of("image/jpeg"));
        }
    }
}
