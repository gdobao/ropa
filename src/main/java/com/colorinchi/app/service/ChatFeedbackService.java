package com.colorinchi.app.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.dto.chat.ChatFeedbackRequest;
import com.colorinchi.app.model.ChatFeedback;
import com.colorinchi.app.repository.ChatFeedbackRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.LogSanitizer;

@Service
public class ChatFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(ChatFeedbackService.class);

    private final ChatFeedbackRepository chatFeedbackRepository;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final ChatAnalyticsService chatAnalyticsService;

    public ChatFeedbackService(ChatFeedbackRepository chatFeedbackRepository,
                               CurrentOwnerAccessor currentOwnerAccessor,
                               ChatAnalyticsService chatAnalyticsService) {
        this.chatFeedbackRepository = chatFeedbackRepository;
        this.currentOwnerAccessor = currentOwnerAccessor;
        this.chatAnalyticsService = chatAnalyticsService;
    }

    @Transactional
    public ChatFeedback create(UUID runId, UUID sessionId, ChatFeedbackRequest request) {
        ChatFeedback feedback = new ChatFeedback();
        feedback.setRunId(runId);
        feedback.setSessionId(sessionId);
        feedback.setOwnerId(currentOwnerId());
        feedback.setRating(request.rating());
        feedback.setComment(request.comment());
        ChatFeedback saved = chatFeedbackRepository.save(feedback);
        recordFeedback(saved);
        return saved;
    }

    private void recordFeedback(ChatFeedback feedback) {
        try {
            chatAnalyticsService.recordFeedback(
                    feedback.getOwnerId(), feedback.getRunId(), feedback.getRating());
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record feedback analytics"), e);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatFeedback> listByRun(UUID runId) {
        return chatFeedbackRepository.findAllByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<ChatFeedback> listBySession(UUID sessionId) {
        return chatFeedbackRepository.findAllBySessionId(sessionId);
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
