package com.colorinchi.app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colorinchi.app.model.ChatRun;

public interface ChatRunRepository extends JpaRepository<ChatRun, UUID> {

    List<ChatRun> findAllBySessionIdAndOwnerIdOrderByStartedAtDesc(UUID sessionId, UUID ownerId);

    Optional<ChatRun> findByIdAndOwnerId(UUID id, UUID ownerId);

    long countBySessionIdAndOwnerId(UUID sessionId, UUID ownerId);
}
