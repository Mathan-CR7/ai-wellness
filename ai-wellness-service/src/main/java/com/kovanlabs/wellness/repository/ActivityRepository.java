package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.ActivityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {

    List<ActivityEntity> findByUserIdOrderByStartTimeDesc(Long userId);

    List<ActivityEntity> findByUserIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
            Long userId, Instant startTime, Instant endTime);

    @Query("""
        SELECT COALESCE(SUM(a.stepCount), 0)
        FROM ActivityEntity a
        WHERE a.userId = :userId
          AND a.startTime >= :startTime
          AND a.endTime <= :endTime
        """)
    Long sumStepCountByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("""
        SELECT COALESCE(SUM(a.distanceMeters), 0.0)
        FROM ActivityEntity a
        WHERE a.userId = :userId
          AND a.startTime >= :startTime
          AND a.endTime <= :endTime
        """)
    Double sumDistanceMetersByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("""
        SELECT COALESCE(SUM(a.caloriesBurned), 0.0)
        FROM ActivityEntity a
        WHERE a.userId = :userId
          AND a.startTime >= :startTime
          AND a.endTime <= :endTime
        """)
    Double sumCaloriesBurnedByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
