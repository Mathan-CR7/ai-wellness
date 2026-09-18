package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;
import com.kovanlabs.wellness.dto.auth.UserRegistrationRequest;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase5ActivityProcessingTests {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Test
    @DisplayName("Test Activity Bounds Validation: Reject Step Count Exceeding Max Daily Limit")
    void testMaxDailyStepsBoundValidation() {
        UserProfileResponse user = userService.registerUser(UserRegistrationRequest.builder()
                .email("boundtest@example.com").password("Pass123!").fullName("Bound User").build());

        Instant now = Instant.now();
        ActivitySyncRequest invalidReq = ActivitySyncRequest.builder()
                .stepCount(200000) // Exceeds maxDailySteps threshold (100,000)
                .distanceMeters(150000.0)
                .caloriesBurned(5000.0)
                .startTime(now.minus(1, ChronoUnit.HOURS))
                .endTime(now)
                .sourceDevice("ANDROID_HEALTH_CONNECT")
                .build();

        assertThrows(IllegalArgumentException.class, () -> activityService.syncActivity(user.getId(), invalidReq));
    }

    @Test
    @DisplayName("Test 7-Day Moving Average & Active Streak Day Calculations")
    void testActivityTrendsCalculation() {
        UserProfileResponse user = userService.registerUser(UserRegistrationRequest.builder()
                .email("trenduser@example.com").password("Pass123!").fullName("Trend User").build());

        Instant now = Instant.now();

        // Sync activity for today (8000 steps)
        activityService.syncActivity(user.getId(), ActivitySyncRequest.builder()
                .stepCount(8000)
                .distanceMeters(5500.0)
                .caloriesBurned(400.0)
                .startTime(now.minus(2, ChronoUnit.HOURS))
                .endTime(now.minus(1, ChronoUnit.HOURS))
                .sourceDevice("ANDROID_HEALTH_CONNECT")
                .build());

        ActivityTrendResponse trends = activityService.getActivityTrends(user.getId());

        assertNotNull(trends);
        assertEquals(user.getId(), trends.getUserId());
        assertTrue(trends.getMovingAverageSteps7Days() > 0);
        assertTrue(trends.getStepCompletionRatePercent() > 0);
        assertEquals(1, trends.getActiveStreakDays());
    }
}
