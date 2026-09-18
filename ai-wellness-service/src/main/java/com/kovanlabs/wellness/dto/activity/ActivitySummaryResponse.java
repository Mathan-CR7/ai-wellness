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
public class ActivitySummaryResponse {

    private Long userId;

    private Long totalSteps;

    private Double totalDistanceMeters;

    private Double totalCaloriesBurned;

    private Instant periodStart;

    private Instant periodEnd;

    private Integer activityRecordCount;
}
