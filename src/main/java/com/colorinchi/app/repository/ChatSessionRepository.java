package com.colorinchi.app.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colorinchi.app.model.ChatSession;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    Optional<ChatSession> findByIdAndOwnerId(UUID id, UUID ownerId);

    long deleteByIdAndOwnerId(UUID id, UUID ownerId);
}
