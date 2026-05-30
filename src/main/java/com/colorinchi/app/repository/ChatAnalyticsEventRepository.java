package com.colorinchi.app.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.colorinchi.app.model.ChatAnalyticsEvent;

public interface ChatAnalyticsEventRepository extends JpaRepository<ChatAnalyticsEvent, UUID> {
}
