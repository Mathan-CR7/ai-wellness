package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.step.DailyStepResponse;
import com.kovanlabs.wellness.dto.step.StepSyncRequest;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.service.ChallengeService;
import com.kovanlabs.wellness.service.StepService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
@Transactional
public class StepServiceImpl implements StepService {

    private static final Logger log = LoggerFactory.getLogger(StepServiceImpl.class);

    private final DailyStepRepository dailyStepRepository;
    private final UserProvider userProvider;
    private final ChallengeService challengeService;

    public StepServiceImpl(
            DailyStepRepository dailyStepRepository,
            UserProvider userProvider,
            @Lazy ChallengeService challengeService
    ) {
        this.dailyStepRepository = dailyStepRepository;
        this.userProvider = userProvider;
        this.challengeService = challengeService;
    }

    @Override
    public DailyStepResponse syncSteps(Long userId, StepSyncRequest request) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        LocalDate targetDate = request.getDate() != null ? request.getDate() : LocalDate.now(ZoneId.systemDefault());
        Long steps = request.getSteps();

        Optional<DailyStepEntity> existingOpt = dailyStepRepository.findByUserIdAndDate(userId, targetDate);

        DailyStepEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            log.info("Updating existing daily step record for userId={} on date={}: previousSteps={}, newSteps={}",
                    userId, targetDate, entity.getSteps(), steps);
            entity.setSteps(steps);
        } else {
            log.info("Creating new daily step record for userId={} on date={}: steps={}",
                    userId, targetDate, steps);
            entity = DailyStepEntity.builder()
                    .userId(userId)
                    .date(targetDate)
                    .steps(steps)
                    .build();
        }

        DailyStepEntity savedEntity = dailyStepRepository.save(entity);

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
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return getStepsForDate(userId, today);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyStepResponse getStepsForDate(Long userId, LocalDate date)
        {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        DailyStepEntity entity = dailyStepRepository.findByUserIdAndDate(userId, date)
                .orElseGet(() -> DailyStepEntity.builder()
                        .userId(userId)
                        .date(date)
                        .steps(0L)
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