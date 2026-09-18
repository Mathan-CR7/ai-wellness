package com.kovanlabs.wellness.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityTrendResponse {

    private Long userId;

    private Double movingAverageSteps7Days;

    private Double stepCompletionRatePercent;

    private Integer activeStreakDays;

    private Double totalDistanceWeeklyMeters;

    private Double totalCaloriesWeekly;
}
