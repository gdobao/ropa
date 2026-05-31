package com.colorinchi.app.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.colorinchi.app.colorimetry.model.ColorProfile;
import com.colorinchi.app.colorimetry.service.ColorSeasonClassifier;
import com.colorinchi.app.config.WardrobeProperties;
import com.colorinchi.app.dto.chat.CategoryInfo;
import com.colorinchi.app.dto.chat.ColorInfo;
import com.colorinchi.app.dto.chat.DailyPlanInfo;
import com.colorinchi.app.dto.chat.GarmentSummary;
import com.colorinchi.app.dto.chat.MaterialInfo;
import com.colorinchi.app.dto.chat.WardrobeContext;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.model.WeekPlan;
import com.colorinchi.app.repository.GarmentRepository;
import com.colorinchi.app.repository.WeekPlanRepository;

@Service
public class WardrobeContextAssembler {

    private static final Logger log = LoggerFactory.getLogger(WardrobeContextAssembler.class);

    private static final Map<DayOfWeek, String> DAY_MAP = Map.ofEntries(
        Map.entry(DayOfWeek.MONDAY, "Lunes"),
        Map.entry(DayOfWeek.TUESDAY, "Martes"),
        Map.entry(DayOfWeek.WEDNESDAY, "Miercoles"),
        Map.entry(DayOfWeek.THURSDAY, "Jueves"),
        Map.entry(DayOfWeek.FRIDAY, "Viernes"),
        Map.entry(DayOfWeek.SATURDAY, "Sabado"),
        Map.entry(DayOfWeek.SUNDAY, "Domingo")
    );

    private final GarmentRepository garmentRepository;
    private final WeekPlanRepository weekPlanRepository;
    private final CurrentOwnerAccessor currentOwnerAccessor;
    private final WardrobeProperties wardrobeProperties;
    private final ColorSeasonClassifier classifier;

    public WardrobeContextAssembler(
            GarmentRepository garmentRepository,
            WeekPlanRepository weekPlanRepository,
            CurrentOwnerAccessor currentOwnerAccessor,
            WardrobeProperties wardrobeProperties,
            ColorSeasonClassifier classifier) {
        this.garmentRepository = garmentRepository;
        this.weekPlanRepository = weekPlanRepository;
        this.currentOwnerAccessor = currentOwnerAccessor;
        this.wardrobeProperties = wardrobeProperties;
        this.classifier = classifier;
    }

    @Transactional(readOnly = true)
    public WardrobeContext assemble() {
        Map<String, ColorProfile> classificationCache = new HashMap<>();
        UUID ownerId = currentOwnerAccessor.getCurrentOwnerId();

        // SQL-first: single query for garments, then aggregate in memory
        List<Garment> allGarments = garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        long totalGarments = allGarments.size();

        // -- Categories --
        List<CategoryInfo> categories = buildCategories(allGarments);

        // -- Colors (top N by count) --
        int colorLimit = wardrobeProperties.colorLimit();
        List<ColorInfo> colors = buildColors(allGarments, colorLimit, classificationCache);

        // -- Materials --
        List<MaterialInfo> materials = buildMaterials(allGarments);

        // -- Seasons --
        Map<String, Long> seasons = buildSeasons(allGarments);

        // -- Favorites --
        long favoritesCount = allGarments.stream().filter(Garment::isFavorite).count();

        // -- Weekly plan --
        List<WeekPlan> allPlans = weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId);
        long plannedDays = allPlans.stream()
                .map(WeekPlan::getDayOfWeek)
                .distinct()
                .count();
        long plannedItems = allPlans.size();

        // Today + upcoming days
        LocalDate today = LocalDate.now();
        int upcomingDays = wardrobeProperties.upcomingDaysToInclude();
        String todayDayName = DAY_MAP.get(today.getDayOfWeek());
        List<String> upcomingDayNames = new ArrayList<>();
        for (int i = 0; i < upcomingDays; i++) {
            DayOfWeek dow = today.plusDays(i).getDayOfWeek();
            upcomingDayNames.add(DAY_MAP.get(dow));
        }

        DailyPlanInfo todayPlan = buildDailyPlan(allPlans, todayDayName);
        List<DailyPlanInfo> upcomingPlans = upcomingDayNames.stream()
                .filter(d -> !d.equals(todayDayName))
                .map(d -> buildDailyPlan(allPlans, d))
                .filter(p -> p.garmentCount() > 0)
                .toList();

