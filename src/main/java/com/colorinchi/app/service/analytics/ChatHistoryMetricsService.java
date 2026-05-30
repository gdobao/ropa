package com.colorinchi.app.service.analytics;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.repository.ChatAnalyticsEventRepository;

@Service
public class ChatHistoryMetricsService {

    private final ChatAnalyticsEventRepository eventRepository;

    public ChatHistoryMetricsService(ChatAnalyticsEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public ChatHistorySnapshot getSnapshot(int days) {
        int boundedDays = Math.max(1, Math.min(days, 90));
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(boundedDays);
        List<Object[]> rows = eventRepository.aggregateByDayAndType(cutoff);

        Map<String, Long> totalsByType = new LinkedHashMap<>();
        Map<LocalDate, Long> eventsByDay = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            String eventType = String.valueOf(row[1]);
            long total = ((Number) row[2]).longValue();
            totalsByType.merge(eventType, total, Long::sum);
            eventsByDay.merge(day, total, Long::sum);
        }
        long maxDailyEvents = eventsByDay.values().stream().mapToLong(Long::longValue).max().orElse(1);
        return new ChatHistorySnapshot(boundedDays, totalsByType, eventsByDay, maxDailyEvents);
    }

    public record ChatHistorySnapshot(int days, Map<String, Long> totalsByType, Map<LocalDate, Long> eventsByDay, long maxDailyEvents) {}
}
