package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.ai.AIChatRequest;
import com.kovanlabs.wellness.dto.ai.AIChatResponse;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.entity.AIConversationEntity;
import com.kovanlabs.wellness.entity.AIMessageEntity;
import com.kovanlabs.wellness.service.AiService;
import com.kovanlabs.wellness.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Wellness Assistant", description = "Endpoints for interacting with the Gemini-powered AI wellness assistant")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiService aiService;
    private final UserService userService;

    public AiController(AiService aiService, UserService userService) {
        this.aiService = aiService;
        this.userService = userService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI", description = "Send a prompt to the AI assistant and get a personalized response based on user data")
    public ResponseEntity<AIChatResponse> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AIChatRequest request
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        return ResponseEntity.ok(aiService.processChat(user.getId(), request));
    }

    @GetMapping("/conversations")
    @Operation(summary = "Get User Conversations", description = "Retrieve a list of all AI chat conversations for the current user")
    public ResponseEntity<List<AIConversationEntity>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        return ResponseEntity.ok(aiService.getUserConversations(user.getId()));
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "Get Conversation Messages", description = "Retrieve all messages for a specific AI conversation")
    public ResponseEntity<List<AIMessageEntity>> getConversationMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long conversationId
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        return ResponseEntity.ok(aiService.getConversationMessages(conversationId, user.getId()));
    }
}
