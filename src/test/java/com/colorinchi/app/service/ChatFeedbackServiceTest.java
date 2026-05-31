package com.colorinchi.app.service;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.colorinchi.app.dto.chat.ChatFeedbackRequest;
import com.colorinchi.app.model.ChatFeedback;
import com.colorinchi.app.repository.ChatFeedbackRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatFeedbackServiceTest {

    @Mock
    private ChatFeedbackRepository chatFeedbackRepository;

    @Mock
    private CurrentOwnerAccessor currentOwnerAccessor;

    @Mock
    private ChatAnalyticsService chatAnalyticsService;

    @InjectMocks
    private ChatFeedbackService service;

    @Test
    void createPersistsMessageIdAlongsideRunId() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID messageId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID runId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID sessionId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        ChatFeedbackRequest request = new ChatFeedbackRequest("up", "Useful");

        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
        when(chatFeedbackRepository.save(any(ChatFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatFeedback result = service.create(messageId, runId, sessionId, request);

        ArgumentCaptor<ChatFeedback> feedbackCaptor = ArgumentCaptor.forClass(ChatFeedback.class);
        verify(chatFeedbackRepository).save(feedbackCaptor.capture());
        ChatFeedback saved = feedbackCaptor.getValue();

        assertThat(saved.getMessageId()).isEqualTo(messageId);
        assertThat(saved.getRunId()).isEqualTo(runId);
        assertThat(saved.getSessionId()).isEqualTo(sessionId);
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getRating()).isEqualTo("up");
        assertThat(saved.getComment()).isEqualTo("Useful");
        assertThat(result).isSameAs(saved);

        verify(chatAnalyticsService).recordFeedback(ownerId, runId, "up");
    }

    @Test
    void createWithoutRunIdStillPersistsMessageId() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID messageId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID sessionId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        ChatFeedbackRequest request = new ChatFeedbackRequest("down", "Not helpful");

        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
        when(chatFeedbackRepository.save(any(ChatFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatFeedback result = service.create(messageId, null, sessionId, request);

        assertThat(result.getMessageId()).isEqualTo(messageId);
        assertThat(result.getRunId()).isNull();
        verify(chatAnalyticsService, never()).recordFeedback(any(), any(), any());
    }
}
