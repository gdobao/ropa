package com.colorinchi.app.dto;

public record WeeklyPlanItem(
        Long planId,
        Long garmentId,
        String garmentName,
        String imageUrl
) {
}
