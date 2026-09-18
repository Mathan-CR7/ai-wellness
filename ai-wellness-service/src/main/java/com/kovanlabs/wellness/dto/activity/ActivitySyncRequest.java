package com.kovanlabs.wellness.dto.activity;

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
public class ActivitySyncRequest {

    @NotNull(message = "Step count is required")
    @Min(value = 0, message = "Step count cannot be negative")
    private Integer stepCount;

    @NotNull(message = "Distance in meters is required")
    @Min(value = 0, message = "Distance cannot be negative")
    private Double distanceMeters;

    @NotNull(message = "Calories burned is required")
    @Min(value = 0, message = "Calories cannot be negative")
    private Double caloriesBurned;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @Builder.Default
    private String sourceDevice = "ANDROID_HEALTH_CONNECT";
}
