package com.colorinchi.app.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.colorinchi.app.config.WardrobeProperties;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.model.WeekPlan;
import com.colorinchi.app.repository.GarmentRepository;
import com.colorinchi.app.repository.WeekPlanRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeekPlanServiceTest {

    @Mock
    private WeekPlanRepository repo;

    @Mock
    private GarmentRepository garmentRepo;

    @Mock
    private CurrentOwnerAccessor currentOwnerAccessor;

    private WeekPlanService service;

    @Captor
    private ArgumentCaptor<List<WeekPlan>> planListCaptor;

    private Garment sampleGarment;
    private final UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        sampleGarment = new Garment();
        ReflectionTestUtils.setField(sampleGarment, "id", 1L);
        sampleGarment.setName("Top Rojo");
        sampleGarment.setCategory("Top");
        sampleGarment.setColorName("Rojo");
        sampleGarment.setColorHex("#FF0000");
        sampleGarment.setImageUrl("/uploads/test.jpg");
        sampleGarment.setOwnerId(ownerId);
        when(currentOwnerAccessor.getCurrentOwnerId()).thenReturn(ownerId);
        service = new WeekPlanService(repo, garmentRepo, currentOwnerAccessor, new WardrobeProperties(
                List.of("Top", "Pantalón", "Vestido", "Falda", "Chaqueta", "Abrigo", "Camisa", "Sudadera", "Zapatos", "Accesorio", "Otro"),
                List.of("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"),
                5, 30, 3));
    }

    // --- getPlansByDay ---

    @Test
    void getPlansByDayReturnsEmptyMapWhenNoPlans() {
        when(repo.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of());

        var plans = service.getPlansByDay();
        assertThat(plans).isEmpty();
    }

    @Test
    void getPlansByDayGroupsByDayOfWeek() {
        WeekPlan lunes1 = createPlan(1L, sampleGarment, "Lunes", 0);
        WeekPlan lunes2 = createPlan(2L, sampleGarment, "Lunes", 1);
        WeekPlan martes1 = createPlan(3L, sampleGarment, "Martes", 0);

        when(repo.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(ownerId)).thenReturn(List.of(lunes1, lunes2, martes1));

        var plans = service.getPlansByDay();

        assertThat(plans).containsOnlyKeys("Lunes", "Martes");
        assertThat(plans.get("Lunes")).hasSize(2);
        assertThat(plans.get("Martes")).hasSize(1);
    }

    // --- assignGarment ---

    @Test
    void assignGarmentSavesNewPlan() {
        when(repo.findByOwnerIdAndGarmentId(ownerId, 1L)).thenReturn(List.of());
        when(garmentRepo.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.of(sampleGarment));
        when(repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(ownerId, "Lunes")).thenReturn(List.of());

        service.assignGarment(1L, "Lunes", 0);

        verify(repo).deleteByOwnerIdAndGarmentId(ownerId, 1L);
        verify(repo).saveAll(planListCaptor.capture());
        WeekPlan saved = planListCaptor.getValue().get(0);
        assertThat(saved.getGarment().getId()).isEqualTo(1L);
        assertThat(saved.getDayOfWeek()).isEqualTo("Lunes");
        assertThat(saved.getPosition()).isEqualTo(0);
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
    }

    @Test
    void assignGarmentThrowsWhenGarmentNotFound() {
        when(repo.findByOwnerIdAndGarmentId(ownerId, 999L)).thenReturn(List.of());
        when(garmentRepo.findByIdAndOwnerId(999L, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignGarment(999L, "Lunes", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prenda no encontrada");

        verify(repo).deleteByOwnerIdAndGarmentId(ownerId, 999L);
        verify(repo, never()).saveAll(anyList());
    }

    // --- remove ---

    @Test
    void removeDeletesById() {
        WeekPlan plan = createPlan(1L, sampleGarment, "Lunes", 0);
        when(repo.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.of(plan));
        when(repo.deleteByIdAndOwnerId(1L, ownerId)).thenReturn(1L);
        when(repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(ownerId, "Lunes")).thenReturn(List.of());

        service.remove(1L);

        verify(repo).deleteByIdAndOwnerId(1L, ownerId);
    }

    @Test
    void removeThrowsWhenPlanBelongsToAnotherOwner() {
        when(repo.findByIdAndOwnerId(1L, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Plan no encontrado");
    }

    // --- reorderDay ---

    @Test
    void reorderDayReordersPlans() {
        WeekPlan plan1 = createPlan(1L, sampleGarment, "Lunes", 0);
        WeekPlan plan2 = createPlan(2L, sampleGarment, "Lunes", 1);
        WeekPlan plan3 = createPlan(3L, sampleGarment, "Lunes", 2);

        when(repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(ownerId, "Lunes"))
                .thenReturn(List.of(plan1, plan2, plan3));

        service.reorderDay("Lunes", List.of(3L, 1L, 2L));

        verify(repo).saveAll(planListCaptor.capture());
        List<WeekPlan> reordered = planListCaptor.getValue();
        assertThat(reordered).hasSize(3);
        assertThat(reordered.get(0).getId()).isEqualTo(3L);
        assertThat(reordered.get(0).getPosition()).isEqualTo(0);
        assertThat(reordered.get(1).getId()).isEqualTo(1L);
        assertThat(reordered.get(1).getPosition()).isEqualTo(1);
        assertThat(reordered.get(2).getId()).isEqualTo(2L);
        assertThat(reordered.get(2).getPosition()).isEqualTo(2);
    }

    @Test
    void reorderDayRejectsUnknownIds() {
        WeekPlan plan1 = createPlan(1L, sampleGarment, "Lunes", 0);
        WeekPlan plan2 = createPlan(2L, sampleGarment, "Lunes", 1);

        when(repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(ownerId, "Lunes"))
                .thenReturn(List.of(plan1, plan2));

        assertThatThrownBy(() -> service.reorderDay("Lunes", List.of(1L, 999L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El orden no coincide con las prendas del día");

        verify(repo, never()).saveAll(anyList());
    }

    @Test
    void reorderDayWithEmptyOrderAndNoPlansDoesNothing() {
        when(repo.findByOwnerIdAndDayOfWeekOrderByPositionAsc(ownerId, "Lunes")).thenReturn(List.of());

        service.reorderDay("Lunes", List.of());

        verify(repo, never()).saveAll(anyList());
    }

    // --- countDistinctDaysPlanned ---

    @Test
    void countDistinctDaysPlannedDelegatesToRepo() {
        when(repo.countDistinctDays(ownerId)).thenReturn(3L);

        long count = service.countDistinctDaysPlanned();

        assertThat(count).isEqualTo(3L);
    }

    // --- countPlanned ---

    @Test
    void countPlannedDelegatesToRepo() {
        when(repo.countByOwnerId(ownerId)).thenReturn(10L);

        long count = service.countPlanned();

        assertThat(count).isEqualTo(10L);
    }

    // --- findCompanionGarments ---

    @Test
    void findCompanionGarmentsWithNoPlansReturnsEmpty() {
        when(repo.findByOwnerIdAndGarmentId(ownerId, 1L)).thenReturn(List.of());

        var result = service.findCompanionGarments(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void findCompanionGarmentsReturnsCompanionsExcludingBase() {
        Garment g1 = createGarment(1L, "Top");
        Garment g2 = createGarment(2L, "Pantalón");
        Garment g3 = createGarment(3L, "Chaqueta");

        WeekPlan basePlan = new WeekPlan();
        basePlan.setGarment(g1);
        basePlan.setDayOfWeek("Lunes");

        WeekPlan companionPlan1 = new WeekPlan();
        companionPlan1.setGarment(g2);
        companionPlan1.setDayOfWeek("Lunes");

        WeekPlan companionPlan2 = new WeekPlan();
        companionPlan2.setGarment(g3);
        companionPlan2.setDayOfWeek("Lunes");

        when(repo.findByOwnerIdAndGarmentId(ownerId, 1L)).thenReturn(List.of(basePlan));
        when(repo.findByOwnerIdAndDayOfWeekInOrderByDayOfWeekAscPositionAsc(ownerId, List.of("Lunes")))
                .thenReturn(List.of(basePlan, companionPlan1, companionPlan2));

        var result = service.findCompanionGarments(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Garment::getId).containsExactly(2L, 3L);
    }

    @Test
    void findCompanionGarmentsLimitsToSix() {
        Garment base = createGarment(1L, "Top");
        WeekPlan basePlan = new WeekPlan();
        basePlan.setGarment(base);
        basePlan.setDayOfWeek("Lunes");

        List<WeekPlan> manyPlans = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> {
                    WeekPlan wp = new WeekPlan();
                    wp.setGarment(createGarment(10L + i, "Pantalón"));
                    wp.setDayOfWeek("Lunes");
                    return wp;
                })
                .toList();

        List<WeekPlan> allPlans = new java.util.ArrayList<>();
        allPlans.add(basePlan);
        allPlans.addAll(manyPlans);

        when(repo.findByOwnerIdAndGarmentId(ownerId, 1L)).thenReturn(List.of(basePlan));
        when(repo.findByOwnerIdAndDayOfWeekInOrderByDayOfWeekAscPositionAsc(ownerId, List.of("Lunes")))
                .thenReturn(allPlans);

        var result = service.findCompanionGarments(1L);

        assertThat(result).hasSizeLessThanOrEqualTo(6);
    }

    @Test
    void findCompanionGarmentsDeduplicatesSameGarment() {
        Garment base = createGarment(1L, "Top");
        Garment sameCompanion = createGarment(2L, "Pantalón");

        WeekPlan basePlan = new WeekPlan();
        basePlan.setGarment(base);
        basePlan.setDayOfWeek("Lunes");

        WeekPlan companionPlan1 = new WeekPlan();
        companionPlan1.setGarment(sameCompanion);
        companionPlan1.setDayOfWeek("Lunes");

        WeekPlan companionPlan2 = new WeekPlan();
        companionPlan2.setGarment(sameCompanion);
        companionPlan2.setDayOfWeek("Martes");

        when(repo.findByOwnerIdAndGarmentId(ownerId, 1L)).thenReturn(List.of(basePlan));
        when(repo.findByOwnerIdAndDayOfWeekInOrderByDayOfWeekAscPositionAsc(ownerId, List.of("Lunes")))
                .thenReturn(List.of(basePlan, companionPlan1, companionPlan2));

        var result = service.findCompanionGarments(1L);

        // Same garment appears on different days, should be deduplicated
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(2L);
    }

    // --- Helpers ---

    private WeekPlan createPlan(Long id, Garment garment, String day, int position) {
        WeekPlan wp = new WeekPlan();
        ReflectionTestUtils.setField(wp, "id", id);
        wp.setGarment(garment);
        wp.setDayOfWeek(day);
        wp.setPosition(position);
        wp.setOwnerId(ownerId);
        return wp;
    }

    private Garment createGarment(Long id, String category) {
        Garment g = new Garment();
        ReflectionTestUtils.setField(g, "id", id);
        g.setName(category + " " + id);
        g.setCategory(category);
        g.setColorName("Test");
        g.setColorHex("#000000");
        g.setImageUrl("/uploads/test.jpg");
        g.setOwnerId(ownerId);
        return g;
    }
}
