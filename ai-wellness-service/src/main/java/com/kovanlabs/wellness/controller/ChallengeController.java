package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.challenge.*;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.service.ChallengeService;
import com.kovanlabs.wellness.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/challenges")
@Tag(name = "Challenge Management", description = "Endpoints for community wellness challenges, memberships, and dynamic leaderboards")
@SecurityRequirement(name = "bearerAuth")
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UserService userService;

    public ChallengeController(ChallengeService challengeService, UserService userService) {
        this.challengeService = challengeService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a new wellness challenge")
    public ResponseEntity<ChallengeResponse> createChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChallengeCreateRequest request
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        ChallengeResponse response = challengeService.createChallenge(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/active")
    @Operation(summary = "List all active wellness challenges")
    public ResponseEntity<List<ChallengeResponse>> getActiveChallenges() {
        List<ChallengeResponse> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(challenges);
    }

    @GetMapping
    @Operation(summary = "List all active wellness challenges (alias for /active)")
    public ResponseEntity<List<ChallengeResponse>> getAllChallenges() {
        List<ChallengeResponse> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get challenge details by challenge ID")
    public ResponseEntity<ChallengeResponse> getChallengeById(@PathVariable Long id) {
        ChallengeResponse response = challengeService.getChallengeById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join a challenge for the authenticated user")
    public ResponseEntity<ChallengeMemberResponse> joinChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        ChallengeMemberResponse response = challengeService.joinChallenge(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/leave")
    @Operation(summary = "Leave a challenge for the authenticated user")
    public ResponseEntity<Void> leaveChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        challengeService.leaveChallenge(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List all members participating in a challenge")
    public ResponseEntity<List<ChallengeMemberResponse>> getChallengeMembers(@PathVariable Long id) {
        List<ChallengeMemberResponse> members = challengeService.getChallengeMembers(id);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{id}/leaderboard")
    @Operation(summary = "Get real-time dynamic leaderboard for a challenge based on actual step records")
    public ResponseEntity<ChallengeLeaderboardResponse> getChallengeLeaderboard(@PathVariable Long id) {
        ChallengeLeaderboardResponse leaderboard = challengeService.getChallengeLeaderboard(id);
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Get user progress and completion percentage for a challenge")
    public ResponseEntity<ChallengeProgressResponse> getChallengeProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        ChallengeProgressResponse progress = challengeService.getChallengeProgress(id, user.getId());
        return ResponseEntity.ok(progress);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete challenge by ID")
    public ResponseEntity<Void> deleteChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        UserProfileResponse requester = userService.getUserProfileByEmail(userDetails.getUsername());
        challengeService.deleteChallenge(id, requester.getId());
        return ResponseEntity.noContent().build();
    }
}
