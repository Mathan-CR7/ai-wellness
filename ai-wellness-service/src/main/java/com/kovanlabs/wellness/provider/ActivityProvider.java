package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.ActivityEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction layer for Health Connect activity data access.
 */
public interface ActivityProvider {

    ActivityEntity save(ActivityEntity activity);

    List<ActivityEntity> saveAll(List<ActivityEntity> activities);

    Optional<ActivityEntity> findById(Long id);

    List<ActivityEntity> findByUserId(Long userId);

    List<ActivityEntity> findByUserIdAndDateRange(Long userId, Instant startTime, Instant endTime);

    Long sumSteps(Long userId, Instant startTime, Instant endTime);

    Double sumDistance(Long userId, Instant startTime, Instant endTime);

    Double sumCalories(Long userId, Instant startTime, Instant endTime);
}
