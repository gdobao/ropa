package com.colorinchi.app.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.ChatAnalyticsEvent;
import com.colorinchi.app.model.ChatSession;
import com.colorinchi.app.repository.ChatAnalyticsEventRepository;
import com.colorinchi.app.repository.ChatSessionRepository;
import com.colorinchi.app.service.analytics.ChatEventType;
import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.repository.AnonymousOwnerRepository;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link ChatDataRetentionService}.
 *
 * <p>Uses native SQL to insert test data with exact timestamps, bypassing
 * JPA lifecycle callbacks that would overwrite {@code createdAt} and
 * {@code updatedAt}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatDataRetentionIntegrationTest {

    @Autowired
    private ChatDataRetentionService retentionService;

    @Autowired
    private ChatAnalyticsEventRepository analyticsEventRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private AnonymousOwnerRepository anonymousOwnerRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private CurrentOwnerAccessor currentOwnerAccessor;

    private final UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        // Ensure the owner exists in the DB for FK constraints
        if (anonymousOwnerRepository.findById(ownerId).isEmpty()) {
            AnonymousOwner owner = new AnonymousOwner();
            owner.setId(ownerId);
            owner.setBootstrap(false);
            anonymousOwnerRepository.saveAndFlush(owner);
        }
    }

    @Test
    void retentionCleanupDeletesOldAnalyticsEvents() {
        // Insert an analytics event with createdAt = 100 days ago (via native SQL)
        UUID oldEventId = UUID.randomUUID();
        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(100);
        insertAnalyticsEventNative(oldEventId, ownerId, ChatEventType.SESSION_CREATED.name(),
                "{\"test\":true}", oldDate);

        // Insert an event with today's date (should be kept)
        UUID recentEventId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        insertAnalyticsEventNative(recentEventId, ownerId, ChatEventType.MESSAGE_SENT.name(),
                "{\"test\":true}", now);

        entityManager.flush();
        entityManager.clear();

        // Verify both exist before cleanup
        assertThat(analyticsEventRepository.findById(oldEventId)).isPresent();
        assertThat(analyticsEventRepository.findById(recentEventId)).isPresent();

        // Run retention cleanup
        retentionService.runRetention();

        // The event_data column uses @JdbcTypeCode(SqlTypes.JSON) which may cause
        // issues with H2. Flush + clear to ensure the DELETE was committed.
        entityManager.flush();
        entityManager.clear();

        // Old event should be deleted (100 days > 90 day retention)
        assertThat(analyticsEventRepository.findById(oldEventId)).isNotPresent();

        // Recent event should remain
        assertThat(analyticsEventRepository.findById(recentEventId)).isPresent();
    }

    @Test
    void retentionCleanupArchivesOldSessions() {
        // Insert a session with updatedAt = 200 days ago (via native SQL)
        UUID oldSessionId = UUID.randomUUID();
        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(200);
        insertSessionNative(oldSessionId, ownerId, "Old Session", "gpt-4o", oldDate);

        // Insert a recently active session (updatedAt = now, should NOT be archived)
        UUID recentSessionId = UUID.randomUUID();
        insertSessionNative(recentSessionId, ownerId, "Recent Session", "gpt-4o", OffsetDateTime.now());

        entityManager.flush();
        entityManager.clear();

        // Verify neither is archived yet
        ChatSession beforeOld = chatSessionRepository.findById(oldSessionId).orElseThrow();
        assertThat(beforeOld.isArchived()).isFalse();

        ChatSession beforeRecent = chatSessionRepository.findById(recentSessionId).orElseThrow();
        assertThat(beforeRecent.isArchived()).isFalse();

        // Run retention cleanup
        retentionService.runRetention();

        entityManager.flush();
        entityManager.clear();

        // Old session should now be archived (200 days > 180 day inactivity threshold)
        ChatSession afterOld = chatSessionRepository.findById(oldSessionId).orElseThrow();
        assertThat(afterOld.isArchived()).isTrue();

        // Recent session should still NOT be archived
        ChatSession afterRecent = chatSessionRepository.findById(recentSessionId).orElseThrow();
        assertThat(afterRecent.isArchived()).isFalse();
    }

    @Test
    void retentionCleanupHandlesEmptyDatabase() {
        retentionService.runRetention();
    }

    // --- Native SQL helpers to bypass @PrePersist ---

    private void insertAnalyticsEventNative(UUID id, UUID ownerId, String eventType,
                                             String eventData, OffsetDateTime createdAt) {
        entityManager.createNativeQuery(
                "INSERT INTO chat_analytics_events (id, owner_id, event_type, event_data, created_at) " +
                "VALUES (?, ?, ?, ?, ?)")
                .setParameter(1, id)
                .setParameter(2, ownerId)
                .setParameter(3, eventType)
                .setParameter(4, eventData)
                .setParameter(5, createdAt)
                .executeUpdate();
    }

    private void insertSessionNative(UUID id, UUID ownerId, String title, String model,
                                     OffsetDateTime updatedAt) {
        OffsetDateTime now = OffsetDateTime.now();
        entityManager.createNativeQuery(
                "INSERT INTO chat_sessions (id, owner_id, title, model, status, created_at, updated_at, archived) " +
                "VALUES (?, ?, ?, ?, 'active', ?, ?, false)")
                .setParameter(1, id)
                .setParameter(2, ownerId)
                .setParameter(3, title)
                .setParameter(4, model)
                .setParameter(5, now)
                .setParameter(6, updatedAt)
                .executeUpdate();
    }
}
