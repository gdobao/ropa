package com.colorinchi.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.Garment;
import com.colorinchi.app.repository.GarmentRepository;

@Service
public class GarmentCompatibilityService {

    private static final int RESULT_LIMIT = 6;

    private static final Map<String, Set<String>> CATEGORY_COMPATIBILITY = Map.ofEntries(
            Map.entry("Top", Set.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio")),
            Map.entry("Pantalón", Set.of("Top", "Chaqueta", "Zapatos")),
            Map.entry("Vestido", Set.of("Chaqueta", "Zapatos", "Accesorio")),
            Map.entry("Chaqueta", Set.of("Top", "Pantalón", "Vestido")),
            Map.entry("Zapatos", Set.of("Top", "Pantalón", "Vestido", "Chaqueta", "Accesorio")),
            Map.entry("Accesorio", Set.of("Top", "Pantalón", "Vestido", "Chaqueta", "Zapatos")),
            Map.entry("Otro", Set.of("Top", "Pantalón", "Vestido")),
            Map.entry("Falda", Set.of("Top", "Chaqueta", "Zapatos", "Accesorio")),
            Map.entry("Abrigo", Set.of("Top", "Pantalón", "Zapatos", "Accesorio")),
            Map.entry("Camisa", Set.of("Pantalón", "Chaqueta", "Zapatos", "Accesorio")),
            Map.entry("Sudadera", Set.of("Pantalón", "Zapatos", "Accesorio")));

    private final GarmentRepository garmentRepository;
    private final CurrentOwnerAccessor currentOwnerAccessor;

    public GarmentCompatibilityService(GarmentRepository garmentRepository, CurrentOwnerAccessor currentOwnerAccessor) {
        this.garmentRepository = garmentRepository;
        this.currentOwnerAccessor = currentOwnerAccessor;
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

        UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();

        return garmentRepository.findByOwnerIdAndCategoryIn(ownerId, candidateCategories).stream()
                .filter(candidate -> base.getId() == null || !base.getId().equals(candidate.getId()))
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
