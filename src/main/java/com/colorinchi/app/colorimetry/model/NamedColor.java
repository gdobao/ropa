package com.colorinchi.app.colorimetry.model;

public record NamedColor(
    String name,
    String hex,
    ColorSeason season,
    boolean neutral
) {}
