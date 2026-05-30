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

import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.repository.ChatMessageRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository repository;

    @Mock
    private CurrentOwnerAccessor currentOwnerAccessor;

    @Mock
    private ChatAnalyticsService chatAnalyticsService;

    @Mock
    private ChatMetricsService chatMetricsService;

    @InjectMocks
    private ChatMessageService service;

    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID sessionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID messageId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private ChatMessage sampleMessage;

    @BeforeEach
    void setUp() {
        sampleMessage = new ChatMessage();
        sampleMessage.setId(messageId);
        sampleMessage.setSessionId(sessionId);
        sampleMessage.setOwnerId(ownerId);
        sampleMessage.setRole("user");
        sampleMessage.setContent("Hello");
        sampleMessage.setTokens(5);
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
    }

    @Test
    void createSavesMessage() {
        when(repository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage saved = invocation.getArgument(0);
            saved.setId(messageId);
            return saved;
        });

        ChatMessage result = service.create(sessionId, "user", "Hello", 5);

        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getRole()).isEqualTo("user");
        assertThat(result.getContent()).isEqualTo("Hello");
        assertThat(result.getTokens()).isEqualTo(5);
    }

    @Test
    void listBySessionReturnsMessages() {
        when(repository.findAllBySessionIdAndOwnerIdOrderByCreatedAtAsc(sessionId, ownerId))
                .thenReturn(List.of(sampleMessage));

        List<ChatMessage> result = service.listBySession(sessionId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Hello");
    }

    @Test
    void getByIdWhenExistsReturnsMessage() {
        when(repository.findByIdAndSessionIdAndOwnerId(messageId, sessionId, ownerId))
                .thenReturn(Optional.of(sampleMessage));

        ChatMessage result = service.getById(messageId, sessionId);

        assertThat(result).isEqualTo(sampleMessage);
    }

    @Test
    void getByIdWhenNotFoundThrowsIllegalArgument() {
        when(repository.findByIdAndSessionIdAndOwnerId(messageId, sessionId, ownerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(messageId, sessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chat message not found");
    }
}
