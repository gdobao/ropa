package com.colorinchi.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.config.ChatRetentionProperties;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.repository.ChatAnalyticsEventRepository;
import com.colorinchi.app.repository.ChatSessionRepository;
import com.colorinchi.app.repository.GarmentRepository;

/**
 * Scheduled service for data retention and cleanup of stale chat data.
 *
 * <p>Runs daily at 3am and performs three cleanup operations:
 * <ol>
 *   <li>Deletes analytics events older than the configured threshold</li>
 *   <li>Archives (soft-deletes) sessions that have been inactive beyond the threshold</li>
 *   <li>Optionally scans the uploads directory for orphaned files</li>
 * </ol>
 */
@Service
public class ChatDataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(ChatDataRetentionService.class);

    private final ChatAnalyticsEventRepository analyticsEventRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final GarmentRepository garmentRepository;
    private final Path uploadDirectory;

    private final int analyticsEventsDays;
    private final int sessionInactiveDays;
    private final boolean orphanUploadCleanup;

    public ChatDataRetentionService(
            ChatAnalyticsEventRepository analyticsEventRepository,
            ChatSessionRepository chatSessionRepository,
            GarmentRepository garmentRepository,
            ChatRetentionProperties retentionProperties) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.garmentRepository = garmentRepository;
        this.uploadDirectory = retentionProperties.uploadDirectory();
        this.analyticsEventsDays = retentionProperties.analyticsEventsDays();
        this.sessionInactiveDays = retentionProperties.sessionInactiveDays();
        this.orphanUploadCleanup = retentionProperties.orphanUploadCleanup();
    }

    /**
     * Runs the full data retention cycle every day at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void runRetention() {
        log.info("Starting daily data retention cycle");

        deleteOldAnalyticsEvents();
        archiveInactiveSessions();
        cleanupOrphanUploads();

        log.info("Daily data retention cycle completed");
    }

    // ---------------------------------------------------------------
    // Analytics event cleanup
    // ---------------------------------------------------------------

    void deleteOldAnalyticsEvents() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(analyticsEventsDays);
        int deleted = analyticsEventRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} analytics events older than {}", deleted, cutoff);
        } else {
            log.debug("No analytics events to delete (cutoff: {})", cutoff);
        }
    }

    // ---------------------------------------------------------------
    // Session archiving
    // ---------------------------------------------------------------

    void archiveInactiveSessions() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(sessionInactiveDays);
        int archived = chatSessionRepository.archiveInactiveSessions(cutoff);
        if (archived > 0) {
            log.info("Archived {} inactive sessions older than {}", archived, cutoff);
        } else {
            log.debug("No inactive sessions to archive (cutoff: {})", cutoff);
        }
    }

    // ---------------------------------------------------------------
    // Orphan upload cleanup
    // ---------------------------------------------------------------

    void cleanupOrphanUploads() {
        if (!Files.isDirectory(uploadDirectory)) {
            log.debug("Upload directory does not exist: {}", uploadDirectory);
            return;
        }

        Set<String> referencedFiles = garmentRepository.findAll().stream()
                .map(Garment::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .map(url -> {
                    // Extract filename from URL (e.g. /uploads/photo.jpg -> photo.jpg)
                    int lastSlash = url.lastIndexOf('/');
                    return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
                })
                .collect(Collectors.toSet());

        try (Stream<Path> files = Files.list(uploadDirectory)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> !referencedFiles.contains(path.getFileName().toString()))
                    .forEach(path -> {
                        log.warn("Orphaned upload file: {} (not referenced by any garment)", path);
                        if (orphanUploadCleanup) {
                            try {
                                Files.delete(path);
                                log.info("Deleted orphaned upload file: {}", path);
                            } catch (IOException e) {
                                log.warn("Failed to delete orphaned file: {}", path, e);
                            }
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to list upload directory: {}", uploadDirectory, e);
        }
    }
}
