package com.kovanlabs.wellness.dto.activity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivitySyncRequest {

    @JsonAlias({"steps", "step_count", "stepCount", "totalSteps"})
    @Min(value = 0, message = "Step count cannot be negative")
    private Integer stepCount;

    @JsonAlias({"steps", "step_count", "stepCount"})
    private Long steps;

    @JsonAlias({"distanceMeters", "distance_meters", "distance"})
    @Min(value = 0, message = "Distance cannot be negative")
    private Double distanceMeters;

    @JsonAlias({"caloriesBurned", "calories_burned", "calories"})
    @Min(value = 0, message = "Calories cannot be negative")
    private Double caloriesBurned;

    @JsonAlias({"startTime", "start_time", "timestamp"})
    private Instant startTime;

    @JsonAlias({"endTime", "end_time"})
    private Instant endTime;

    @Builder.Default
    private String sourceDevice = "ANDROID_HEALTH_CONNECT";

    public Long getEffectiveStepCount() {
        if (stepCount != null) return stepCount.longValue();
        if (steps != null) return steps;
        return 0L;
    }
}
