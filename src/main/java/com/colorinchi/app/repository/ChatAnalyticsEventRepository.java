package com.colorinchi.app.repository;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colorinchi.app.model.ChatAnalyticsEvent;

public interface ChatAnalyticsEventRepository extends JpaRepository<ChatAnalyticsEvent, UUID> {

    @Modifying
    @Query("DELETE FROM ChatAnalyticsEvent e WHERE e.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
