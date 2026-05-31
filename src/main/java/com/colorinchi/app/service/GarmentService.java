package com.colorinchi.app.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.dto.DashboardStats;
import com.colorinchi.app.dto.GarmentReviewForm;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.repository.GarmentRepository;

@Service
public class GarmentService {

    private final GarmentRepository garmentRepository;
    private final CurrentOwnerAccessor currentOwnerAccessor;

    public GarmentService(GarmentRepository garmentRepository, CurrentOwnerAccessor currentOwnerAccessor) {
        this.garmentRepository = garmentRepository;
        this.currentOwnerAccessor = currentOwnerAccessor;
    }

    @Transactional(readOnly = true)
    public List<Garment> latest() {
        return garmentRepository.findTop12ByOwnerIdOrderByCreatedAtDesc(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public List<Garment> all() {
        return garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public Page<Garment> all(Pageable pageable) {
        return garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentOwnerId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Garment> filterByCategory(String category) {
        if (category == null || category.isBlank()) {
            return all();
        }
        return garmentRepository.findByOwnerIdAndCategoryOrderByCreatedAtDesc(currentOwnerId(), category);
    }

    @Transactional(readOnly = true)
    public List<Garment> favorites() {
        return garmentRepository.findByOwnerIdAndFavoriteTrueOrderByCreatedAtDesc(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats(long plannedDays, long plannedItems) {
        UUID ownerId = currentOwnerId();
        long total = garmentRepository.countByOwnerId(ownerId);
        long favorites = garmentRepository.countByOwnerIdAndFavoriteTrue(ownerId);
        int usagePercent = total > 0 ? (int) ((plannedDays * 100) / 7) : 0;
        var categories = garmentRepository.countByCategoryGrouped(ownerId).stream()
            .map(r -> new DashboardStats.CategoryCount((String) r[0], (Long) r[1]))
            .toList();
        long plannedCoveragePercent = total > 0 ? Math.min(100, (plannedItems * 100) / total) : 0;
        long nonFavoriteCount = Math.max(0, total - favorites);
        return new DashboardStats(total, favorites, usagePercent, plannedDays, plannedItems, plannedCoveragePercent, nonFavoriteCount, categories);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopColors() {
        return garmentRepository.countByColorGrouped(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public Garment get(Long id) {
        return garmentRepository.findByIdAndOwnerId(id, currentOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Prenda no encontrada"));
    }

    @Transactional
    public Garment create(GarmentReviewForm form) {
        Garment garment = new Garment();
        garment.setName(form.getName());
        garment.setCategory(form.getCategory());
        garment.setColorName(form.getColorName());
        garment.setColorHex(form.getColorHex());
        garment.setMaterial(form.getMaterial());
        garment.setSeason(form.getSeason());
        garment.setImageUrl(form.getImageUrl());
        garment.setAiType(form.getAiType());
        garment.setAiColorName(form.getAiColorName());
        garment.setAiColorHex(form.getAiColorHex());
        garment.setAiConfidence(form.getAiConfidence());
        garment.setAiModel(form.getAiModel());
        garment.setOwnerId(currentOwnerId());
        garment.setUserConfirmed(true);
        return garmentRepository.save(garment);
    }

    @Transactional
    public Garment update(Long id, GarmentReviewForm form) {
        Garment garment = get(id);
        garment.setName(form.getName());
        garment.setCategory(form.getCategory());
        garment.setColorName(form.getColorName());
        garment.setColorHex(form.getColorHex());
        garment.setMaterial(form.getMaterial());
        garment.setSeason(form.getSeason());
        // AI fields and imageUrl are preserved — not updated from edit form
        return garmentRepository.save(garment);
    }

    @Transactional
    public boolean toggleFavorite(Long id) {
        Garment garment = get(id);
        garment.setFavorite(!garment.isFavorite());
        garmentRepository.save(garment);
        return garment.isFavorite();
    }

    @Transactional
    public void delete(Long id) {
        if (garmentRepository.deleteByIdAndOwnerId(id, currentOwnerId()) == 0) {
            throw new IllegalArgumentException("Prenda no encontrada");
        }
    }

    private static final String PLACEHOLDER = "/img/placeholder.svg";

    private record SeedItem(String name, String category, String colorName, String colorHex, String material, String season) {}

    @Transactional
    public void seed() {
        UUID ownerId = currentOwnerId();
        List<SeedItem> items = List.of(
            new SeedItem("Top Negro", "Top", "Negro", "#1A1A1A", "Algodón", "Todo el año"),
            new SeedItem("Top Blanco", "Top", "Blanco", "#FFFFFF", "Algodón", "Todo el año"),
            new SeedItem("Camisa Blanca", "Camisa", "Blanco", "#FFFFFF", "Algodón", "Primavera/Verano"),
            new SeedItem("Camisa Azul claro", "Camisa", "Azul claro", "#A8D8EA", "Algodón", "Primavera/Verano"),
            new SeedItem("Pantalón Negro", "Pantalón", "Negro", "#1A1A1A", "Algodón", "Todo el año"),
            new SeedItem("Pantalón Beige", "Pantalón", "Beige", "#F5E6CC", "Lino", "Primavera/Verano"),
            new SeedItem("Jeans Azul oscuro", "Pantalón", "Azul oscuro", "#2C3E50", "Mezclilla", "Todo el año"),
            new SeedItem("Jeans Gris", "Pantalón", "Gris", "#808080", "Mezclilla", "Todo el año"),
            new SeedItem("Chaqueta Negra", "Chaqueta", "Negro", "#1A1A1A", "Poliéster", "Otoño/Invierno"),
            new SeedItem("Chaqueta Gris", "Chaqueta", "Gris", "#A9A9A9", "Lana", "Otoño/Invierno"),
            new SeedItem("Sudadera Gris", "Sudadera", "Gris", "#B0B0B0", "Algodón", "Otoño/Invierno"),
            new SeedItem("Sudadera Verde oliva", "Sudadera", "Verde oliva", "#556B2F", "Algodón", "Otoño/Invierno"),
            new SeedItem("Vestido Negro", "Vestido", "Negro", "#1A1A1A", "Viscosa", "Primavera/Verano"),
            new SeedItem("Falda Negra", "Falda", "Negro", "#1A1A1A", "Algodón", "Primavera/Verano"),
            new SeedItem("Zapatillas Blancas", "Zapatos", "Blanco", "#FFFFFF", "Cuero", "Todo el año"),
            new SeedItem("Botines Negros", "Zapatos", "Negro", "#1A1A1A", "Cuero", "Otoño/Invierno"),
            new SeedItem("Abrigo Negro", "Abrigo", "Negro", "#1A1A1A", "Lana", "Invierno"),
            new SeedItem("Abrigo Camel", "Abrigo", "Camel", "#C19A6B", "Lana", "Invierno"),
            new SeedItem("Bufanda Gris", "Accesorio", "Gris", "#808080", "Lana", "Todo el año"),
            new SeedItem("Cinturón Negro", "Accesorio", "Negro", "#1A1A1A", "Cuero", "Todo el año"),
            // Chillones — no combinan
            new SeedItem("Top Fucsia", "Top", "Fucsia", "#FF00FF", "Poliéster", "Verano"),
            new SeedItem("Pantalón Naranja", "Pantalón", "Naranja", "#FF6600", "Algodón", "Verano"),
            new SeedItem("Chaqueta Verde lima", "Chaqueta", "Verde lima", "#32CD32", "Nylon", "Primavera"),
            new SeedItem("Vestido Amarillo neón", "Vestido", "Amarillo neón", "#FFFF00", "Poliéster", "Verano"),
            new SeedItem("Zapatos Rojos brillantes", "Zapatos", "Rojo brillante", "#FF0000", "Cuero sintético", "Primavera/Verano")
        );
        for (SeedItem item : items) {
            Garment g = new Garment();
            g.setName(item.name());
            g.setCategory(item.category());
            g.setColorName(item.colorName());
            g.setColorHex(item.colorHex());
            g.setMaterial(item.material());
            g.setSeason(item.season());
            g.setImageUrl(PLACEHOLDER);
            g.setOwnerId(ownerId);
            g.setUserConfirmed(true);
            garmentRepository.save(g);
        }
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
