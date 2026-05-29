package com.colorinchi.app.repository;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.colorinchi.app.model.Garment;
import com.colorinchi.app.model.WeekPlan;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WeekPlanRepositoryTest {

    @Autowired
    private WeekPlanRepository repository;

    @Autowired
    private GarmentRepository garmentRepository;

    private Garment savedGarment;
    private Garment otherGarment;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        garmentRepository.deleteAll();

        savedGarment = garmentRepository.save(createGarment("Top Rojo", "Top", "Rojo"));
        otherGarment = garmentRepository.save(createGarment("Pantalón Azul", "Pantalón", "Azul"));
    }

    @Test
    void findByDayOfWeekOrderByPositionAscReturnsOrderedByPosition() {
        WeekPlan wp1 = createWeekPlan(savedGarment, "Lunes", 1);
        WeekPlan wp2 = createWeekPlan(savedGarment, "Lunes", 0);
        WeekPlan wp3 = createWeekPlan(savedGarment, "Lunes", 2);
        repository.saveAll(List.of(wp1, wp2, wp3));

        List<WeekPlan> plans = repository.findByDayOfWeekOrderByPositionAsc("Lunes");

        assertThat(plans).hasSize(3);
        assertThat(plans.get(0).getPosition()).isEqualTo(0);
        assertThat(plans.get(1).getPosition()).isEqualTo(1);
        assertThat(plans.get(2).getPosition()).isEqualTo(2);
    }

    @Test
    void findByGarmentIdReturnsPlansForThatGarment() {
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(otherGarment, "Martes", 0));

        List<WeekPlan> plans = repository.findByGarmentId(savedGarment.getId());

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getGarment().getId()).isEqualTo(savedGarment.getId());
    }

    @Test
    void findByDayOfWeekInOrderByDayOfWeekAscPositionAscReturnsOrdered() {
        repository.save(createWeekPlan(savedGarment, "Martes", 1));
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(savedGarment, "Martes", 0));

        List<WeekPlan> plans = repository.findByDayOfWeekInOrderByDayOfWeekAscPositionAsc(
                List.of("Lunes", "Martes"));

        assertThat(plans).hasSize(3);
        assertThat(plans.get(0).getDayOfWeek()).isEqualTo("Lunes");
        assertThat(plans.get(0).getPosition()).isEqualTo(0);
        assertThat(plans.get(1).getDayOfWeek()).isEqualTo("Martes");
        assertThat(plans.get(1).getPosition()).isEqualTo(0);
        assertThat(plans.get(2).getDayOfWeek()).isEqualTo("Martes");
        assertThat(plans.get(2).getPosition()).isEqualTo(1);
    }

    @Test
    void deleteByGarmentIdRemovesAllPlansForThatGarment() {
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(savedGarment, "Martes", 0));
        repository.save(createWeekPlan(otherGarment, "Miercoles", 0));

        repository.deleteByGarmentId(savedGarment.getId());

        List<WeekPlan> remaining = repository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getGarment().getId()).isEqualTo(otherGarment.getId());
    }

    @Test
    void countDistinctDaysReturnsUniqueDayCount() {
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(savedGarment, "Lunes", 1));
        repository.save(createWeekPlan(otherGarment, "Martes", 0));

        long distinctDays = repository.countDistinctDays();

        assertThat(distinctDays).isEqualTo(2);
    }

    @Test
    void findAllByOrderByDayOfWeekAscPositionAscReturnsOrdered() {
        repository.save(createWeekPlan(savedGarment, "Martes", 1));
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(otherGarment, "Lunes", 1));

        List<WeekPlan> all = repository.findAllByOrderByDayOfWeekAscPositionAsc();

        assertThat(all).hasSize(3);
        assertThat(all.get(0).getDayOfWeek()).isEqualTo("Lunes");
        assertThat(all.get(0).getPosition()).isEqualTo(0);
        assertThat(all.get(1).getDayOfWeek()).isEqualTo("Lunes");
        assertThat(all.get(1).getPosition()).isEqualTo(1);
        assertThat(all.get(2).getDayOfWeek()).isEqualTo("Martes");
        assertThat(all.get(2).getPosition()).isEqualTo(1);
    }

    // --- Helpers ---

    private Garment createGarment(String name, String category, String colorName) {
        Garment g = new Garment();
        g.setName(name);
        g.setCategory(category);
        g.setColorName(colorName);
        g.setColorHex("#000000");
        g.setImageUrl("/uploads/" + name + ".jpg");
        g.setFavorite(false);
        g.setUserConfirmed(true);
        return g;
    }

    private WeekPlan createWeekPlan(Garment garment, String dayOfWeek, int position) {
        WeekPlan wp = new WeekPlan();
        wp.setGarment(garment);
        wp.setDayOfWeek(dayOfWeek);
        wp.setPosition(position);
        return wp;
    }
}
