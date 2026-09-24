package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.activity.ActivityUpdateMessage;
import com.kovanlabs.wellness.dto.step.DailyStepResponse;
import com.kovanlabs.wellness.dto.step.StepSyncRequest;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.service.ChallengeService;
import com.kovanlabs.wellness.service.InactivityDetectionService;
import com.kovanlabs.wellness.service.StepService;
import com.kovanlabs.wellness.service.WebSocketLeaderboardPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StepServiceImpl implements StepService {

    private static final Logger log = LoggerFactory.getLogger(StepServiceImpl.class);

    private final DailyStepRepository dailyStepRepository;
    private final UserProvider userProvider;
    private final ChallengeService challengeService;
    private final InactivityDetectionService inactivityDetectionService;
    private final WebSocketLeaderboardPublisher leaderboardPublisher;

    public StepServiceImpl(
            DailyStepRepository dailyStepRepository,
            UserProvider userProvider,
            @Lazy ChallengeService challengeService,
            @Lazy InactivityDetectionService inactivityDetectionService,
            WebSocketLeaderboardPublisher leaderboardPublisher
    ) {
        this.dailyStepRepository = dailyStepRepository;
        this.userProvider = userProvider;
        this.challengeService = challengeService;
        this.inactivityDetectionService = inactivityDetectionService;
        this.leaderboardPublisher = leaderboardPublisher;
    }

    @Override
    public DailyStepResponse syncSteps(Long userId, StepSyncRequest request) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        LocalDate targetDate = request.getParsedDate();
        LocalDate serverToday = LocalDate.now(ZoneId.systemDefault());
        Long steps = request.getEffectiveSteps();

        DailyStepEntity savedEntity = null;

        for (LocalDate dateToUpdate : List.of(targetDate, serverToday)) {
            try {
                Optional<DailyStepEntity> existingOpt = dailyStepRepository.findByUserIdAndDate(userId, dateToUpdate);
                DailyStepEntity entity;
                if (existingOpt.isPresent()) {
                    entity = existingOpt.get();
                    entity.setSteps(steps);
                } else {
                    entity = DailyStepEntity.builder()
                            .userId(userId)
                            .date(dateToUpdate)
                            .steps(steps)
                            .build();
                }
                DailyStepEntity res = dailyStepRepository.save(entity);
                if (dateToUpdate.equals(targetDate)) {
                    savedEntity = res;
                }
            } catch (Exception e) {
                log.warn("Failed to save daily step entity for date {}: {}", dateToUpdate, e.getMessage());
            }
        }

        if (savedEntity == null) {
            savedEntity = dailyStepRepository.findByUserIdAndDate(userId, targetDate)
                    .orElseGet(() -> DailyStepEntity.builder().userId(userId).date(targetDate).steps(steps).build());
        }

        // Broadcast real-time user activity STOMP update to WebSocket topic
        try {
            UserEntity user = userProvider.findById(userId).orElse(null);
            String email = user != null ? user.getEmail() : "";
            double distance = steps * 0.753;
            double calories = steps * 0.04;

            ActivityUpdateMessage updateMsg = ActivityUpdateMessage.builder()
                    .userId(userId)
                    .userEmail(email)
                    .date(targetDate.toString())
                    .steps(steps)
                    .distanceMeters(distance)
                    .caloriesBurned(calories)
                    .timestamp(Instant.now())
                    .build();

            leaderboardPublisher.publishUserActivityUpdate(userId, updateMsg);
        } catch (Exception e) {
            log.warn("Failed to broadcast user activity STOMP update for userId={}: {}", userId, e.getMessage());
        }

        try {
            inactivityDetectionService.registerStepSync(userId, steps);
        } catch (Exception e) {
            log.warn("Failed to register step sync with InactivityDetectionService for userId={}: {}", userId, e.getMessage());
        }

        try {
            challengeService.recalculateAndBroadcastLeaderboards(userId);
        } catch (Exception e) {
            log.warn("Failed to broadcast challenge leaderboard update for userId={}: {}", userId, e.getMessage(), e);
        }

        return toResponse(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyStepResponse getTodaySteps(Long userId) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Optional<DailyStepEntity> todayOpt = dailyStepRepository.findByUserIdAndDate(userId, today);
        if (todayOpt.isPresent()) {
            return toResponse(todayOpt.get());
        }

        // Timezone safety: retrieve most recent active daily step record for user if date calculation differs slightly across timezones
        List<DailyStepEntity> recent = dailyStepRepository.findByUserIdOrderByDateDesc(userId);
        if (!recent.isEmpty()) {
            return toResponse(recent.get(0));
        }

        return DailyStepResponse.builder()
                .userId(userId)
                .date(today)
                .steps(0L)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DailyStepResponse getStepsForDate(Long userId, LocalDate date) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        Long dailySteps = dailyStepRepository.findByUserIdAndDate(userId, date)
                .map(DailyStepEntity::getSteps)
                .orElse(0L);

        DailyStepEntity entity = dailyStepRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> DailyStepEntity.builder()
                        .userId(userId)
                        .date(date)
                        .steps(dailySteps)
                        .build());

        return toResponse(entity);
    }

    private DailyStepResponse toResponse(DailyStepEntity entity) {
        return DailyStepResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .date(entity.getDate())
                .steps(entity.getSteps())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}