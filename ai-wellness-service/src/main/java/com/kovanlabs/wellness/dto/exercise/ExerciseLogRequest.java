package com.kovanlabs.wellness.dto.exercise;

import com.kovanlabs.wellness.entity.enums.ExerciseType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseLogRequest {

    @NotNull(message = "Exercise type is required")
    private ExerciseType exerciseType;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    private Integer avgHeartRate;

    @NotNull(message = "Calories burned is required")
    @Min(value = 0, message = "Calories cannot be negative")
    private Double caloriesBurned;

    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
}
