package com.colorinchi.app.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public WeekPlanService(WeekPlanRepository repo, GarmentRepository garmentRepo) {
        this.repo = repo;
        this.garmentRepo = garmentRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, List<WeekPlan>> getPlansByDay() {
        List<WeekPlan> all = repo.findAllByOrderByDayOfWeekAscPositionAsc();
        Map<String, List<WeekPlan>> map = new LinkedHashMap<>();
        for (WeekPlan wp : all) {
            map.computeIfAbsent(wp.getDayOfWeek(), k -> new ArrayList<>()).add(wp);
        }
        return map;
    }

    @Transactional
    public void assignGarment(Long garmentId, String dayOfWeek, int position) {
        repo.deleteByGarmentId(garmentId);
        Garment garment = garmentRepo.findById(garmentId)
                .orElseThrow(() -> new IllegalArgumentException("Prenda no encontrada"));
        WeekPlan wp = new WeekPlan();
        wp.setGarment(garment);
        wp.setDayOfWeek(dayOfWeek);
        wp.setPosition(position);
        repo.save(wp);
    }

    @Transactional
    public void remove(Long planId) {
        repo.deleteById(planId);
    }

    @Transactional
    public void reorderDay(String dayOfWeek, List<Long> order) {
        List<WeekPlan> currentPlans = repo.findByDayOfWeekOrderByPositionAsc(dayOfWeek);
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
        return repo.countDistinctDays();
    }

    @Transactional(readOnly = true)
    public long countPlanned() {
        return repo.count();
    }

    @Transactional(readOnly = true)
    public List<Garment> findCompanionGarments(Long garmentId) {
        List<WeekPlan> basePlans = repo.findByGarmentId(garmentId);
        if (basePlans.isEmpty()) {
            return List.of();
        }

        Collection<String> days = basePlans.stream()
                .map(WeekPlan::getDayOfWeek)
                .toList();

        List<WeekPlan> dayPlans = repo.findByDayOfWeekInOrderByDayOfWeekAscPositionAsc(days);
        Set<Long> seenIds = new HashSet<>();

        return dayPlans.stream()
                .map(WeekPlan::getGarment)
                .filter(garment -> garment != null && garment.getId() != null)
                .filter(garment -> !garment.getId().equals(garmentId))
                .filter(garment -> seenIds.add(garment.getId()))
                .limit(6)
                .toList();
    }
}
