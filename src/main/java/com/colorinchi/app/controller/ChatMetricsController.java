package com.colorinchi.app.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.colorinchi.app.service.analytics.ChatHistoryMetricsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;

/**
 * Exposes operational chat metrics at {@code GET /api/admin/metrics}.
 *
 * <p>No authentication is required (the app uses {@code permitAll()}
 * for all requests).
 */
@RestController
public class ChatMetricsController {

    private final ChatMetricsService chatMetricsService;
    private final ChatHistoryMetricsService chatHistoryMetricsService;

    public ChatMetricsController(ChatMetricsService chatMetricsService,
                                 ChatHistoryMetricsService chatHistoryMetricsService) {
        this.chatMetricsService = chatMetricsService;
        this.chatHistoryMetricsService = chatHistoryMetricsService;
    }

    @GetMapping("/api/admin/metrics")
    public Map<String, Long> getMetrics() {
        return chatMetricsService.getSnapshot();
    }

    @GetMapping("/api/admin/metrics/history")
    public ChatHistoryMetricsService.ChatHistorySnapshot getHistory(@RequestParam(defaultValue = "14") int days) {
        return chatHistoryMetricsService.getSnapshot(days);
    }

}
