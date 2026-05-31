package com.colorinchi.app.colorimetry.model;

import java.util.Collections;
import java.util.List;

public record CompatibilityResult(
    int score,
    ColorHarmony harmony,
    String explanation,
    List<String> warnings
) {

    /**
     * Compact canonical constructor that defensively copies and defaults the warnings list.
     */
    public CompatibilityResult {
        if (warnings == null) {
            warnings = Collections.emptyList();
        } else {
            warnings = List.copyOf(warnings);
        }
    }

    /**
     * Convenience factory for a result with no warnings.
     */
    public static CompatibilityResult of(int score, ColorHarmony harmony, String explanation) {
        return new CompatibilityResult(score, harmony, explanation, Collections.emptyList());
    }
}
