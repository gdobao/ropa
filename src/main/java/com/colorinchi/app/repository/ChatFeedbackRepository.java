package com.colorinchi.app.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colorinchi.app.model.ChatFeedback;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, UUID> {

    List<ChatFeedback> findAllByRunId(UUID runId);

    List<ChatFeedback> findAllBySessionId(UUID sessionId);
}
