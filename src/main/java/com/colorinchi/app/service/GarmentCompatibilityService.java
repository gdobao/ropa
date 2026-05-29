package com.colorinchi.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.Garment;

@Service
public class GarmentCompatibilityService {

    private static final int RESULT_LIMIT = 6;

    private static final Map<String, Set<String>> CATEGORY_COMPATIBILITY = Map.of(
            "Top", Set.of("Pantalon", "Chaqueta", "Zapatos", "Accesorio"),
            "Pantalon", Set.of("Top", "Chaqueta", "Zapatos"),
            "Vestido", Set.of("Chaqueta", "Zapatos", "Accesorio"),
            "Chaqueta", Set.of("Top", "Pantalon", "Vestido"),
            "Zapatos", Set.of("Top", "Pantalon", "Vestido", "Chaqueta", "Accesorio"),
            "Accesorio", Set.of("Top", "Pantalon", "Vestido", "Chaqueta", "Zapatos"),
            "Otro", Set.of("Top", "Pantalon", "Vestido"));

    private final GarmentService garmentService;

    public GarmentCompatibilityService(GarmentService garmentService) {
        this.garmentService = garmentService;
    }

    @Transactional(readOnly = true)
    public List<Garment> findCompatible(Garment base) {
        if (base == null || base.getCategory() == null || base.getCategory().isBlank()) {
            return List.of();
        }

        Set<String> candidateCategories = CATEGORY_COMPATIBILITY.getOrDefault(base.getCategory(), Set.of());
        if (candidateCategories.isEmpty()) {
            return List.of();
        }

        String baseSeason = normalize(base.getSeason());

        return garmentService.all().stream()
                .filter(candidate -> base.getId() == null || !base.getId().equals(candidate.getId()))
                .filter(candidate -> candidateCategories.contains(candidate.getCategory()))
                .sorted(Comparator.comparing((Garment candidate) -> !sameSeason(baseSeason, candidate.getSeason())))
                .limit(RESULT_LIMIT)
                .toList();
    }

    private boolean sameSeason(String baseSeason, String candidateSeason) {
        if (baseSeason == null) {
            return false;
        }
        return baseSeason.equals(normalize(candidateSeason));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }
}
