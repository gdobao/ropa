package com.colorinchi.app.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.colorinchi.app.config.WardrobeProperties;
import com.colorinchi.app.dto.chat.WardrobeContext;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.model.WeekPlan;
import com.colorinchi.app.repository.GarmentRepository;
import com.colorinchi.app.repository.WeekPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WardrobeContextAssemblerTest {

    @Mock
    private GarmentRepository garmentRepository;

    @Mock
    private WeekPlanRepository weekPlanRepository;

    @Mock
    private CurrentOwnerAccessor currentOwnerAccessor;

    private WardrobeProperties wardrobeProperties;
    private WardrobeContextAssembler assembler;

    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        wardrobeProperties = new WardrobeProperties(
                List.of("Top", "Pantalón", "Vestido", "Zapatos"),
                List.of("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"),
                5,    // colorLimit
                30,   // maxGarmentsInSummary
                3     // upcomingDaysToInclude
        );
        assembler = new WardrobeContextAssembler(
                garmentRepository, weekPlanRepository, currentOwnerAccessor, wardrobeProperties);
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
    }

    @Test
    void assembleWithEmptyWardrobe() {
        when(garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(List.of());
        when(weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of());

        WardrobeContext ctx = assembler.assemble();

        assertThat(ctx.totalGarments()).isZero();
        assertThat(ctx.categories()).isEmpty();
        assertThat(ctx.colors()).isEmpty();
        assertThat(ctx.materials()).isEmpty();
        assertThat(ctx.seasons()).isEmpty();
        assertThat(ctx.favoritesCount()).isZero();
        assertThat(ctx.plannedDays()).isZero();
        assertThat(ctx.plannedItems()).isZero();
        assertThat(ctx.todayPlan().garmentCount()).isZero();
        assertThat(ctx.upcomingPlans()).isEmpty();
    }

    @Test
    void assembleWithGarmentsReturnsCorrectCounts() {
        List<Garment> garments = List.of(
                createGarment(1L, "Top", "Rojo", "#FF0000", "Algodon", "Verano", false),
                createGarment(2L, "Pantalón", "Azul", "#0000FF", "Jean", "Invierno", true),
                createGarment(3L, "Top", "Negro", "#000000", "Algodon", "Verano", false),
                createGarment(4L, "Zapatos", "Negro", "#000000", "Cuero", null, true),
                createGarment(5L, "Vestido", "Rojo", "#FF0000", "Seda", "Verano", false)
        );

        when(garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(garments);
        when(weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of());

        WardrobeContext ctx = assembler.assemble();

        assertThat(ctx.totalGarments()).isEqualTo(5);
        assertThat(ctx.favoritesCount()).isEqualTo(2);

        // Categories
        assertThat(ctx.categories()).hasSize(4);
        assertThat(ctx.categories()).anyMatch(c -> c.category().equals("Top") && c.count() == 2);
        assertThat(ctx.categories()).anyMatch(c -> c.category().equals("Pantalón") && c.count() == 1);
        assertThat(ctx.categories()).anyMatch(c -> c.category().equals("Zapatos") && c.count() == 1);
        assertThat(ctx.categories()).anyMatch(c -> c.category().equals("Vestido") && c.count() == 1);
    }

    @Test
    void assembleWithColorsReturnsTopN() {
        List<Garment> garments = List.of(
                createGarment(1L, "Top", "Rojo", "#FF0000", null, null, false),
                createGarment(2L, "Pantalón", "Azul", "#0000FF", null, null, false),
                createGarment(3L, "Top", "Rojo", "#FF0000", null, null, false),
                createGarment(4L, "Zapatos", "Negro", "#000000", null, null, false)
        );

        when(garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(garments);
        when(weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of());

        WardrobeContext ctx = assembler.assemble();

        // Rojo should be first (count=2), then Azul (count=1), then Negro (count=1)
        assertThat(ctx.colors()).isNotEmpty();
        assertThat(ctx.colors().get(0).colorName()).isEqualTo("Rojo");
        assertThat(ctx.colors().get(0).count()).isEqualTo(2);
    }

    @Test
    void assembleWithMaterialsGroupsCorrectly() {
        List<Garment> garments = List.of(
                createGarment(1L, "Top", "Rojo", "#FF0000", "Algodon", null, false),
                createGarment(2L, "Pantalón", "Azul", "#0000FF", "Jean", null, false),
                createGarment(3L, "Camisa", "Blanco", "#FFFFFF", "Algodon", null, false),
                createGarment(4L, "Zapatos", "Negro", "#000000", "Cuero", null, false)
        );

        when(garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(garments);
        when(weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of());

        WardrobeContext ctx = assembler.assemble();

        assertThat(ctx.materials()).hasSize(3);
        assertThat(ctx.materials()).anyMatch(m -> m.material().equals("Algodon") && m.count() == 2);
        assertThat(ctx.materials()).anyMatch(m -> m.material().equals("Jean") && m.count() == 1);
        assertThat(ctx.materials()).anyMatch(m -> m.material().equals("Cuero") && m.count() == 1);
    }

    @Test
    void assembleWithSeasonsDistributesCorrectly() {
        List<Garment> garments = List.of(
                createGarment(1L, "Top", "Rojo", "#FF0000", null, "Verano", false),
                createGarment(2L, "Pantalón", "Azul", "#0000FF", null, "Invierno", false),
                createGarment(3L, "Top", "Negro", "#000000", null, "Verano", false),
                createGarment(4L, "Zapatos", "Negro", "#000000", null, "Verano", false)
        );

        when(garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(garments);
        when(weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of());

        WardrobeContext ctx = assembler.assemble();

        assertThat(ctx.seasons()).hasSize(2);
        assertThat(ctx.seasons()).containsEntry("Verano", 3L);
        assertThat(ctx.seasons()).containsEntry("Invierno", 1L);
    }

    @Test
    void assembleWithWeeklyPlanIncludesTodayAndUpcoming() {
        LocalDate today = LocalDate.now();
        String todayName = switch (today.getDayOfWeek()) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miercoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sabado";
            case SUNDAY -> "Domingo";
        };

        Garment top = createGarment(1L, "Top", "Rojo", "#FF0000", null, null, false);
        Garment pants = createGarment(2L, "Pantalón", "Azul", "#0000FF", null, null, false);

        WeekPlan todayPlan = new WeekPlan();
        todayPlan.setGarment(top);
        todayPlan.setDayOfWeek(todayName);
        todayPlan.setPosition(0);
        todayPlan.setOwnerId(ownerId);

        WeekPlan tomorrowPlan = new WeekPlan();
        tomorrowPlan.setGarment(pants);
        tomorrowPlan.setDayOfWeek(today.plusDays(1).getDayOfWeek().name().substring(0, 1)
                + today.plusDays(1).getDayOfWeek().name().substring(1).toLowerCase());
        // Actually use the proper Spanish name
        String tomorrowName = switch (today.plusDays(1).getDayOfWeek()) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miercoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sabado";
            case SUNDAY -> "Domingo";
        };
        tomorrowPlan.setDayOfWeek(tomorrowName);
        tomorrowPlan.setPosition(0);
        tomorrowPlan.setOwnerId(ownerId);

        List<Garment> garments = List.of(top, pants);
        List<WeekPlan> plans = List.of(todayPlan, tomorrowPlan);

        when(garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(garments);
        when(weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(plans);

        WardrobeContext ctx = assembler.assemble();

        assertThat(ctx.plannedDays()).isGreaterThanOrEqualTo(1);
        assertThat(ctx.plannedItems()).isEqualTo(2);

        // Today's plan should be populated
        assertThat(ctx.todayPlan()).isNotNull();
        assertThat(ctx.todayPlan().dayOfWeek()).isEqualTo(todayName);
    }

    @Test
    void assembleColorLimitRespected() {
        wardrobeProperties = new WardrobeProperties(
                List.of("Top", "Pantalón"),
                List.of("Lunes", "Martes"),
                2,   // colorLimit: only top 2 colors
                30,
                3
        );
        assembler = new WardrobeContextAssembler(
                garmentRepository, weekPlanRepository, currentOwnerAccessor, wardrobeProperties);

        List<Garment> garments = List.of(
                createGarment(1L, "Top", "Rojo", "#FF0000", null, null, false),
                createGarment(2L, "Pantalón", "Azul", "#0000FF", null, null, false),
                createGarment(3L, "Top", "Rojo", "#FF0000", null, null, false),
                createGarment(4L, "Zapatos", "Negro", "#000000", null, null, false),
                createGarment(5L, "Vestido", "Verde", "#00FF00", null, null, false)
        );

        when(garmentRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)).thenReturn(garments);
        when(weekPlanRepository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of());

        WardrobeContext ctx = assembler.assemble();

        assertThat(ctx.colors().size()).isLessThanOrEqualTo(2);
    }

    private Garment createGarment(Long id, String category, String colorName, String colorHex,
                                   String material, String season, boolean favorite) {
        Garment g = new Garment();
        ReflectionTestUtils.setField(g, "id", id);
        g.setName(category + " " + colorName);
        g.setCategory(category);
        g.setColorName(colorName);
        g.setColorHex(colorHex);
        g.setMaterial(material);
        g.setSeason(season);
        g.setFavorite(favorite);
        g.setImageUrl("/uploads/" + id + ".jpg");
        g.setOwnerId(ownerId);
        g.setUserConfirmed(true);
        g.setCreatedAt(OffsetDateTime.now());
        g.setUpdatedAt(OffsetDateTime.now());
        return g;
    }
}
