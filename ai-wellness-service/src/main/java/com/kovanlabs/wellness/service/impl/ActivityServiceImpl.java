package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySummaryResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;
import com.kovanlabs.wellness.dto.activity.ActivityUpdateMessage;
import com.kovanlabs.wellness.entity.ActivityEntity;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.mapper.ActivityMapper;
import com.kovanlabs.wellness.provider.ActivityProvider;
import com.kovanlabs.wellness.provider.TeamProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.ActivityRepository;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.service.*;
import com.kovanlabs.wellness.service.ActivityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final ChallengeService challengeService;

    public ActivityServiceImpl(
            ActivityProvider activityProvider,
            UserProvider userProvider,
            ActivityMapper activityMapper,
            ActivityValidator activityValidator,
            ActivityTrendCalculator activityTrendCalculator,
            WebSocketLeaderboardPublisher leaderboardPublisher,
            @Lazy LeaderboardService leaderboardService,
            TeamProvider teamProvider,
            DailyStepRepository dailyStepRepository,
            ActivityRepository activityRepository,
            @Lazy InactivityDetectionService inactivityDetectionService,
            @Lazy ChallengeService challengeService
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
        this.challengeService = challengeService;
    }

    @Override
    public ActivityResponse syncActivity(Long userId, ActivitySyncRequest request) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        activityValidator.validate(request);

        // 1. Idempotently update DailyStepEntity in DB (SINGLE SOURCE OF TRUTH)
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Optional<DailyStepEntity> existingStepOpt = dailyStepRepository.findByUserIdAndDate(userId, today);
        DailyStepEntity stepEntity;
        long newStepCount = request.getStepCount().longValue();

        if (existingStepOpt.isPresent()) {
            stepEntity = existingStepOpt.get();
            stepEntity.setSteps(newStepCount);
        } else {
            stepEntity = DailyStepEntity.builder()
                    .userId(userId)
                    .date(today)
                    .steps(newStepCount)
                    .build();
        }
        dailyStepRepository.save(stepEntity);

        // 2. Broadcast real-time user activity STOMP update immediately to user
        try {
            UserEntity user = userProvider.findById(userId).orElse(null);
            String email = user != null ? user.getEmail() : "";
            double distance = request.getDistanceMeters() != null && request.getDistanceMeters() > 0
                    ? request.getDistanceMeters()
                    : newStepCount * 0.753;
            double calories = request.getCaloriesBurned() != null && request.getCaloriesBurned() > 0
                    ? request.getCaloriesBurned()
                    : newStepCount * 0.04;

            ActivityUpdateMessage updateMsg = ActivityUpdateMessage.builder()
                    .userId(userId)
                    .userEmail(email)
                    .date(today.toString())
                    .steps(newStepCount)
                    .distanceMeters(distance)
                    .caloriesBurned(calories)
                    .timestamp(Instant.now())
                    .build();

            leaderboardPublisher.publishUserActivityUpdate(userId, updateMsg);
        } catch (Exception e) {
            log.warn("Failed to broadcast user activity WebSocket update for userId={}: {}", userId, e.getMessage());
        }

        // 3. Register step sync with InactivityDetectionService
        try {
            inactivityDetectionService.registerStepSync(userId, newStepCount);
        } catch (Exception e) {
            log.warn("Failed to register activity step sync with InactivityDetectionService for userId={}: {}", userId, e.getMessage());
        }

        // 4. Save active Activity record log
        ActivityEntity entity = activityMapper.toEntity(request);
        entity.setUserId(userId);
        ActivityEntity savedActivity = activityProvider.save(entity);

        // 5. Broadcast real-time WebSocket leaderboard updates
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

        // 6. Broadcast real-time challenge leaderboard updates
        try {
            challengeService.recalculateAndBroadcastLeaderboards(userId);
        } catch (Exception e) {
            log.warn("Failed to broadcast challenge leaderboard update for userId={}: {}", userId, e.getMessage(), e);
        }

        return activityMapper.toResponse(savedActivity);
    }

    @Override
    public List<ActivityResponse> getUserActivities(Long userId) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate thirtyDaysAgo = today.minusDays(30);

        List<DailyStepEntity> existingSteps = dailyStepRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, thirtyDaysAgo, today);

        List<DailyStepEntity> fullList = new ArrayList<>(existingSteps);
        if (fullList.isEmpty()) {
            Optional<DailyStepEntity> todayEntity = dailyStepRepository.findByUserIdAndDate(userId, today);
            if (todayEntity.isPresent()) {
                fullList.add(todayEntity.get());
            } else {
                List<DailyStepEntity> recent = dailyStepRepository.findByUserIdOrderByDateDesc(userId);
                if (!recent.isEmpty()) {
                    fullList.add(recent.get(0));
                }
            }
        }

        return fullList.stream().map(d -> {
            Instant dayInstant = d.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
            double distanceMeters = d.getSteps() * 0.762;
            double caloriesBurned = d.getSteps() * 0.04;
            return ActivityResponse.builder()
                    .id(d.getId())
                    .userId(d.getUserId())
                    .stepCount(d.getSteps().intValue())
                    .distanceMeters(distanceMeters)
                    .caloriesBurned(caloriesBurned)
                    .startTime(dayInstant)
                    .endTime(dayInstant.plus(1, ChronoUnit.DAYS))
                    .sourceDevice("Health Connect Sensor")
                    .syncedAt(d.getUpdatedAt() != null ? d.getUpdatedAt() : dayInstant)
                    .build();
        }).collect(Collectors.toList());
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
                .orElseGet(() -> {
                    List<DailyStepEntity> recent = dailyStepRepository.findByUserIdOrderByDateDesc(userId);
                    return (!recent.isEmpty()) ? recent.get(0).getSteps() : 0L;
                });

        Double totalDistance = dailySteps * 0.753;
        Double totalCalories = dailySteps * 0.04;

        return ActivitySummaryResponse.builder()
                .userId(userId)
                .totalSteps(dailySteps)
                .totalDistanceMeters(totalDistance)
                .totalCaloriesBurned(totalCalories)
                .periodStart(startTime)
                .periodEnd(endTime)
                .activityRecordCount(1)
                .build();
    }

    @Override
    public ActivityTrendResponse getActivityTrends(Long userId) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<ActivityResponse> activities = getUserActivities(userId);
        List<ActivityEntity> activityEntities = activities.stream().map(a -> {
            return ActivityEntity.builder()
                    .userId(a.getUserId())
                    .stepCount(a.getStepCount())
                    .distanceMeters(a.getDistanceMeters())
                    .caloriesBurned(a.getCaloriesBurned())
                    .startTime(a.getStartTime())
                    .endTime(a.getEndTime())
                    .sourceDevice(a.getSourceDevice())
                    .build();
        }).collect(Collectors.toList());
        return activityTrendCalculator.calculate7DayTrend(userId, activityEntities, Instant.now());
    }
}