package com.colorinchi.app.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.repository.ChatSessionRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatEventType;
import com.colorinchi.app.service.analytics.ChatMetricsService;
import com.colorinchi.app.service.analytics.LogSanitizer;

@Service
public class ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private final ChatSessionRepository chatSessionRepository;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final ChatAnalyticsService chatAnalyticsService;
    private final ChatMetricsService chatMetricsService;

    public ChatSessionService(ChatSessionRepository chatSessionRepository,
                              CurrentOwnerAccessor currentOwnerAccessor,
                              ChatAnalyticsService chatAnalyticsService,
                              ChatMetricsService chatMetricsService) {
        this.chatSessionRepository = chatSessionRepository;
        this.currentOwnerAccessor = currentOwnerAccessor;
        this.chatAnalyticsService = chatAnalyticsService;
        this.chatMetricsService = chatMetricsService;
    }

    @Transactional
    public ChatSession create(CreateSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setOwnerId(currentOwnerId());
        session.setTitle(request.title() != null ? request.title() : "Nueva conversación");
        session.setModel(request.model() != null ? request.model() : "qwen3.6");
        session.setStatus("active");
        ChatSession saved = chatSessionRepository.save(session);
        recordSessionCreated(saved);
        return saved;
    }

    private void recordSessionCreated(ChatSession session) {
        try {
            chatAnalyticsService.recordEvent(
                    session.getOwnerId(),
                    ChatEventType.SESSION_CREATED,
                    Map.of("sessionId", session.getId().toString(),
                           "model", session.getModel() != null ? session.getModel() : ""));
            chatMetricsService.increment(ChatMetricsService.SESSIONS_CREATED);
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record session analytics"), e);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatSession> listByOwner() {
        return chatSessionRepository.findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public ChatSession getById(UUID id) {
        return chatSessionRepository.findByIdAndOwnerId(id, currentOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));
    }

    @Transactional
    public ChatSession updateTitle(UUID id, String title) {
        ChatSession session = getById(id);
        session.setTitle(title);
        return chatSessionRepository.save(session);
    }

    @Transactional
    public void delete(UUID id) {
        if (chatSessionRepository.deleteByIdAndOwnerId(id, currentOwnerId()) == 0) {
            throw new IllegalArgumentException("Chat session not found");
        }
    }

    /**
     * Refresh {@code updatedAt} so retention policies don't archive a session
     * that has recent chat activity (messages or runs).
     */
    @Transactional
    public void touch(UUID sessionId) {
        chatSessionRepository.findById(sessionId).ifPresent(session -> {
            // @PreUpdate on the entity will set updatedAt to now on save
            chatSessionRepository.save(session);
        });
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
