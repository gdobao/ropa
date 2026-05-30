package com.colorinchi.app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colorinchi.app.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findAllBySessionIdAndOwnerIdOrderByCreatedAtAsc(UUID sessionId, UUID ownerId);

    Optional<ChatMessage> findByIdAndSessionIdAndOwnerId(UUID id, UUID sessionId, UUID ownerId);

    long countBySessionIdAndOwnerId(UUID sessionId, UUID ownerId);
}