        List<GarmentSummary> garmentSummaries = allGarments.stream()
                .limit(wardrobeProperties.maxGarmentsInSummary())
                .map(g -> toGarmentSummary(g, classificationCache))
                .toList();

        // -- Color seasons --
        Map<String, Long> colorSeasons = buildColorSeasons(allGarments, classificationCache);

        return new WardrobeContext(
                totalGarments,
                categories,
                colors,
                materials,
                seasons,
                favoritesCount,
                plannedDays,
                plannedItems,
                todayPlan,
                upcomingPlans,
                garmentSummaries,
                colorSeasons);
    }

    private List<CategoryInfo> buildCategories(List<Garment> garments) {
        Map<String, Long> grouped = garments.stream()
                .collect(Collectors.groupingBy(Garment::getCategory, LinkedHashMap::new, Collectors.counting()));
        return grouped.entrySet().stream()
                .map(e -> new CategoryInfo(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ColorInfo> buildColors(List<Garment> garments, int limit, Map<String, ColorProfile> classificationCache) {
        Map<String, List<Garment>> byColor = garments.stream()
                .filter(g -> g.getColorHex() != null && !g.getColorHex().isBlank())
                .collect(Collectors.groupingBy(
                        g -> g.getColorName() + "|" + g.getColorHex(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        return byColor.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().size(), a.getValue().size()))
                .limit(limit)
                .map(e -> {
                    String[] parts = e.getKey().split("\\|", 2);
                    String hex = parts.length > 1 ? parts[1] : null;
                    String season = null;
                    if (hex != null && !hex.isBlank()) {
                        ColorProfile profile = classifyCached(hex, classificationCache);
                        if (profile != null && profile.season() != null) {
                            season = profile.season().displayName();
                        }
                    }
                    return new ColorInfo(parts[0], hex, e.getValue().size(), season);
                })
                .toList();
    }

    private List<MaterialInfo> buildMaterials(List<Garment> garments) {
        Map<String, Long> grouped = garments.stream()
                .filter(g -> g.getMaterial() != null && !g.getMaterial().isBlank())
                .collect(Collectors.groupingBy(Garment::getMaterial, LinkedHashMap::new, Collectors.counting()));
        return grouped.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> new MaterialInfo(e.getKey(), e.getValue()))
                .toList();
    }

    private Map<String, Long> buildSeasons(List<Garment> garments) {
        return garments.stream()
                .filter(g -> g.getSeason() != null && !g.getSeason().isBlank())
                .collect(Collectors.groupingBy(Garment::getSeason, LinkedHashMap::new, Collectors.counting()));
    }

    private Map<String, Long> buildColorSeasons(List<Garment> garments, Map<String, ColorProfile> classificationCache) {
        Map<String, Long> seasons = new LinkedHashMap<>();
        for (Garment g : garments) {
            if (g.getColorHex() != null && !g.getColorHex().isBlank()) {
                ColorProfile profile = classifyCached(g.getColorHex(), classificationCache);
                String label = profile != null && profile.season() != null
                        ? profile.season().displayName()
                        : "Sin estación";
                seasons.merge(label, 1L, Long::sum);
            }
        }
        return seasons;
    }

    private ColorProfile classifyCached(String hex, Map<String, ColorProfile> classificationCache) {
        if (hex == null || hex.isBlank()) return null;
        return classificationCache.computeIfAbsent(hex, h -> {
            try {
                return classifier.classify(h);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private GarmentSummary toGarmentSummary(Garment garment, Map<String, ColorProfile> classificationCache) {
        String garmentSeason = null;
        if (garment.getColorHex() != null && !garment.getColorHex().isBlank()) {
            ColorProfile profile = classifyCached(garment.getColorHex(), classificationCache);
            if (profile != null && profile.season() != null) {
                garmentSeason = profile.season().displayName();
            }
        }
        return new GarmentSummary(
                garment.getId(), garment.getName(), garment.getCategory(),
                garment.getColorName(), garment.getColorHex(),
                garment.getMaterial(), garment.getSeason(), garment.isFavorite(), garmentSeason);
    }

    private DailyPlanInfo buildDailyPlan(List<WeekPlan> allPlans, String dayName) {
        List<WeekPlan> dayPlans = allPlans.stream()
                .filter(p -> p.getDayOfWeek().equals(dayName))
                .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .toList();
        List<String> garmentNames = dayPlans.stream()
                .map(p -> p.getGarment().getName())
                .toList();
        return new DailyPlanInfo(dayName, dayPlans.size(), garmentNames);
    }
}
