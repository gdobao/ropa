package com.colorinchi.app.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.model.Garment;
import com.colorinchi.app.model.WeekPlan;
import com.colorinchi.app.repository.GarmentRepository;
import com.colorinchi.app.repository.WeekPlanRepository;

@Service
public class WeekPlanService {

    private final WeekPlanRepository repo;
    private final GarmentRepository garmentRepo;
    private final CurrentOwnerAccessor currentOwnerAccessor;

    public WeekPlanService(WeekPlanRepository repo, GarmentRepository garmentRepo, CurrentOwnerAccessor currentOwnerAccessor) {
        this.repo = repo;
        this.garmentRepo = garmentRepo;
        this.currentOwnerAccessor = currentOwnerAccessor;
    }

    @Transactional(readOnly = true)
    public Map<String, List<WeekPlan>> getPlansByDay() {
        List<WeekPlan> all = repo.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(currentOwnerId());
        Map<String, List<WeekPlan>> map = new LinkedHashMap<>();
        for (WeekPlan wp : all) {
            map.computeIfAbsent(wp.getDayOfWeek(), k -> new ArrayList<>()).add(wp);
        }
        return map;
    }

    @Transactional
    public void assignGarment(Long garmentId, String dayOfWeek, int position) {
        UUID ownerId = currentOwnerId();
        repo.deleteByOwnerIdAndGarmentId(ownerId, garmentId);
        Garment garment = garmentRepo.findByIdAndOwnerId(garmentId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Prenda no encontrada"));
        WeekPlan wp = new WeekPlan();
        wp.setGarment(garment);
        wp.setDayOfWeek(dayOfWeek);
        wp.setPosition(position);
        wp.setOwnerId(ownerId);
        repo.save(wp);
    }

    @Transactional
    public void remove(Long planId) {
        if (repo.deleteByIdAndOwnerId(planId, currentOwnerId()) == 0) {
            throw new IllegalArgumentException("Plan no encontrado");
        }
    }

    @Transactional
    public void reorderDay(String dayOfWeek, List<Long> order) {
        List<WeekPlan> currentPlans = repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(currentOwnerId(), dayOfWeek);
        Map<Long, WeekPlan> byId = new HashMap<>();
        for (WeekPlan plan : currentPlans) {
            byId.put(plan.getId(), plan);
        }

        List<WeekPlan> reordered = new ArrayList<>();
        int position = 0;
        for (Long planId : order) {
            WeekPlan plan = byId.get(planId);
            if (plan != null) {
                plan.setPosition(position++);
                reordered.add(plan);
            }
        }

        repo.saveAll(reordered);
    }

    @Transactional(readOnly = true)
    public long countDistinctDaysPlanned() {
        return repo.countDistinctDays(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public long countPlanned() {
        return repo.countByOwnerId(currentOwnerId());
    }

    @Transactional(readOnly = true)
    public List<Garment> findCompanionGarments(Long garmentId) {
        UUID ownerId = currentOwnerId();
        List<WeekPlan> basePlans = repo.findByOwnerIdAndGarmentId(ownerId, garmentId);
        if (basePlans.isEmpty()) {
            return List.of();
        }

        Collection<String> days = basePlans.stream()
                .map(WeekPlan::getDayOfWeek)
                .toList();

        List<WeekPlan> dayPlans = repo.findByOwnerIdAndDayOfWeekInOrderByDayOfWeekAscPositionAsc(ownerId, days);
        Set<Long> seenIds = new HashSet<>();

        return dayPlans.stream()
                .map(WeekPlan::getGarment)
                .filter(garment -> garment != null && garment.getId() != null)
                .filter(garment -> !garment.getId().equals(garmentId))
                .filter(garment -> seenIds.add(garment.getId()))
                .limit(6)
                .toList();
    }

    private UUID currentOwnerId() {
        return currentOwnerAccessor.getCurrentOwnerId();
    }
}
