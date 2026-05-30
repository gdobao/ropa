package com.colorinchi.app.service;

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
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.repository.ChatSessionRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    }

    @Test
    void createWithNullFieldsUsesDefaults() {
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
    void listByOwnerReturnsAllSessions() {
        when(repository.findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(ownerId)).thenReturn(List.of(sampleSession));

        List<ChatSession> result = service.listByOwner();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Chat");
    }

    @Test
    void getByIdWhenExistsReturnsSession() {
        when(repository.findByIdAndOwnerId(sessionId, ownerId)).thenReturn(Optional.of(sampleSession));

        ChatSession result = service.getById(sessionId);

        assertThat(result).isEqualTo(sampleSession);
    }

    @Test
    void getByIdWhenNotFoundThrowsIllegalArgument() {
        when(repository.findByIdAndOwnerId(sessionId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chat session not found");
    }

    @Test
    void updateTitleModifiesAndSaves() {
        when(repository.findByIdAndOwnerId(sessionId, ownerId)).thenReturn(Optional.of(sampleSession));
        when(repository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatSession result = service.updateTitle(sessionId, "Updated Title");

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        verify(repository).save(sampleSession);
    }

    @Test
    void deleteCallsRepositoryDeleteByIdAndOwnerId() {
        when(repository.deleteByIdAndOwnerId(sessionId, ownerId)).thenReturn(1L);

        service.delete(sessionId);

        verify(repository).deleteByIdAndOwnerId(sessionId, ownerId);
    }

    @Test
    void deleteThrowsWhenSessionBelongsToAnotherOwner() {
        when(repository.deleteByIdAndOwnerId(sessionId, ownerId)).thenReturn(0L);

        assertThatThrownBy(() -> service.delete(sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chat session not found");
    }
}
