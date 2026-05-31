package com.colorinchi.app.colorimetry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.colorimetry")
public record ColorimetryProperties(
    double deltaEWeight,
    double seasonWeight,
    double ruleWeight,
    double deltaESigmoidMidpoint,
    double deltaESigmoidSteepness,
    double neutralDeltaThreshold,
    double intensityThreshold,
    double warmCoolThreshold
) {
    // Default factory
    public static ColorimetryProperties defaults() {
        return new ColorimetryProperties(
            0.40,    // deltaEWeight
            0.30,    // seasonWeight
            0.30,    // ruleWeight
            6.5,     // deltaESigmoidMidpoint
            0.35,    // deltaESigmoidSteepness
            15.0,    // neutralDeltaThreshold
            0.30,    // intensityThreshold
            5.0      // warmCoolThreshold
        );
    }
}
