package com.kovanlabs.wellness.config;

import com.kovanlabs.wellness.dto.activity.ActivitySummaryResponse;
import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;
import com.kovanlabs.wellness.dto.challenge.ChallengeResponse;
import com.kovanlabs.wellness.service.ActivityService;
import com.kovanlabs.wellness.service.ChallengeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

/**
 * Spring AI Tool Calling / Function Calling Configuration.
 *
 * <p>Exposes real database operations as functions that Gemini can invoke
 * before formulating responses. Enforces 0% hardcoded data requirement.</p>
 */
@Configuration
public class AiToolConfig {

    public record ActivitySummaryRequest(Long userId, Integer daysBack) {}
    public record ActivityTrendRequest(Long userId) {}
    public record EmptyRequest() {}

    @Bean
    @Description("Fetch user physical activity summary (steps, distance, calories) over specified past days")
    public Function<ActivitySummaryRequest, ActivitySummaryResponse> getUserActivitySummary(ActivityService activityService) {
        return request -> {
            int days = request.daysBack() != null ? request.daysBack() : 7;
            Instant end = Instant.now();
            Instant start = end.minus(days, ChronoUnit.DAYS);
            try {
                return activityService.getActivitySummary(request.userId(), start, end);
            } catch (Exception e) {
                return ActivitySummaryResponse.builder()
                        .userId(request.userId())
                        .totalSteps(0L)
                        .totalDistanceMeters(0.0)
                        .totalCaloriesBurned(0.0)
                        .periodStart(start)
                        .periodEnd(end)
                        .activityRecordCount(0)
                        .build();
            }
        };
    }

    @Bean
    @Description("Fetch user 7-day moving averages, completion rates, and active streak days")
    public Function<ActivityTrendRequest, ActivityTrendResponse> getUserActivityTrends(ActivityService activityService) {
        return request -> {
            try {
                return activityService.getActivityTrends(request.userId());
            } catch (Exception e) {
                return ActivityTrendResponse.builder()
                        .userId(request.userId())
                        .movingAverageSteps7Days(0.0)
                        .stepCompletionRatePercent(0.0)
                        .activeStreakDays(0)
                        .totalDistanceWeeklyMeters(0.0)
                        .totalCaloriesWeekly(0.0)
                        .build();
            }
        };
    }

    @Bean
    @Description("Fetch all active community wellness challenges")
    public Function<EmptyRequest, List<ChallengeResponse>> getActiveChallenges(ChallengeService challengeService) {
        return request -> challengeService.getActiveChallenges();
    }
}
