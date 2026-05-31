package com.colorinchi.app.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.config.AiModelConfig;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.repository.ChatSessionRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionRepository repository;

    @Mock
    private CurrentOwnerAccessor currentOwnerAccessor;

    @Mock
    private ChatAnalyticsService chatAnalyticsService;

    @Mock
    private ChatMetricsService chatMetricsService;

    @Mock
    private ModelRouter modelRouter;

    @InjectMocks
    private ChatSessionService service;

    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID sessionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private ChatSession sampleSession;

    @BeforeEach
    void setUp() {
        sampleSession = new ChatSession();
        sampleSession.setId(sessionId);
        sampleSession.setOwnerId(ownerId);
        sampleSession.setTitle("Test Chat");
        sampleSession.setModel("gpt-4o");
        sampleSession.setStatus("active");
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
    }

    @Test
    void createWithAllFieldsSavesSession() {
        AiModelConfig gptModel = new AiModelConfig();
        gptModel.setId("gpt-4o");
        when(modelRouter.resolve("gpt-4o")).thenReturn(gptModel);

        CreateSessionRequest request = new CreateSessionRequest("My Chat", "gpt-4o");
        when(repository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession saved = invocation.getArgument(0);
            saved.setId(sessionId);
            return saved;
        });

        ChatSession result = service.create(request);

        assertThat(result.getId()).isEqualTo(sessionId);
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getTitle()).isEqualTo("My Chat");
        assertThat(result.getModel()).isEqualTo("gpt-4o");
        assertThat(result.getStatus()).isEqualTo("active");
        assertThat(result.getSurface()).isEqualTo(ChatSurface.MAIN_CHAT);
    }

    @Test
    void createWithNullFieldsUsesDefaults() {
        AiModelConfig defaultModel = new AiModelConfig();
        defaultModel.setId("qwen3.6");
        when(modelRouter.resolve(isNull())).thenReturn(defaultModel);

        CreateSessionRequest request = new CreateSessionRequest(null, null);
        when(repository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession saved = invocation.getArgument(0);
            saved.setId(sessionId);
            return saved;
        });

        ChatSession result = service.create(request);

        assertThat(result.getTitle()).isEqualTo("Nueva conversación");
        assertThat(result.getModel()).isEqualTo("qwen3.6");
    }

    @Test
    void createWithBlankModelUsesDefaultModel() {
        AiModelConfig defaultModel = new AiModelConfig();
        defaultModel.setId("qwen3.6");
        when(modelRouter.resolve(isNull())).thenReturn(defaultModel);

        CreateSessionRequest request = new CreateSessionRequest("My Chat", "   ");
        when(repository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession result = service.create(request);

        assertThat(result.getModel()).isEqualTo("qwen3.6");
        verify(modelRouter).resolve(null);
    }

    @Test
    void createWithUnsupportedModelThrows() {
        CreateSessionRequest request = new CreateSessionRequest("My Chat", "bad-model");
        when(modelRouter.resolve("bad-model")).thenThrow(new IllegalArgumentException("Modelo no soportado"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Modelo no soportado");
    }

    @Test
    void createCompanionSessionPersistsCompanionSurface() {
        AiModelConfig defaultModel = new AiModelConfig();
        defaultModel.setId("qwen3.6");
        when(modelRouter.resolve(isNull())).thenReturn(defaultModel);

        CreateSessionRequest request = new CreateSessionRequest("Companion", null);
        when(repository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession result = service.create(request, ChatSurface.COMPANION);

        assertThat(result.getSurface()).isEqualTo(ChatSurface.COMPANION);
    }

    @Test
    void listByOwnerReturnsAllSessions() {
        when(repository.findAllByOwnerIdAndSurfaceAndArchivedFalseOrderByUpdatedAtDesc(ownerId, ChatSurface.MAIN_CHAT))
                .thenReturn(List.of(sampleSession));

        List<ChatSession> result = service.listByOwner();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Chat");
    }

    @Test
    void getByIdWhenExistsReturnsSession() {
        when(repository.findByIdAndOwnerIdAndSurface(sessionId, ownerId, ChatSurface.MAIN_CHAT))
                .thenReturn(Optional.of(sampleSession));

        ChatSession result = service.getById(sessionId);

        assertThat(result).isEqualTo(sampleSession);
    }

    @Test
    void getByIdWhenNotFoundThrowsIllegalArgument() {
        when(repository.findByIdAndOwnerIdAndSurface(sessionId, ownerId, ChatSurface.MAIN_CHAT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chat session not found");
    }

    @Test
    void getByIdUsesRequestedSurface() {
        when(repository.findByIdAndOwnerIdAndSurface(sessionId, ownerId, ChatSurface.COMPANION))
                .thenReturn(Optional.of(sampleSession));

        ChatSession result = service.getById(ChatSurface.COMPANION, sessionId);

        assertThat(result).isEqualTo(sampleSession);
    }

    @Test
    void updateTitleModifiesAndSaves() {
        when(repository.findByIdAndOwnerIdAndSurface(sessionId, ownerId, ChatSurface.MAIN_CHAT))
                .thenReturn(Optional.of(sampleSession));
        when(repository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession result = service.updateTitle(sessionId, "Updated Title");

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(repository).save(sampleSession);
    }

    @Test
    void deleteCallsRepositoryDeleteByIdAndOwnerId() {
        when(repository.deleteByIdAndOwnerIdAndSurface(sessionId, ownerId, ChatSurface.MAIN_CHAT)).thenReturn(1L);

        service.delete(sessionId);

        verify(repository).deleteByIdAndOwnerIdAndSurface(sessionId, ownerId, ChatSurface.MAIN_CHAT);
    }

    @Test
    void deleteThrowsWhenSessionBelongsToAnotherOwner() {
        when(repository.deleteByIdAndOwnerIdAndSurface(sessionId, ownerId, ChatSurface.MAIN_CHAT)).thenReturn(0L);

        assertThatThrownBy(() -> service.delete(sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chat session not found");
    }

    @Test
    void touchOnlyTouchesOwnedMainChatSession() {
        service.touch(sessionId);

        verify(repository).touchSession(eq(sessionId), eq(ownerId), eq(ChatSurface.MAIN_CHAT), any(OffsetDateTime.class));
    }

    @Test
    void touchCompanionSessionPassesCompanionSurface() {
        service.touch(ChatSurface.COMPANION, sessionId);

        verify(repository).touchSession(eq(sessionId), eq(ownerId), eq(ChatSurface.COMPANION), any(OffsetDateTime.class));
    }

    @Test
    void touchDoesNotBypassSurfaceOrOwnerChecks() {
        service.touch(sessionId);

        verify(repository).touchSession(eq(sessionId), eq(ownerId), eq(ChatSurface.MAIN_CHAT), any(OffsetDateTime.class));
    }
}
