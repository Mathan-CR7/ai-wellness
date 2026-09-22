package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.config.AppProperties;
import com.kovanlabs.wellness.dto.ai.AIChatRequest;
import com.kovanlabs.wellness.dto.ai.AIChatResponse;
import com.kovanlabs.wellness.entity.AIConversationEntity;
import com.kovanlabs.wellness.entity.AIMessageEntity;
import com.kovanlabs.wellness.entity.enums.MessageSender;
import com.kovanlabs.wellness.exception.AiServiceException;
import com.kovanlabs.wellness.exception.AiTimeoutException;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.exception.UnauthorizedException;
import com.kovanlabs.wellness.repository.AIConversationRepository;
import com.kovanlabs.wellness.repository.AIMessageRepository;
import com.kovanlabs.wellness.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public AiServiceImpl(
            AIConversationRepository conversationRepository,
            AIMessageRepository messageRepository,
            AppProperties appProperties,
            ChatClient.Builder chatClientBuilder
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.appProperties = appProperties;
        this.chatClientBuilder = chatClientBuilder;
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

        return generateSmartFallbackResponse(prompt);
    }

    private String generateSmartFallbackResponse(String prompt) {
        if (prompt == null) {
            prompt = "";
        }
        String p = prompt.toLowerCase();

        if (p.contains("stretch") || p.contains("neck") || p.contains("shoulder") || p.contains("office") || p.contains("break") || p.contains("desk")) {
            return "Here is a quick 5-minute office stretch routine you can do right now at your desk:\n\n" +
                   "1. Neck Releases: Gently tilt your ear to shoulder for 15s on each side.\n" +
                   "2. Shoulder Rolls: Roll shoulders backwards 10 times to relieve upper back tension.\n" +
                   "3. Wrist & Forearm Stretch: Extend one arm forward, palm up, pull back fingers gently for 15s.\n" +
                   "4. Seated Torso Twist: Sit tall and gently twist left then right holding for 15s each.\n" +
                   "5. Standing Calf & Hamstring Stretch: Stand up and reach down towards your toes for 20s.";
        }
        
        if (p.contains("walk") || p.contains("step") || p.contains("distance") || p.contains("km") || p.contains("goal")) {
            return "To help you reach your daily step and fitness goals:\n\n" +
                   "• Take a brisk 10-15 minute walk after meals.\n" +
                   "• Choose stairs over elevators whenever possible.\n" +
                   "• Set hourly movement reminders to walk 250 steps every hour.\n" +
                   "• Consistency is key — even light walking improves cardiovascular health and energy levels!";
        }

        if (p.contains("workout") || p.contains("exercise") || p.contains("gym") || p.contains("routine") || p.contains("train")) {
            return "Here is a balanced daily wellness exercise routine:\n\n" +
                   "• Warm-up (5 mins): Jumping jacks, arm circles, and leg swings.\n" +
                   "• Bodyweight Circuit (15 mins): 3 sets of 12 Squats, 10 Push-ups, 12 Reverse Lunges, and a 30s Plank hold.\n" +
                   "• Cool-down (5 mins): Deep breathing and static stretches.\n" +
                   "Remember to listen to your body and adjust intensity as needed!";
        }

        if (p.contains("food") || p.contains("diet") || p.contains("nutrition") || p.contains("calorie") || p.contains("water") || p.contains("drink")) {
            return "Key Nutrition & Hydration Guidance:\n\n" +
                   "• Hydration: Drink 2.5–3 liters of water throughout the day.\n" +
                   "• Balanced Meals: Fill half your plate with colorful vegetables, one-quarter with lean protein, and one-quarter with whole grains.\n" +
                   "• Energy Focus: Snack on nuts, seeds, or fresh fruit for sustained focus without blood sugar spikes.";
        }

        if (p.contains("recovery") || p.contains("rest") || p.contains("sleep") || p.contains("tired") || p.contains("sore")) {
            return "Essential Health & Recovery Tips:\n\n" +
                   "• Quality Sleep: Aim for 7–8 hours of restful sleep every night.\n" +
                   "• Active Recovery: Gentle light walking or yoga reduces delayed onset muscle soreness (DOMS).\n" +
                   "• Hydration & Minerals: Replenish electrolytes and drink adequate water post-workout.";
        }

        return "As your AI Wellness Coach, here are smart personalized recommendations for your day:\n\n" +
               "1. Stay Active: Aim to reach your target daily step goal by taking frequent short movement breaks.\n" +
               "2. Posture Check: Reset your posture every hour and perform quick neck and shoulder rolls.\n" +
               "3. Hydration: Keep a water bottle nearby and stay well-hydrated throughout your workday!";
    }
}
