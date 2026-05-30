package com.colorinchi.app.dto.chat;

public record ChatFeedbackRequest(
    String rating,
    String comment
) {}
