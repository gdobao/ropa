package com.colorinchi.app.repository;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.colorinchi.app.model.AnonymousOwner;
import com.colorinchi.app.model.Garment;
import com.colorinchi.app.model.WeekPlan;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WeekPlanRepositoryTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Autowired
    private WeekPlanRepository repository;

    @Autowired
    private GarmentRepository garmentRepository;

    @Autowired
    private AnonymousOwnerRepository anonymousOwnerRepository;

    private Garment savedGarment;
    private Garment otherGarment;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        garmentRepository.deleteAll();
        anonymousOwnerRepository.save(createOwner(OTHER_OWNER_ID));

        savedGarment = garmentRepository.save(createGarment("Top Rojo", "Top", "Rojo"));
        otherGarment = garmentRepository.save(createGarment("Pantalón Azul", "Pantalón", "Azul"));
    }

    @Test
    void findByDayOfWeekOrderByPositionAscReturnsOrderedByPosition() {
        WeekPlan wp1 = createWeekPlan(savedGarment, "Lunes", 1);
        WeekPlan wp2 = createWeekPlan(savedGarment, "Lunes", 0);
        WeekPlan wp3 = createWeekPlan(savedGarment, "Lunes", 2);
        repository.saveAll(List.of(wp1, wp2, wp3));

        List<WeekPlan> plans = repository.findByOwnerIdAndDayOfWeekOrderByPositionAsc(OWNER_ID, "Lunes");

        assertThat(plans).hasSize(3);
        assertThat(plans.get(0).getPosition()).isEqualTo(0);
        assertThat(plans.get(1).getPosition()).isEqualTo(1);
        assertThat(plans.get(2).getPosition()).isEqualTo(2);
    }

    @Test
    void findByGarmentIdReturnsPlansForThatGarment() {
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(otherGarment, "Martes", 0));

        List<WeekPlan> plans = repository.findByOwnerIdAndGarmentId(OWNER_ID, savedGarment.getId());

        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getGarment().getId()).isEqualTo(savedGarment.getId());
    }

    @Test
    void findByDayOfWeekInOrderByDayOfWeekAscPositionAscReturnsOrdered() {
        repository.save(createWeekPlan(savedGarment, "Martes", 1));
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(savedGarment, "Martes", 0));

        List<WeekPlan> plans = repository.findByOwnerIdAndDayOfWeekInOrderByDayOfWeekAscPositionAsc(
                OWNER_ID, List.of("Lunes", "Martes"));

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

        long deleted = repository.deleteByOwnerIdAndGarmentId(OWNER_ID, savedGarment.getId());

        assertThat(deleted).isEqualTo(2);
        List<WeekPlan> remaining = repository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getGarment().getId()).isEqualTo(otherGarment.getId());
    }

    @Test
    void foreignOwnerCannotReadAnotherOwnersPlans() {
        Garment foreignGarment = garmentRepository.save(createGarmentForOwner(OTHER_OWNER_ID, "Foreign", "Top", "Negro"));
        repository.save(createWeekPlanForOwner(foreignGarment, OTHER_OWNER_ID, "Lunes", 0));

        List<WeekPlan> visible = repository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(OWNER_ID);

        assertThat(visible).isEmpty();
    }

    @Test
    void foreignOwnerCannotDeleteAnotherOwnersPlans() {
        Garment foreignGarment = garmentRepository.save(createGarmentForOwner(OTHER_OWNER_ID, "Foreign", "Top", "Negro"));
        WeekPlan foreignPlan = repository.save(createWeekPlanForOwner(foreignGarment, OTHER_OWNER_ID, "Lunes", 0));

        long deleted = repository.deleteByIdAndOwnerId(foreignPlan.getId(), OWNER_ID);

        assertThat(deleted).isZero();
        assertThat(repository.findByOwnerIdAndGarmentId(OTHER_OWNER_ID, foreignGarment.getId())).hasSize(1);
    }

    @Test
    void countDistinctDaysReturnsUniqueDayCount() {
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(savedGarment, "Lunes", 1));
        repository.save(createWeekPlan(otherGarment, "Martes", 0));

        long distinctDays = repository.countDistinctDays(OWNER_ID);

        assertThat(distinctDays).isEqualTo(2);
    }

    @Test
    void findAllByOrderByDayOfWeekAscPositionAscReturnsOrdered() {
        repository.save(createWeekPlan(savedGarment, "Martes", 1));
        repository.save(createWeekPlan(savedGarment, "Lunes", 0));
        repository.save(createWeekPlan(otherGarment, "Lunes", 1));

        List<WeekPlan> all = repository.findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(OWNER_ID);

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
        return createGarmentForOwner(OWNER_ID, name, category, colorName);
    }

    private Garment createGarmentForOwner(UUID ownerId, String name, String category, String colorName) {
        Garment g = new Garment();
        g.setName(name);
        g.setCategory(category);
        g.setColorName(colorName);
        g.setColorHex("#000000");
        g.setImageUrl("/uploads/" + name + ".jpg");
        g.setFavorite(false);
        g.setOwnerId(ownerId);
        g.setUserConfirmed(true);
        return g;
    }

    private WeekPlan createWeekPlan(Garment garment, String dayOfWeek, int position) {
        return createWeekPlanForOwner(garment, OWNER_ID, dayOfWeek, position);
    }

    private WeekPlan createWeekPlanForOwner(Garment garment, UUID ownerId, String dayOfWeek, int position) {
        WeekPlan wp = new WeekPlan();
        wp.setGarment(garment);
        wp.setDayOfWeek(dayOfWeek);
        wp.setPosition(position);
        wp.setOwnerId(ownerId);
        return wp;
    }

    private AnonymousOwner createOwner(UUID ownerId) {
        AnonymousOwner owner = new AnonymousOwner();
        owner.setId(ownerId);
        owner.setBootstrap(false);
        return owner;
    }
}
