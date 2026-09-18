package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.ExerciseEntity;
import com.kovanlabs.wellness.repository.ExerciseRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaExerciseProvider implements ExerciseProvider {

    private final ExerciseRepository exerciseRepository;

    public JpaExerciseProvider(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Override
    public ExerciseEntity save(ExerciseEntity exercise) {
        return exerciseRepository.save(exercise);
    }

    @Override
    public Optional<ExerciseEntity> findById(Long id) {
        return exerciseRepository.findById(id);
    }

    @Override
    public List<ExerciseEntity> findByUserId(Long userId) {
        return exerciseRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    @Override
    public List<ExerciseEntity> findByUserIdAndDateRange(Long userId, Instant start, Instant end) {
        return exerciseRepository.findByUserIdAndTimestampBetweenOrderByTimestampAsc(userId, start, end);
    }

    @Override
    public void deleteById(Long id) {
        exerciseRepository.deleteById(id);
    }
}
