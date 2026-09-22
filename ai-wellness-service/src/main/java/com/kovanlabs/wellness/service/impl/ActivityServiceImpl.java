package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySummaryResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;
import com.kovanlabs.wellness.entity.ActivityEntity;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import com.kovanlabs.wellness.exception.ActivityDataNotAvailableException;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.mapper.ActivityMapper;
import com.kovanlabs.wellness.provider.ActivityProvider;
import com.kovanlabs.wellness.provider.TeamProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.ActivityRepository;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.service.ActivityService;
import com.kovanlabs.wellness.service.ActivityTrendCalculator;
import com.kovanlabs.wellness.service.ActivityValidator;
import com.kovanlabs.wellness.service.InactivityDetectionService;
import com.kovanlabs.wellness.service.LeaderboardService;
import com.kovanlabs.wellness.service.WebSocketLeaderboardPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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
    private final DailyStepRepository dailyStepRepository;
    private final ActivityRepository activityRepository;
    private final InactivityDetectionService inactivityDetectionService;

    public ActivityServiceImpl(
            ActivityProvider activityProvider,
            UserProvider userProvider,
            ActivityMapper activityMapper,
            ActivityValidator activityValidator,
            ActivityTrendCalculator activityTrendCalculator,
            WebSocketLeaderboardPublisher leaderboardPublisher,
            LeaderboardService leaderboardService,
            TeamProvider teamProvider,
            DailyStepRepository dailyStepRepository,
            ActivityRepository activityRepository,
            @Lazy InactivityDetectionService inactivityDetectionService
    ) {
        this.activityProvider = activityProvider;
        this.userProvider = userProvider;
        this.activityMapper = activityMapper;
        this.activityValidator = activityValidator;
        this.activityTrendCalculator = activityTrendCalculator;
        this.leaderboardPublisher = leaderboardPublisher;
        this.leaderboardService = leaderboardService;
        this.teamProvider = teamProvider;
        this.dailyStepRepository = dailyStepRepository;
        this.activityRepository = activityRepository;
        this.inactivityDetectionService = inactivityDetectionService;
    }

    @Override
    public ActivityResponse syncActivity(Long userId, ActivitySyncRequest request) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Validate threshold bounds
        activityValidator.validate(request);

        // 1. Idempotently update DailyStepEntity so daily step sensor reading is strictly accurate
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Optional<DailyStepEntity> existingStepOpt = dailyStepRepository.findByUserIdAndDate(userId, today);
        DailyStepEntity stepEntity;
        if (existingStepOpt.isPresent()) {
            stepEntity = existingStepOpt.get();
            stepEntity.setSteps(request.getStepCount().longValue());
        } else {
            stepEntity = DailyStepEntity.builder()
                    .userId(userId)
                    .date(today)
                    .steps(request.getStepCount().longValue())
                    .build();
        }
        dailyStepRepository.save(stepEntity);

        // 2. Register step sync with InactivityDetectionService to check if real physical steps increased
        try {
            inactivityDetectionService.registerStepSync(userId, request.getStepCount().longValue());
        } catch (Exception e) {
            log.warn("Failed to register activity step sync with InactivityDetectionService for userId={}: {}", userId, e.getMessage());
        }

        // 3. Purge old duplicate 10s sync records for this user to prevent metric accumulation
        try {
            activityRepository.deleteByUserId(userId);
        } catch (Exception e) {
            log.warn("Could not purge duplicate activity records for userId={}: {}", userId, e.getMessage());
        }

        // 4. Save clean single active Activity record
        ActivityEntity entity = activityMapper.toEntity(request);
        entity.setUserId(userId);
        ActivityEntity savedActivity = activityProvider.save(entity);

        // 5. Broadcast real-time WebSocket leaderboard updates to Team #1 & user's teams
        try {
            Instant now = Instant.now();
            Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
            var leaderboard = leaderboardService.getTeamLeaderboard(1L, weekAgo, now);
            leaderboardPublisher.publishLeaderboardUpdate(1L, leaderboard);

            List<TeamMemberEntity> teams = teamProvider.findTeamsByUserId(userId);
            for (TeamMemberEntity membership : teams) {
                if (!membership.getTeamId().equals(1L)) {
                    var teamLeaderboard = leaderboardService.getTeamLeaderboard(membership.getTeamId(), weekAgo, now);
                    leaderboardPublisher.publishLeaderboardUpdate(membership.getTeamId(), teamLeaderboard);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast leaderboard update for userId={}: {}", userId, e.getMessage(), e);
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

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Long dailySteps = dailyStepRepository.findByUserIdAndDate(userId, today)
                .map(DailyStepEntity::getSteps)
                .orElse(0L);

        List<ActivityEntity> activities = activityProvider.findByUserId(userId);
        ActivityEntity latest = (activities != null && !activities.isEmpty()) ? activities.get(0) : null;

        long totalSteps = dailySteps;
        if (totalSteps == 0L && latest != null && latest.getStepCount() != null) {
            totalSteps = latest.getStepCount();
        }

        Double totalDistance = (latest != null && latest.getDistanceMeters() != null && latest.getDistanceMeters() > 0)
                ? latest.getDistanceMeters()
                : totalSteps * 0.66;

        Double totalCalories = (latest != null && latest.getCaloriesBurned() != null && latest.getCaloriesBurned() > 0)
                ? latest.getCaloriesBurned()
                : totalSteps * 0.04;

        return ActivitySummaryResponse.builder()
                .userId(userId)
                .totalSteps(totalSteps)
                .totalDistanceMeters(totalDistance)
                .totalCaloriesBurned(totalCalories)
                .periodStart(startTime)
                .periodEnd(endTime)
                .activityRecordCount(activities != null ? activities.size() : 0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityTrendResponse getActivityTrends(Long userId) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<ActivityEntity> activities = activityProvider.findByUserId(userId);
        if (activities.isEmpty())
        {
            throw new ActivityDataNotAvailableException("No activity data available to compute trends for user: " + userId);
        }
        return activityTrendCalculator.calculate7DayTrend(userId, activities, Instant.now());
    }
}
