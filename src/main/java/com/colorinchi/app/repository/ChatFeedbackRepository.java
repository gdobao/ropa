package com.colorinchi.app.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colorinchi.app.model.ChatFeedback;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, UUID> {

    List<ChatFeedback> findAllByMessageIdAndOwnerId(UUID messageId, UUID ownerId);

    List<ChatFeedback> findAllByRunIdAndOwnerId(UUID runId, UUID ownerId);

    List<ChatFeedback> findAllBySessionIdAndOwnerId(UUID sessionId, UUID ownerId);
}
