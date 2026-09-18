package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.ExerciseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<ExerciseEntity, Long> {

    List<ExerciseEntity> findByUserIdOrderByTimestampDesc(Long userId);

    List<ExerciseEntity> findByUserIdAndTimestampBetweenOrderByTimestampAsc(
            Long userId, Instant start, Instant end);
}
