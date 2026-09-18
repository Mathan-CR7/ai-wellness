package com.kovanlabs.wellness.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResponse {

    private Long id;

    private Long userId;

    private Integer stepCount;

    private Double distanceMeters;

    private Double caloriesBurned;

    private Instant startTime;

    private Instant endTime;

    private String sourceDevice;

    private Instant syncedAt;
}
