package com.colorinchi.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.colorinchi.app.service.analytics.ChatHistoryMetricsService;
import com.colorinchi.app.service.analytics.ChatMetricsService;

@Controller
public class AdminChatMetricsController {

    private final ChatMetricsService chatMetricsService;
    private final ChatHistoryMetricsService chatHistoryMetricsService;

    public AdminChatMetricsController(ChatMetricsService chatMetricsService,
                                      ChatHistoryMetricsService chatHistoryMetricsService) {
        this.chatMetricsService = chatMetricsService;
        this.chatHistoryMetricsService = chatHistoryMetricsService;
    }

    @GetMapping("/admin/chat-metrics")
    String chatMetrics(Model model) {
        model.addAttribute("liveMetrics", chatMetricsService.getSnapshot());
        model.addAttribute("history", chatHistoryMetricsService.getSnapshot(14));
        return "admin-chat-metrics";
    }
}
