package com.kovanlabs.wellness.dto.exercise;

import com.kovanlabs.wellness.entity.enums.ExerciseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseResponse {

    private Long id;

    private Long userId;

    private ExerciseType exerciseType;

    private Integer durationMinutes;

    private Integer avgHeartRate;

    private Double caloriesBurned;

    private Instant timestamp;
}
