package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.ExerciseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction layer for workout exercise data access.
 */
public interface ExerciseProvider {

    ExerciseEntity save(ExerciseEntity exercise);

    Optional<ExerciseEntity> findById(Long id);

    List<ExerciseEntity> findByUserId(Long userId);

    List<ExerciseEntity> findByUserIdAndDateRange(Long userId, Instant start, Instant end);

    void deleteById(Long id);
}
