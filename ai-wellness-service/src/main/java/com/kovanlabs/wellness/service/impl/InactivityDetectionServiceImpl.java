package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.ai.InactivitySuggestionMessage;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.entity.UserActivityStateEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.repository.UserActivityStateRepository;
import com.kovanlabs.wellness.service.AiService;
import com.kovanlabs.wellness.service.InactivityDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class InactivityDetectionServiceImpl implements InactivityDetectionService {

    private static final Logger log = LoggerFactory.getLogger(InactivityDetectionServiceImpl.class);

    private final UserActivityStateRepository userActivityStateRepository;
    private final UserProvider userProvider;
    private final DailyStepRepository dailyStepRepository;
    private final AiService aiService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.inactivity.min-duration-minutes:120}")
    private long minDurationMinutes;

    @Value("${app.inactivity.max-duration-minutes:180}")
    private long maxDurationMinutes;

    public InactivityDetectionServiceImpl(
            UserActivityStateRepository userActivityStateRepository,
            UserProvider userProvider,
            DailyStepRepository dailyStepRepository,
            AiService aiService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.userActivityStateRepository = userActivityStateRepository;
        this.userProvider = userProvider;
        this.dailyStepRepository = dailyStepRepository;
        this.aiService = aiService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void registerStepSync(Long userId, long currentSteps) {
        Optional<UserActivityStateEntity> stateOpt = userActivityStateRepository.findByUserId(userId);
        UserActivityStateEntity state;

        if (stateOpt.isPresent()) {
            state = stateOpt.get();
            long previousSteps = state.getLastStepCount() != null ? state.getLastStepCount() : 0L;

            // REAL PHYSICAL MOVEMENT CHECK: Reset inactivity ONLY if steps actually increased!
            if (currentSteps > previousSteps) {
                log.info("Real physical movement detected for userId={}: steps increased from {} to {}. Resetting inactivity timer.",
                        userId, previousSteps, currentSteps);
                state.setLastStepCount(currentSteps);
                state.setLastActivityTime(Instant.now());
                state.setNotificationSent(false);
                state.setNotificationSentAt(null);
            } else {
                log.debug("Routine sync for userId={}: steps unchanged ({}). Keeping lastActivityTime unchanged.",
                        userId, currentSteps);
            }
        } else {
            log.info("Initializing UserActivityState for userId={} with initial steps={}", userId, currentSteps);
            state = UserActivityStateEntity.builder()
                    .userId(userId)
                    .lastActivityTime(Instant.now())
                    .lastStepCount(currentSteps)
                    .notificationSent(false)
                    .notificationSentAt(null)
                    .build();
        }

        userActivityStateRepository.save(state);
    }

    @Override
    public void evaluateInactivityForUser(Long userId) {
        UserEntity user = userProvider.findById(userId).orElse(null);
        if (user == null) return;

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Long todaySteps = dailyStepRepository.findByUserIdAndDate(userId, today)
                .map(DailyStepEntity::getSteps)
                .orElse(0L);

        Optional<UserActivityStateEntity> stateOpt = userActivityStateRepository.findByUserId(userId);
        UserActivityStateEntity state;

        if (stateOpt.isPresent()) {
            state = stateOpt.get();
        } else {
            state = UserActivityStateEntity.builder()
                    .userId(userId)
                    .lastActivityTime(Instant.now())
                    .lastStepCount(todaySteps)
                    .notificationSent(false)
                    .build();
            state = userActivityStateRepository.save(state);
        }

        Instant lastActivity = state.getLastActivityTime() != null ? state.getLastActivityTime() : Instant.now();
        long inactivityMinutes = Duration.between(lastActivity, Instant.now()).toMinutes();

        log.debug("Evaluating inactivity for userId={}: inactive for {} minutes (threshold: {} mins, notificationSent: {})",
                userId, inactivityMinutes, minDurationMinutes, state.getNotificationSent());

        // TRIGGER CONDITION: Inactivity >= configured min duration AND notification not already sent
        if (inactivityMinutes >= minDurationMinutes && !Boolean.TRUE.equals(state.getNotificationSent())) {
            log.info("Inactivity threshold reached for userId={} (inactive {} mins >= {} mins min). Triggering Spring AI...",
                    userId, inactivityMinutes, minDurationMinutes);

            // 1. Invoke Spring AI to generate dynamic, personalized recommendation
            String aiSuggestion = aiService.generateInactivitySuggestion(
                    userId,
                    user.getFullName(),
                    todaySteps,
                    inactivityMinutes,
                    10000L
            );

            // 2. Persist notification state to MySQL so backend restarts never cause duplicate alerts
            state.setNotificationSent(true);
            state.setNotificationSentAt(Instant.now());
            userActivityStateRepository.save(state);

            // 3. Deliver via WebSocket / STOMP
            InactivitySuggestionMessage msg = InactivitySuggestionMessage.builder()
                    .userId(userId)
                    .userEmail(user.getEmail())
                    .suggestion(aiSuggestion)
                    .inactivityMinutes(inactivityMinutes)
                    .currentSteps(todaySteps)
                    .timestamp(Instant.now())
                    .build();

            try {
                messagingTemplate.convertAndSend("/topic/users/" + userId + "/inactivity-suggestion", msg);
                messagingTemplate.convertAndSend("/topic/inactivity-suggestions", msg);
                log.info("Successfully delivered AI inactivity suggestion via STOMP to userId={}", userId);
            } catch (Exception e) {
                log.error("Failed to broadcast WebSocket STOMP notification for userId={}: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    @Scheduled(fixedDelayString = "${app.inactivity.check-interval-ms:60000}")
    public void checkAllUsersInactivity() {
        try {
            List<UserEntity> users = userProvider.findAll();
            for (UserEntity user : users) {
                evaluateInactivityForUser(user.getId());
            }
        } catch (Exception e)
        {
            log.error("Error in scheduled inactivity check: {}", e.getMessage(), e);
        }
    }
}
