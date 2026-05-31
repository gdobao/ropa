package com.colorinchi.app.dto.chat;

import java.util.List;

public record CompanionTipContext(
    String summary,
    List<String> tips
) {}
