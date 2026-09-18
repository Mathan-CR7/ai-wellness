package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySummaryResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;
import com.kovanlabs.wellness.entity.ActivityEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import com.kovanlabs.wellness.exception.ActivityDataNotAvailableException;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.mapper.ActivityMapper;
import com.kovanlabs.wellness.provider.ActivityProvider;
import com.kovanlabs.wellness.provider.TeamProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.service.ActivityService;
import com.kovanlabs.wellness.service.ActivityTrendCalculator;
import com.kovanlabs.wellness.service.ActivityValidator;
import com.kovanlabs.wellness.service.LeaderboardService;
import com.kovanlabs.wellness.service.WebSocketLeaderboardPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class ActivityServiceImpl implements ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityServiceImpl.class);

    private final ActivityProvider activityProvider;
    private final UserProvider userProvider;
    private final ActivityMapper activityMapper;
    private final ActivityValidator activityValidator;
    private final ActivityTrendCalculator activityTrendCalculator;
    private final WebSocketLeaderboardPublisher leaderboardPublisher;
    private final LeaderboardService leaderboardService;
    private final TeamProvider teamProvider;

    public ActivityServiceImpl(
            ActivityProvider activityProvider,
            UserProvider userProvider,
            ActivityMapper activityMapper,
            ActivityValidator activityValidator,
            ActivityTrendCalculator activityTrendCalculator,
            WebSocketLeaderboardPublisher leaderboardPublisher,
            LeaderboardService leaderboardService,
            TeamProvider teamProvider
    ) {
        this.activityProvider = activityProvider;
        this.userProvider = userProvider;
        this.activityMapper = activityMapper;
        this.activityValidator = activityValidator;
        this.activityTrendCalculator = activityTrendCalculator;
        this.leaderboardPublisher = leaderboardPublisher;
        this.leaderboardService = leaderboardService;
        this.teamProvider = teamProvider;
    }

    @Override
    public ActivityResponse syncActivity(Long userId, ActivitySyncRequest request) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Validate threshold bounds
        activityValidator.validate(request);

        ActivityEntity entity = activityMapper.toEntity(request);
        entity.setUserId(userId);

        ActivityEntity savedActivity = activityProvider.save(entity);

        // Broadcast leaderboard updates for all teams the user belongs to
        List<TeamMemberEntity> teams = teamProvider.findTeamsByUserId(userId);
        for (TeamMemberEntity membership : teams) {
            Long teamId = membership.getTeamId();
            try {
                Instant now = Instant.now();
                Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
                var leaderboard = leaderboardService.getTeamLeaderboard(teamId, weekAgo, now);
                leaderboardPublisher.publishLeaderboardUpdate(teamId, leaderboard);
            } catch (Exception e) {
                // Leaderboard broadcast failure must not break the activity sync
                log.warn("Failed to broadcast leaderboard update for teamId={}: {}", teamId, e.getMessage(), e);
            }
        }

        return activityMapper.toResponse(savedActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getUserActivities(Long userId) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<ActivityEntity> activities = activityProvider.findByUserId(userId);
        if (activities.isEmpty()) {
            throw new ActivityDataNotAvailableException("No physical activity data found for user: " + userId);
        }

        return activityMapper.toResponseList(activities);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivitySummaryResponse getActivitySummary(Long userId, Instant startTime, Instant endTime) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<ActivityEntity> activities = activityProvider.findByUserIdAndDateRange(userId, startTime, endTime);
        if (activities.isEmpty()) {
            throw new ActivityDataNotAvailableException(
                    "No physical activity data available between " + startTime + " and " + endTime + " for user: " + userId);
        }

        Long totalSteps = activityProvider.sumSteps(userId, startTime, endTime);
        Double totalDistance = activityProvider.sumDistance(userId, startTime, endTime);
        Double totalCalories = activityProvider.sumCalories(userId, startTime, endTime);

        return ActivitySummaryResponse.builder()
                .userId(userId)
                .totalSteps(totalSteps != null ? totalSteps : 0L)
                .totalDistanceMeters(totalDistance != null ? totalDistance : 0.0)
                .totalCaloriesBurned(totalCalories != null ? totalCalories : 0.0)
                .periodStart(startTime)
                .periodEnd(endTime)
                .activityRecordCount(activities.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityTrendResponse getActivityTrends(Long userId) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<ActivityEntity> activities = activityProvider.findByUserId(userId);
        if (activities.isEmpty()) {
            throw new ActivityDataNotAvailableException("No activity data available to compute trends for user: " + userId);
        }

        return activityTrendCalculator.calculate7DayTrend(userId, activities, Instant.now());
    }
}
