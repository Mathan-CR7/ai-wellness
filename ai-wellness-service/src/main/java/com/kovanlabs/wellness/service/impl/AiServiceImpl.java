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
    public List<AIConversationEntity> getUserConversations(Long userId) {
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

            if (response == null || response.isBlank()) {
                log.error("Gemini returned a blank response for userId={}", userId);
                throw new AiServiceException(
                        "The AI service returned an empty response. Please try again.");
            }

            return response;

        } catch (AiServiceException | AiTimeoutException rethrow) {
            throw rethrow;

        } catch (Exception e) {
            if (isTimeoutCause(e)) {
                log.error("Gemini request timed out for userId={}: {}", userId, e.getMessage());
                throw new AiTimeoutException(
                        "The AI service did not respond in time. Please try again in a moment.", e);
            }

            log.warn("Gemini API call unavailable for userId={}, returning local fallback wellness guidance: {}", userId, e.getMessage());
            return "Based on your activity profile: To reach your 10,000 daily steps goal, try taking a 15-minute walk after meals, taking the stairs, and scheduling short movement breaks throughout your day!";
        }
    }

    private boolean isTimeoutCause(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof TimeoutException
                    || cause instanceof java.net.SocketTimeoutException
                    || (cause.getMessage() != null && cause.getMessage().toLowerCase().contains("timeout"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
