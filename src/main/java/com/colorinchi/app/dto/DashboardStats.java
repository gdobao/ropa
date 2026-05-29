package com.colorinchi.app.dto;

import java.util.List;

public record DashboardStats(
    long totalGarments,
    long favoriteCount,
    int usagePercent,
    long plannedDays,
    long plannedItems,
    List<DashboardStats.CategoryCount> categoryBreakdown
) {
    public record CategoryCount(String category, long count) {}
}
