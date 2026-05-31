package com.colorinchi.app.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.ChatRun;
import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.repository.ChatRunRepository;
import com.colorinchi.app.service.analytics.ChatAnalyticsService;
import com.colorinchi.app.service.analytics.ChatEventType;
import com.colorinchi.app.service.analytics.ChatMetricsService;
import com.colorinchi.app.service.analytics.LogSanitizer;

@Service
public class ChatRunService {

    private static final Logger log = LoggerFactory.getLogger(ChatRunService.class);

    private final ChatRunRepository chatRunRepository;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final ChatAnalyticsService chatAnalyticsService;
    private final ChatMetricsService chatMetricsService;
    private final ChatSessionService chatSessionService;

    public ChatRunService(ChatRunRepository chatRunRepository,
                           CurrentOwnerAccessor currentOwnerAccessor,
                           ChatAnalyticsService chatAnalyticsService,
                           ChatMetricsService chatMetricsService,
                           @Lazy ChatSessionService chatSessionService) {
        this.chatRunRepository = chatRunRepository;
        this.currentOwnerAccessor = currentOwnerAccessor;
        this.chatAnalyticsService = chatAnalyticsService;
        this.chatMetricsService = chatMetricsService;
        this.chatSessionService = chatSessionService;
    }

    @Transactional
    public ChatRun create(UUID sessionId, String modelRequested) {
        ChatRun run = new ChatRun();
        run.setSessionId(sessionId);
        run.setOwnerId(currentOwnerId());
        run.setModelRequested(modelRequested);
        run.setStatus("pending");
        return chatRunRepository.save(run);
    }

    @Transactional
    public ChatRun started(UUID id) {
        ChatRun run = getById(id);
        run.setStatus("running");
        run.setStartedAt(OffsetDateTime.now());
        ChatRun saved = chatRunRepository.save(run);
        recordRunStarted(saved);
        return saved;
    }

    @Transactional
    public boolean markStreaming(UUID id, UUID ownerId) {
        return chatRunRepository.markStreaming(id, ownerId) > 0;
    }

    @Transactional
    public ChatRun complete(UUID id, String modelResolved, int totalTokens) {
        return complete(id, modelResolved, totalTokens, ChatSurface.MAIN_CHAT);
    }

    @Transactional
    public ChatRun complete(UUID id, String modelResolved, int totalTokens, ChatSurface surface) {
        ChatRun run = getById(id);
        String s = run.getStatus();
        if (!"running".equals(s) && !"streaming".equals(s)) {
            throw new IllegalStateException("Run " + id + " is not in running/streaming state");
        }
        run.setStatus("completed");
        run.setModelResolved(modelResolved);
        run.setTotalTokens(totalTokens);
        run.setCompletedAt(OffsetDateTime.now());
        ChatRun saved = chatRunRepository.save(run);
        chatSessionService.touch(surface, run.getSessionId());
        recordRunCompleted(saved);
        return saved;
    }

    @Transactional
    public ChatRun fail(UUID id, String errorMessage) {
        return fail(id, errorMessage, ChatSurface.MAIN_CHAT);
    }

    @Transactional
    public ChatRun fail(UUID id, String errorMessage, ChatSurface surface) {
        ChatRun run = getById(id);
        String s = run.getStatus();
        if (!"running".equals(s) && !"streaming".equals(s)) {
            throw new IllegalStateException("Run " + id + " is not in running/streaming state");
        }
        run.setStatus("failed");
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(OffsetDateTime.now());
        ChatRun saved = chatRunRepository.save(run);
        chatSessionService.touch(surface, run.getSessionId());
        recordRunFailed(saved);
        return saved;
    }

    /**
     * Complete a run with an explicit owner ID — safe for use on non-request
     * threads (e.g. Reactor callbacks) where {@link CurrentOwnerAccessor} has
     * no HTTP request context.
     */
    @Transactional
    public ChatRun complete(UUID id, UUID ownerId, String modelResolved, int totalTokens) {
        return complete(id, ownerId, modelResolved, totalTokens, ChatSurface.MAIN_CHAT);
    }

