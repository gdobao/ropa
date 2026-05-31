package com.colorinchi.app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.colorinchi.app.model.WeekPlan;

public interface WeekPlanRepository extends JpaRepository<WeekPlan, Long> {

    List<WeekPlan> findByOwnerIdAndDayOfWeekOrderByPositionAsc(UUID ownerId, String dayOfWeek);

    List<WeekPlan> findByOwnerIdAndGarmentId(UUID ownerId, Long garmentId);

    Optional<WeekPlan> findByIdAndOwnerId(Long id, UUID ownerId);

    @EntityGraph(attributePaths = "garment")
    List<WeekPlan> findByOwnerIdAndDayOfWeekInOrderByDayOfWeekAscPositionAsc(UUID ownerId, Collection<String> days);

    @EntityGraph(attributePaths = "garment")
    List<WeekPlan> findAllByOwnerIdOrderByDayOfWeekAscPositionAsc(UUID ownerId);

    long deleteByOwnerIdAndGarmentId(UUID ownerId, Long garmentId);

    long deleteByIdAndOwnerId(Long id, UUID ownerId);

    long countByOwnerId(UUID ownerId);

    @Query("SELECT COUNT(DISTINCT w.dayOfWeek) FROM WeekPlan w WHERE w.ownerId = :ownerId")
    long countDistinctDays(@Param("ownerId") UUID ownerId);
}
