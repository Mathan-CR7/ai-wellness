package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;
import com.kovanlabs.wellness.entity.ActivityEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ActivityTrendCalculator {

    private static final int DAILY_STEP_GOAL = 10000;

    public ActivityTrendResponse calculate7DayTrend(Long userId, List<ActivityEntity> activities, Instant now) {
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        List<ActivityEntity> recentActivities = activities.stream()
                .filter(a -> !a.getStartTime().isBefore(sevenDaysAgo))
                .collect(Collectors.toList());

        long totalStepsWeekly = recentActivities.stream().mapToLong(ActivityEntity::getStepCount).sum();
        double totalDistanceWeekly = recentActivities.stream().mapToDouble(ActivityEntity::getDistanceMeters).sum();
        double totalCaloriesWeekly = recentActivities.stream().mapToDouble(ActivityEntity::getCaloriesBurned).sum();

        double movingAverageSteps = totalStepsWeekly / 7.0;
        double stepCompletionRate = Math.min(100.0, (movingAverageSteps / DAILY_STEP_GOAL) * 100.0);

        // Calculate consecutive active streak days
        Map<LocalDate, Long> dailyStepsMap = activities.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartTime().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.summingLong(ActivityEntity::getStepCount)
                ));

        int streak = 0;
        LocalDate currentDay = now.atZone(ZoneId.systemDefault()).toLocalDate();

        while (dailyStepsMap.containsKey(currentDay) && dailyStepsMap.get(currentDay) >= 5000) {
            streak++;
            currentDay = currentDay.minusDays(1);
        }

        return ActivityTrendResponse.builder()
                .userId(userId)
                .movingAverageSteps7Days(Math.round(movingAverageSteps * 100.0) / 100.0)
                .stepCompletionRatePercent(Math.round(stepCompletionRate * 100.0) / 100.0)
                .activeStreakDays(streak)
                .totalDistanceWeeklyMeters(Math.round(totalDistanceWeekly * 100.0) / 100.0)
                .totalCaloriesWeekly(Math.round(totalCaloriesWeekly * 100.0) / 100.0)
                .build();
    }
}
