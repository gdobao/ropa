package com.colorinchi.app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colorinchi.app.model.ChatRun;

public interface ChatRunRepository extends JpaRepository<ChatRun, UUID> {

    List<ChatRun> findAllBySessionIdAndOwnerIdOrderByStartedAtDesc(UUID sessionId, UUID ownerId);

    Optional<ChatRun> findByIdAndOwnerId(UUID id, UUID ownerId);

    long countBySessionIdAndOwnerId(UUID sessionId, UUID ownerId);

    @Modifying
    @Query("UPDATE ChatRun r SET r.status = 'streaming' WHERE r.id = :id AND r.ownerId = :ownerId AND r.status = 'running'")
    int markStreaming(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
