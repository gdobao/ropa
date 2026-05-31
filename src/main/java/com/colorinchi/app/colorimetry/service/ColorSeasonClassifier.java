package com.colorinchi.app.colorimetry.service;

import org.springframework.stereotype.Service;

import com.colorinchi.app.colorimetry.config.ColorimetryProperties;
import com.colorinchi.app.colorimetry.data.ColorPaletteStore;
import com.colorinchi.app.colorimetry.model.ColorProfile;
import com.colorinchi.app.colorimetry.model.ColorSeason;
import com.colorinchi.app.colorimetry.util.ColorSpaceConverter;

/**
 * Classifies a hex colour into a {@link ColorProfile} using the CIELAB
 * four-season matrix, falling back to nearest-neighbour lookup against
 * the {@link ColorPaletteStore} when the matrix result is ambiguous.
 */
@Service
public class ColorSeasonClassifier {

    private static final double FALLBACK_CONFIDENCE_FACTOR = 0.7;
    private static final double DISAGREEMENT_CONFIDENCE_FACTOR = 0.5;

    private final ColorPaletteStore paletteStore;
    private final ColorimetryProperties colorimetryProperties;

    public ColorSeasonClassifier(ColorPaletteStore paletteStore, ColorimetryProperties colorimetryProperties) {
        this.paletteStore = paletteStore;
        this.colorimetryProperties = colorimetryProperties;
    }

    /**
     * Classify the given hex colour and return a {@link ColorProfile}.
     * <ol>
     *   <li>Convert hex to CIELAB via {@link ColorSpaceConverter#hexToLab}</li>
     *   <li>Derive an initial profile with
     *       {@link ColorProfile#fromLab(double, double, double, String, double, double)},
     *       using the configured {@code warmCoolThreshold} and {@code intensityThreshold}</li>
     *   <li>Always compute the nearest-neighbour season from the palette store</li>
     *   <li>If the matrix season is {@code null}, use the fallback with reduced confidence</li>
     *   <li>If the matrix season disagrees with the nearest-neighbour and the
     *       nearest neighbour is not null, prefer the nearest-neighbour with
     *       {@code confidence * 0.5} (matrix is likely oversimplified for borderline colours)</li>
     *   <li>Otherwise return the profile as-is</li>
     * </ol>
     *
     * @param hex colour string, e.g. {@code #FF5733} (with or without {@code #})
     * @return a derived {@link ColorProfile}
     */
    public ColorProfile classify(String hex) {
        double[] lab = ColorSpaceConverter.hexToLab(hex);
        ColorProfile profile = ColorProfile.fromLab(lab[0], lab[1], lab[2], hex,
                colorimetryProperties.warmCoolThreshold(),
                colorimetryProperties.intensityThreshold());

        ColorSeason nnSeason = paletteStore.classifyByNearestNeighbor(hex);

        if (profile.season() == null && nnSeason != null) {
            double adjustedConfidence = profile.confidence() * FALLBACK_CONFIDENCE_FACTOR;
            return withSeason(profile, nnSeason, adjustedConfidence);
        }

        if (profile.season() != null && nnSeason != null && profile.season() != nnSeason) {
            double adjustedConfidence = profile.confidence() * DISAGREEMENT_CONFIDENCE_FACTOR;
            return withSeason(profile, nnSeason, adjustedConfidence);
        }

        return profile;
    }

    private static ColorProfile withSeason(ColorProfile original, ColorSeason season, double confidence) {
        return new ColorProfile(
                season,
                original.temperature(),
                original.intensity(),
                original.depth(),
                confidence,
                original.lab(),
                original.hex()
        );
    }
}
