package com.colorinchi.app.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.colorinchi.app.model.WeekPlan;

public interface WeekPlanRepository extends JpaRepository<WeekPlan, Long> {

    List<WeekPlan> findByDayOfWeekOrderByPositionAsc(String dayOfWeek);

    List<WeekPlan> findByGarmentId(Long garmentId);

    List<WeekPlan> findByDayOfWeekInOrderByDayOfWeekAscPositionAsc(Collection<String> days);

    List<WeekPlan> findAllByOrderByDayOfWeekAscPositionAsc();

    void deleteByGarmentId(Long garmentId);

    @Query("SELECT COUNT(DISTINCT w.dayOfWeek) FROM WeekPlan w")
    long countDistinctDays();
}
