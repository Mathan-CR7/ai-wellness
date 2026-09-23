package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.config.AppProperties;
import com.kovanlabs.wellness.dto.ai.AIChatRequest;
import com.kovanlabs.wellness.dto.ai.AIChatResponse;
import com.kovanlabs.wellness.entity.AIConversationEntity;
import com.kovanlabs.wellness.entity.AIMessageEntity;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.entity.UserActivityStateEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.entity.enums.MessageSender;
import com.kovanlabs.wellness.exception.AiServiceException;
import com.kovanlabs.wellness.exception.AiTimeoutException;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.exception.UnauthorizedException;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.AIConversationRepository;
import com.kovanlabs.wellness.repository.AIMessageRepository;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.repository.UserActivityStateRepository;
import com.kovanlabs.wellness.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
@Transactional
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private final AIConversationRepository conversationRepository;
    private final AIMessageRepository messageRepository;
    private final AppProperties appProperties;
    private final ChatClient.Builder chatClientBuilder;
    private final UserProvider userProvider;
    private final DailyStepRepository dailyStepRepository;
    private final UserActivityStateRepository userActivityStateRepository;

    public AiServiceImpl(
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            AppProperties appProperties,
            ChatClient.Builder chatClientBuilder,
            UserProvider userProvider,
            DailyStepRepository dailyStepRepository,
            UserActivityStateRepository userActivityStateRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.appProperties = appProperties;
        this.chatClientBuilder = chatClientBuilder;
        this.userProvider = userProvider;
        this.dailyStepRepository = dailyStepRepository;
        this.userActivityStateRepository = userActivityStateRepository;
    }

    @Override
    public AIChatResponse processChat(Long userId, AIChatRequest request) {
        AIConversationEntity conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Conversation not found with id: " + request.getConversationId()));
            if (!conversation.getUserId().equals(userId)) {
                throw new UnauthorizedException("Unauthorized access to conversation.");
            }
        }
        else {
            String title = request.getPrompt().length() > 30
                    ? request.getPrompt().substring(0, 30) + "..."
                    : request.getPrompt();
            conversation = conversationRepository.save(AIConversationEntity.builder()
                    .userId(userId)
                    .title(title)
                    .build());
        }

        messageRepository.save(AIMessageEntity.builder()
                .conversationId(conversation.getId())
                .sender(MessageSender.USER)
                .content(request.getPrompt())
                .build());

        String aiTextResponse = callGemini(userId, request.getPrompt());

        AIMessageEntity aiMsg = messageRepository.save(AIMessageEntity.builder()
                .conversationId(conversation.getId())
                .sender(MessageSender.AI)
                .content(aiTextResponse)
                .build());

        return AIChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(aiMsg.getId())
                .content(aiTextResponse)
                .timestamp(aiMsg.getTimestamp())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIConversationEntity> getUserConversations(Long userId)
    {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIMessageEntity> getConversationMessages(Long conversationId, Long userId) {
        AIConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found with id: " + conversationId));
        if (!conversation.getUserId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized access to conversation.");
        }
        return messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
    }

    @Override
    public String generateInactivitySuggestion(Long userId, String fullName, long currentSteps, long inactivityMinutes, long targetGoal) {
        String systemPrompt = "You are an AI physical wellness coach. " +
                "Generate a concise, friendly, 2-3 sentence physical wellness & movement recommendation for a user who has been sedentary. " +
                "Suggest quick 10-15 minute exercises (such as shoulder rotations, wrist stretches, neck rolls, or a quick walk) appropriate for someone who has been sitting for a while. " +
                "Do NOT include markdown formatting, bullet points, or quotes. Keep it direct, practical, and encouraging.";

        String userPrompt = String.format(
                "User: %s (ID: %d)\n" +
                "Current Daily Steps: %d / %d\n" +
                "Inactivity Duration: %d minutes (%d hours %d minutes)\n" +
                "Generate a personalized movement suggestion.",
                fullName != null ? fullName : "User",
                userId,
                currentSteps,
                targetGoal,
                inactivityMinutes,
                inactivityMinutes / 60,
                inactivityMinutes % 60
        );

        try {
            String response = chatClientBuilder.build()
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            if (response != null && !response.isBlank()) {
                return response.trim();
            }
        } catch (Exception e) {
            log.warn("Spring AI call unavailable for userId={}, generating dynamic contextual suggestion: {}", userId, e.getMessage());
        }

        long remaining = Math.max(0, targetGoal - currentSteps);
        long hours = inactivityMinutes / 60;
        long mins = inactivityMinutes % 60;
        String timeStr = hours > 0 ? hours + "h " + mins + "m" : mins + "m";
        return String.format(
                "You've been inactive for %s. Take a 5-minute walk and try some shoulder rotations, wrist stretches, and light neck rolls. You have %,d steps today — %,d steps remaining for your daily goal!",
                timeStr, currentSteps, remaining
        );
    }

    private String callGemini(Long userId, String prompt) {
        try {
            String response = chatClientBuilder.build()
                    .prompt()
                    .system(appProperties.ai().systemPromptPrefix() + " User ID: " + userId)
                    .user(prompt)
                    .tools("getUserActivitySummary", "getUserActivityTrends", "getActiveChallenges")
                    .call()
                    .content();

            if (response != null && !response.isBlank()) {
                return response;
            }
        } catch (Throwable t) {
            log.warn("Gemini AI API call unavailable for userId={}, returning dynamic local fallback wellness guidance: {}", userId, t.getMessage());
        }

        return generateSmartFallbackResponse(userId, prompt);
    }

    private String generateSmartFallbackResponse(Long userId, String prompt) {
        String userName = "Athlete";
        long targetGoal = 10000L;
        long todaySteps = 0L;
        long inactivityMinutes = 0L;

        try {
            if (userProvider != null && userId != null) {
                UserEntity u = userProvider.findById(userId).orElse(null);
                if (u != null) {
                    if (u.getFullName() != null && !u.getFullName().isBlank()) {
                        userName = u.getFullName();
                    }
                }
            }
            if (dailyStepRepository != null && userId != null) {
                DailyStepEntity ds = dailyStepRepository.findByUserIdAndDate(userId, LocalDate.now()).orElse(null);
                if (ds != null && ds.getSteps() != null) {
                    todaySteps = ds.getSteps();
                }
            }
            if (userActivityStateRepository != null && userId != null) {
                UserActivityStateEntity state = userActivityStateRepository.findByUserId(userId).orElse(null);
                if (state != null && state.getLastActivityTime() != null) {
                    inactivityMinutes = Duration.between(state.getLastActivityTime(), Instant.now()).toMinutes();
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching live user metrics for AI dynamic response for userId={}: {}", userId, e.getMessage());
        }

        double distanceKm = todaySteps * 0.000762;
        double caloriesBurned = todaySteps * 0.04;
        long remainingSteps = Math.max(0, targetGoal - todaySteps);
        int progressPct = targetGoal > 0 ? (int) ((todaySteps * 100) / targetGoal) : 0;

        String p = (prompt != null) ? prompt.toLowerCase() : "";

        if (p.contains("stretch") || p.contains("neck") || p.contains("shoulder") || p.contains("office") || p.contains("break") || p.contains("desk")) {
            return String.format(
                "Hello %s! Based on your current Health Connect stats for today:\n\n" +
                "• Today's Steps: %,d / %,d steps (%d%% completed)\n" +
                "• Distance: %.2f km | Calories: %.0f kcal\n" +
                "• Inactivity Duration: %d minutes\n\n" +
                "Here is a personalized 5-minute movement break routine to relieve desk fatigue:\n" +
                "1. Neck Releases: Gently tilt your ear to shoulder for 15 seconds on each side.\n" +
                "2. Shoulder Rotations: Roll shoulders backward 10 times to unlock upper back tension.\n" +
                "3. Seated Torso Twists: Hold each side for 15 seconds to flex your spine.\n" +
                "4. Quick Step Walk: Take a 2-minute walk to add ~200 steps toward your %,d remaining steps!",
                userName, todaySteps, targetGoal, progressPct, distanceKm, caloriesBurned, inactivityMinutes, remainingSteps
            );
        }

        if (p.contains("walk") || p.contains("step") || p.contains("distance") || p.contains("km") || p.contains("goal") || p.contains("progress")) {
            return String.format(
                "Hi %s! Here is your real-time step activity summary:\n\n" +
                "• Current Steps Today: %,d steps\n" +
                "• Target Goal: %,d steps (%,d steps remaining)\n" +
                "• Goal Completion: %d%%\n" +
                "• Estimated Distance: %.2f km\n" +
                "• Calories Burned: %.0f kcal\n\n" +
                "Coach Advice: To close your remaining %,d steps, schedule a 15-minute brisk walk post-lunch or take the stairs. Every step gets you closer to your daily goal!",
                userName, todaySteps, targetGoal, remainingSteps, progressPct, distanceKm, caloriesBurned, remainingSteps
            );
        }

        if (p.contains("workout") || p.contains("exercise") || p.contains("gym") || p.contains("routine") || p.contains("train") || p.contains("burn")) {
            return String.format(
                "Hello %s! Here is a custom workout recommendation for today:\n\n" +
                "• Daily Step Context: %,d steps (%.2f km, %.0f kcal burned)\n" +
                "• Remaining Goal: %,d steps\n\n" +
                "Recommended Exercise Routine:\n" +
                "1. Warm-up (5 mins): Light arm circles, leg swings, and brisk walking.\n" +
                "2. Bodyweight Circuit (15 mins): 3 rounds of 12 Bodyweight Squats, 10 Push-ups, and a 30-second Plank hold.\n" +
                "3. Cool-down (5 mins): Deep breathing and gentle leg stretches.",
                userName, todaySteps, distanceKm, caloriesBurned, remainingSteps
            );
        }

        if (p.contains("food") || p.contains("diet") || p.contains("nutrition") || p.contains("calorie") || p.contains("water") || p.contains("drink")) {
            return String.format(
                "Hi %s! Personal Nutrition & Energy Guidance:\n\n" +
                "• Active Energy Spent Today: ~%.0f kcal across %,d steps (%.2f km)\n" +
                "• Hydration Target: Aim for 2.5–3.0 liters of water today.\n" +
                "• Fueling Tip: Pair lean proteins with complex carbohydrates post-walk to replenish muscle glycogen and sustain your energy levels!",
                userName, caloriesBurned, todaySteps, distanceKm
            );
        }

        if (p.contains("recovery") || p.contains("rest") || p.contains("sleep") || p.contains("tired") || p.contains("sore")) {
            return String.format(
                "Hello %s! Personal Recovery & Wellness Analysis:\n\n" +
                "• Today's Accumulated Activity: %,d steps (%.2f km)\n" +
                "• Sedentary Status: %d minutes since last active movement\n\n" +
                "Recovery Recommendations:\n" +
                "1. Prioritize 7–8 hours of restful sleep tonight.\n" +
                "2. Perform light static leg and calf stretching to relieve muscle tightness.\n" +
                "3. Stay hydrated to optimize metabolic recovery!",
                userName, todaySteps, distanceKm, inactivityMinutes
            );
        }

        return String.format(
            "Hello %s! As your AI Wellness Coach, here is your personalized daily update:\n\n" +
            "• Daily Step Goal: %,d / %,d steps (%d%% completed)\n" +
            "• Distance Covered: %.2f km | Active Calories: %.0f kcal\n" +
            "• Steps Remaining: %,d steps\n\n" +
            "Recommended Action: Take short 5-minute movement breaks throughout your day to hit your step target and boost your physical wellness!",
            userName, todaySteps, targetGoal, progressPct, distanceKm, caloriesBurned, remainingSteps
        );
    }
}
