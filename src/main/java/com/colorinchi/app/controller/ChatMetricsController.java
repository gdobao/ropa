package com.colorinchi.app.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public ChatMetricsController(ChatMetricsService chatMetricsService) {
        this.chatMetricsService = chatMetricsService;
    }

    @GetMapping("/api/admin/metrics")
    public Map<String, Long> getMetrics() {
        return chatMetricsService.getSnapshot();
    }
}
