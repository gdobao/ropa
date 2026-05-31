package com.colorinchi.app.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.config.WardrobeProperties;
import com.colorinchi.app.dto.WeeklyPlanItem;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.model.WeekPlan;
import com.colorinchi.app.repository.GarmentRepository;
import com.colorinchi.app.repository.WeekPlanRepository;

@Service
public class WeekPlanService {

    private final WeekPlanRepository repo;
    private final GarmentRepository garmentRepo;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final WardrobeProperties wardrobeProperties;

    public WeekPlanService(WeekPlanRepository repo, GarmentRepository garmentRepo,
                           CurrentOwnerAccessor currentOwnerAccessor, WardrobeProperties wardrobeProperties) {
        this.repo = repo;
        this.garmentRepo = garmentRepo;
        this.currentOwnerAccessor = currentOwnerAccessor;
        this.wardrobeProperties = wardrobeProperties;
    }

    @Transactional(readOnly = true)
    public Map<String, List<WeeklyPlanItem>> getPlansByDay() {
        List<WeekPlan> all = repo.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(currentOwnerId());
        Map<String, List<WeeklyPlanItem>> map = new LinkedHashMap<>();
        for (WeekPlan wp : all) {
            map.computeIfAbsent(wp.getDayOfWeek(), k -> new ArrayList<>()).add(toItem(wp));
        }
        return map;
    }

    @Transactional
    public void assignGarment(Long garmentId, String dayOfWeek, int position) {
        validateDay(dayOfWeek);
        UUID ownerId = currentOwnerId();
        List<WeekPlan> previousPlans = repo.findByOwnerIdAndGarmentId(ownerId, garmentId);
        repo.deleteByOwnerIdAndGarmentId(ownerId, garmentId);
        Garment garment = garmentRepo.findByIdAndOwnerId(garmentId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Prenda no encontrada"));

        Set<String> previousDays = previousPlans.stream()
                .map(WeekPlan::getDayOfWeek)
                .filter(previousDay -> !dayOfWeek.equals(previousDay))
                .collect(java.util.stream.Collectors.toSet());
        previousDays.forEach(day -> reindexDay(ownerId, day));

        List<WeekPlan> dayPlans = new ArrayList<>(repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(ownerId, dayOfWeek));
        WeekPlan wp = new WeekPlan();
        wp.setGarment(garment);
        wp.setDayOfWeek(dayOfWeek);
        wp.setOwnerId(ownerId);

        int insertAt = Math.max(0, Math.min(position, dayPlans.size()));
        dayPlans.add(insertAt, wp);
        saveReindexed(dayPlans);
    }

    @Transactional
    public void remove(Long planId) {
        UUID ownerId = currentOwnerId();
        WeekPlan plan = repo.findByIdAndOwnerId(planId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado"));
        String dayOfWeek = plan.getDayOfWeek();
        repo.deleteByIdAndOwnerId(planId, ownerId);
        reindexDay(ownerId, dayOfWeek);
    }

    @Transactional
    public void reorderDay(String dayOfWeek, List<Long> order) {
        validateDay(dayOfWeek);
        if (order == null) {
            throw new IllegalArgumentException("Orden inválido");
        }

        List<WeekPlan> currentPlans = repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(currentOwnerId(), dayOfWeek);
        if (currentPlans.isEmpty() && order.isEmpty()) {
            return;
        }

        Map<Long, WeekPlan> byId = new HashMap<>();
        for (WeekPlan plan : currentPlans) {
            byId.put(plan.getId(), plan);
        }

        Set<Long> requestedIds = new LinkedHashSet<>(order);
        if (requestedIds.size() != order.size() || !requestedIds.equals(byId.keySet())) {
            throw new IllegalArgumentException("El orden no coincide con las prendas del día");
        }

        List<WeekPlan> reordered = new ArrayList<>();
        for (Long planId : order) {
            reordered.add(byId.get(planId));
        }

        saveReindexed(reordered);
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

    private void validateDay(String dayOfWeek) {
        if (dayOfWeek == null || !wardrobeProperties.days().contains(dayOfWeek)) {
            throw new IllegalArgumentException("Día no válido");
        }
    }

    private void reindexDay(UUID ownerId, String dayOfWeek) {
        saveReindexed(repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(ownerId, dayOfWeek));
    }

    private void saveReindexed(List<WeekPlan> plans) {
        for (int i = 0; i < plans.size(); i++) {
            plans.get(i).setPosition(i);
        }
        repo.saveAll(plans);
    }

    private WeeklyPlanItem toItem(WeekPlan plan) {
        Garment garment = plan.getGarment();
        return new WeeklyPlanItem(plan.getId(), garment.getId(), garment.getName(), garment.getImageUrl());
    }
}
