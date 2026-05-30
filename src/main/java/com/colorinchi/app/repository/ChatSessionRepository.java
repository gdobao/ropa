package com.colorinchi.app.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colorinchi.app.model.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    List<ChatSession> findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(UUID ownerId);

    Optional<ChatSession> findByIdAndOwnerId(UUID id, UUID ownerId);

    long deleteByIdAndOwnerId(UUID id, UUID ownerId);

    @Modifying
    @Query("UPDATE ChatSession s SET s.archived = true WHERE s.archived = false AND s.updatedAt < :cutoff")
    int archiveInactiveSessions(@Param("cutoff") OffsetDateTime cutoff);
}
