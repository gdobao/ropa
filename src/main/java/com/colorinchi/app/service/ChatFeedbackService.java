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
    private static final int MAX_COMMENT_LENGTH = 1_000;

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
    public ChatFeedback create(UUID messageId, UUID runId, UUID sessionId, ChatFeedbackRequest request) {
        String rating = validateRating(request);
        String comment = sanitizeComment(request == null ? null : request.comment());

        ChatFeedback feedback = new ChatFeedback();
        feedback.setMessageId(messageId);
        feedback.setRunId(runId);
        feedback.setSessionId(sessionId);
        feedback.setOwnerId(currentOwnerId());
        feedback.setRating(rating);
        feedback.setComment(comment);
        ChatFeedback saved = chatFeedbackRepository.save(feedback);
        recordFeedback(saved);
        return saved;
    }

    private String validateRating(ChatFeedbackRequest request) {
        if (request == null || request.rating() == null || request.rating().isBlank()) {
            throw new IllegalArgumentException("Rating is required");
        }
        String rating = request.rating().trim().toLowerCase(java.util.Locale.ROOT);
        if (!"up".equals(rating) && !"down".equals(rating)) {
            throw new IllegalArgumentException("Rating must be 'up' or 'down'");
        }
        return rating;
    }

    private String sanitizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String sanitized = comment.trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        if (sanitized.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comment is too long");
        }
        return sanitized;
    }

    private void recordFeedback(ChatFeedback feedback) {
        if (feedback.getRunId() == null) return;
        try {
            chatAnalyticsService.recordFeedback(
                    feedback.getOwnerId(), feedback.getRunId(), feedback.getRating());
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record feedback analytics"), e);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatFeedback> listByRun(UUID runId) {
        return chatFeedbackRepository.findAllByRunIdAndOwnerId(runId, currentOwnerId());
    }

    @Transactional(readOnly = true)
    public List<ChatFeedback> listBySession(UUID sessionId) {
        return chatFeedbackRepository.findAllBySessionIdAndOwnerId(sessionId, currentOwnerId());
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
