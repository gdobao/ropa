package com.colorinchi.app.dto.chat;

import java.util.List;
import java.util.Map;

public record WardrobeContext(
    long totalGarments,
    List<CategoryInfo> categories,
    List<ColorInfo> colors,
    List<MaterialInfo> materials,
    Map<String, Long> seasons,
    long favoritesCount,
    long plannedDays,
    long plannedItems,
    DailyPlanInfo todayPlan,
    List<DailyPlanInfo> upcomingPlans,
    List<GarmentSummary> garments
) {}
