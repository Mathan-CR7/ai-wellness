package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.DailyStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyStepRepository extends JpaRepository<DailyStepEntity, Long> {

    Optional<DailyStepEntity> findByUserIdAndDate(Long userId, LocalDate date);

    @Query("SELECT COALESCE(SUM(d.steps), 0) FROM DailyStepEntity d WHERE d.userId = :userId AND d.date >= :startDate AND d.date <= :endDate")
    Long sumStepsByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<DailyStepEntity> findByUserIdInAndDateBetween(List<Long> userIds, LocalDate startDate, LocalDate endDate);
}
