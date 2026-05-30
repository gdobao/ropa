package com.colorinchi.app.dto;

import java.util.List;

public record DashboardStats(
    long totalGarments,
    long favoriteCount,
    int usagePercent,
    long plannedDays,
    long plannedItems,
    long plannedCoveragePercent,
    long nonFavoriteCount,
    List<DashboardStats.CategoryCount> categoryBreakdown
) {
    public record CategoryCount(String category, long count) {}
}
