package com.colorinchi.app.dto;

import java.util.List;

public record InspirationLook(
    long id,
    String name,
    String description,
    List<String> tags,
    String vibe,
    List<String> garmentTypes,
    String color1,
    String color2,
    String color3
) {}