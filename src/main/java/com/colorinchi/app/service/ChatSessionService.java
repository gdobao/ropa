package com.colorinchi.app.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.dto.chat.CreateSessionRequest;
import com.colorinchi.app.model.ChatSurface;
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
        return create(request, ChatSurface.MAIN_CHAT);
    }

    @Transactional
    public ChatSession create(CreateSessionRequest request, ChatSurface surface) {
        ChatSession session = new ChatSession();
        session.setOwnerId(currentOwnerId());
        session.setTitle(request.title() != null ? request.title() : "Nueva conversación");
        session.setModel(request.model() != null ? request.model() : "qwen3.6");
        session.setStatus("active");
        session.setSurface(surface);
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
        return listByOwner(ChatSurface.MAIN_CHAT);
    }

    @Transactional(readOnly = true)
    public List<ChatSession> listByOwner(ChatSurface surface) {
        return chatSessionRepository.findAllByOwnerIdAndSurfaceAndArchivedFalseOrderByUpdatedAtDesc(
                currentOwnerId(), surface);
    }

    @Transactional(readOnly = true)
    public ChatSession getById(UUID id) {
        return getById(ChatSurface.MAIN_CHAT, id);
    }

    @Transactional(readOnly = true)
    public ChatSession getById(ChatSurface surface, UUID id) {
        return chatSessionRepository.findByIdAndOwnerIdAndSurface(id, currentOwnerId(), surface)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));
    }

    @Transactional
    public ChatSession updateTitle(UUID id, String title) {
        return updateTitle(ChatSurface.MAIN_CHAT, id, title);
    }

    @Transactional
    public ChatSession updateTitle(ChatSurface surface, UUID id, String title) {
        ChatSession session = getById(surface, id);
        session.setTitle(title);
        return chatSessionRepository.save(session);
    }

    @Transactional
    public void delete(UUID id) {
        delete(ChatSurface.MAIN_CHAT, id);
    }

    @Transactional
    public void delete(ChatSurface surface, UUID id) {
        if (chatSessionRepository.deleteByIdAndOwnerIdAndSurface(id, currentOwnerId(), surface) == 0) {
            throw new IllegalArgumentException("Chat session not found");
        }
    }

    /**
     * Refresh {@code updatedAt} so retention policies don't archive a session
     * that has recent chat activity (messages or runs).
     */
    @Transactional
    public void touch(UUID sessionId) {
        touch(ChatSurface.MAIN_CHAT, sessionId);
    }

    @Transactional
    public void touch(ChatSurface surface, UUID sessionId) {
        chatSessionRepository.touchSession(sessionId, currentOwnerId(), surface, OffsetDateTime.now());
    }

    @Transactional
    public void touch(ChatSurface surface, UUID sessionId, UUID ownerId) {
        chatSessionRepository.touchSession(sessionId, ownerId, surface, OffsetDateTime.now());
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
