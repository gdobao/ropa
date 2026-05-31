package com.colorinchi.app.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colorinchi.app.model.ChatSurface;
import com.colorinchi.app.model.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findAllByOwnerIdAndSurfaceOrderByUpdatedAtDesc(UUID ownerId, ChatSurface surface);

    List<ChatSession> findAllByOwnerIdAndSurfaceAndArchivedFalseOrderByUpdatedAtDesc(UUID ownerId, ChatSurface surface);

    Optional<ChatSession> findByIdAndOwnerIdAndSurface(UUID id, UUID ownerId, ChatSurface surface);

    long deleteByIdAndOwnerIdAndSurface(UUID id, UUID ownerId, ChatSurface surface);

    @Modifying
    @Query("UPDATE ChatSession s SET s.archived = true WHERE s.archived = false AND s.updatedAt < :cutoff")
    int archiveInactiveSessions(@Param("cutoff") OffsetDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatSession s SET s.updatedAt = :now WHERE s.id = :id AND s.ownerId = :ownerId AND s.surface = :surface")
    int touchSession(@Param("id") UUID id, @Param("ownerId") UUID ownerId, @Param("surface") ChatSurface surface, @Param("now") OffsetDateTime now);
}
