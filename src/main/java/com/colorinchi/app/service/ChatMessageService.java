package com.colorinchi.app.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.ChatMessage;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.repository.ChatMessageRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatEventType;
import com.colorinchi.app.service.analytics.ChatMetricsService;
import com.colorinchi.app.service.analytics.LogSanitizer;

@Service
public class ChatMessageService {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final ChatAnalyticsService chatAnalyticsService;
    private final ChatMetricsService chatMetricsService;
    private final ChatSessionService chatSessionService;

    public ChatMessageService(ChatMessageRepository chatMessageRepository,
                              CurrentOwnerAccessor currentOwnerAccessor,
                              ChatAnalyticsService chatAnalyticsService,
                              ChatMetricsService chatMetricsService,
                              @Lazy ChatSessionService chatSessionService) {
        this.chatMessageRepository = chatMessageRepository;
        this.currentOwnerAccessor = currentOwnerAccessor;
        this.chatAnalyticsService = chatAnalyticsService;
        this.chatMetricsService = chatMetricsService;
        this.chatSessionService = chatSessionService;
    }

    @Transactional
    public ChatMessage create(UUID sessionId, String role, String content, int tokens) {
        return create(sessionId, role, content, tokens, ChatSurface.MAIN_CHAT);
    }

    @Transactional
    public ChatMessage create(UUID sessionId, String role, String content, int tokens, ChatSurface surface) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setOwnerId(currentOwnerId());
        message.setRole(role);
        message.setContent(content);
        message.setTokens(tokens);
        ChatMessage saved = chatMessageRepository.save(message);
        chatSessionService.touch(surface, sessionId);
        recordMessageSent(saved);
        return saved;
    }

    /**
     * Create a message with an explicit owner ID — safe for use on non-request
     * threads (e.g. Reactor callbacks) where {@link CurrentOwnerAccessor} has
     * no HTTP request context.
     */
    @Transactional
    public ChatMessage create(UUID sessionId, UUID ownerId, String role, String content, int tokens) {
        return create(sessionId, ownerId, role, content, tokens, ChatSurface.MAIN_CHAT);
    }

    @Transactional
    public ChatMessage create(UUID sessionId, UUID ownerId, String role, String content, int tokens, ChatSurface surface) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setOwnerId(ownerId);
        message.setRole(role);
        message.setContent(content);
        message.setTokens(tokens);
        ChatMessage saved = chatMessageRepository.save(message);
        chatSessionService.touch(surface, sessionId, ownerId);
        recordMessageSent(saved);
        return saved;
    }

    private void recordMessageSent(ChatMessage message) {
        try {
            chatAnalyticsService.recordEvent(
                    message.getOwnerId(),
                    ChatEventType.MESSAGE_SENT,
                    Map.of("messageId", message.getId().toString(),
                           "sessionId", message.getSessionId().toString(),
                           "role", message.getRole(),
                           "tokens", message.getTokens()));
            chatMetricsService.increment(ChatMetricsService.MESSAGES_PROCESSED);
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record message analytics"), e);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> listBySession(UUID sessionId) {
        return chatMessageRepository.findAllBySessionIdAndOwnerIdOrderByCreatedAtAsc(sessionId, currentOwnerId());
    }

    @Transactional(readOnly = true)
    public ChatMessage getById(UUID id, UUID sessionId) {
        return chatMessageRepository.findByIdAndSessionIdAndOwnerId(id, sessionId, currentOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Chat message not found"));
    }

    @Transactional(readOnly = true)
    public ChatMessage getById(UUID id) {
        return chatMessageRepository.findByIdAndOwnerId(id, currentOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Chat message not found"));
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
