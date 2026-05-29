package com.colorinchi.app.service;

import java.util.List;

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

    public GarmentService(GarmentRepository garmentRepository) {
        this.garmentRepository = garmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Garment> latest() {
        return garmentRepository.findTop12ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Garment> all() {
        return garmentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Page<Garment> all(Pageable pageable) {
        return garmentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<Garment> filterByCategory(String category) {
        if (category == null || category.isBlank()) {
            return all();
        }
        return garmentRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    @Transactional(readOnly = true)
    public List<Garment> favorites() {
        return garmentRepository.findByFavoriteTrueOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats(long plannedDays, long plannedItems) {
        long total = garmentRepository.count();
        long favorites = garmentRepository.countByFavoriteTrue();
        int usagePercent = total > 0 ? (int) ((plannedDays * 100) / 7) : 0;
        var categories = garmentRepository.countByCategoryGrouped().stream()
            .map(r -> new DashboardStats.CategoryCount((String) r[0], (Long) r[1]))
            .toList();
        return new DashboardStats(total, favorites, usagePercent, plannedDays, plannedItems, categories);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getTopColors() {
        return garmentRepository.countByColorGrouped();
    }

    @Transactional(readOnly = true)
    public Garment get(Long id) {
        return garmentRepository.findById(id)
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
        garmentRepository.deleteById(id);
    }
}