    @Transactional
    public ChatRun complete(UUID id, UUID ownerId, String modelResolved, int totalTokens, ChatSurface surface) {
        ChatRun run = chatRunRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Chat run not found"));
        String s = run.getStatus();
        if (!"running".equals(s) && !"streaming".equals(s)) {
            throw new IllegalStateException("Run " + id + " is not in running state");
        }
        run.setStatus("completed");
        run.setModelResolved(modelResolved);
        run.setTotalTokens(totalTokens);
        run.setCompletedAt(OffsetDateTime.now());
        ChatRun saved = chatRunRepository.save(run);
        chatSessionService.touch(surface, run.getSessionId(), ownerId);
        recordRunCompleted(saved);
        return saved;
    }

    /**
     * Fail a run with an explicit owner ID — safe for use on non-request
     * threads (e.g. Reactor callbacks) where {@link CurrentOwnerAccessor} has
     * no HTTP request context.
     */
    @Transactional
    public ChatRun fail(UUID id, UUID ownerId, String errorMessage) {
        return fail(id, ownerId, errorMessage, ChatSurface.MAIN_CHAT);
    }

    @Transactional
    public ChatRun fail(UUID id, UUID ownerId, String errorMessage, ChatSurface surface) {
        ChatRun run = chatRunRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Chat run not found"));
        String s = run.getStatus();
        if (!"running".equals(s) && !"streaming".equals(s)) {
            throw new IllegalStateException("Run " + id + " is not in running state");
        }
        run.setStatus("failed");
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(OffsetDateTime.now());
        ChatRun saved = chatRunRepository.save(run);
        chatSessionService.touch(surface, run.getSessionId(), ownerId);
        recordRunFailed(saved);
        return saved;
    }

    private void recordRunStarted(ChatRun run) {
        try {
            chatAnalyticsService.recordEvent(
                    run.getOwnerId(),
                    ChatEventType.RUN_STARTED,
                    Map.of("runId", run.getId().toString(),
                           "sessionId", run.getSessionId().toString(),
                           "modelRequested", run.getModelRequested() != null ? run.getModelRequested() : ""));
            chatMetricsService.increment(ChatMetricsService.RUNS_STARTED);
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record run-started analytics"), e);
        }
    }

    private void recordRunCompleted(ChatRun run) {
        try {
            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("runId", run.getId().toString());
            data.put("sessionId", run.getSessionId().toString());
            data.put("modelResolved", run.getModelResolved() != null ? run.getModelResolved() : "");
            data.put("totalTokens", run.getTotalTokens());
            if (run.getStartedAt() != null) {
                long latencyMs = java.time.Duration.between(run.getStartedAt(), run.getCompletedAt()).toMillis();
                data.put("latencyMs", latencyMs);
                chatMetricsService.recordLatency(latencyMs);
            }
            chatAnalyticsService.recordEvent(
                    run.getOwnerId(), ChatEventType.RUN_COMPLETED, data);
            chatMetricsService.increment(ChatMetricsService.RUNS_COMPLETED);
            chatMetricsService.recordTokens(run.getTotalTokens());
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record run-completed analytics"), e);
        }
    }

    private void recordRunFailed(ChatRun run) {
        try {
            String errorType = run.getErrorMessage() != null
                    ? run.getErrorMessage().replaceAll("[^a-zA-Z0-9_]", "_").substring(0,
                          Math.min(run.getErrorMessage().length(), 50))
                    : "unknown";
            chatAnalyticsService.recordEvent(
                    run.getOwnerId(),
                    ChatEventType.RUN_FAILED,
                    Map.of("runId", run.getId().toString(),
                           "sessionId", run.getSessionId().toString(),
                           "errorType", errorType));
            chatMetricsService.increment(ChatMetricsService.RUNS_FAILED);
            chatMetricsService.increment(ChatMetricsService.ERRORS_GENERIC);
        } catch (Exception e) {
            log.warn(LogSanitizer.sanitize("Failed to record run-failed analytics"), e);
        }
    }

    @Transactional(readOnly = true)
    public ChatRun getById(UUID id) {
        return chatRunRepository.findByIdAndOwnerId(id, currentOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Chat run not found"));
    }

    @Transactional(readOnly = true)
    public List<ChatRun> listBySession(UUID sessionId) {
        return chatRunRepository.findAllBySessionIdAndOwnerIdOrderByStartedAtDesc(sessionId, currentOwnerId());
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
