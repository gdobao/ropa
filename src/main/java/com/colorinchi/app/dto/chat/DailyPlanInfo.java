package com.colorinchi.app.dto.chat;

import java.util.List;

public record DailyPlanInfo(
    String dayOfWeek,
    int garmentCount,
    List<String> garmentNames
) {}
