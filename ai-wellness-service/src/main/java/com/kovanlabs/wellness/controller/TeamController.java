package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.team.TeamCreateRequest;
import com.kovanlabs.wellness.dto.team.TeamMemberResponse;
import com.kovanlabs.wellness.dto.team.TeamResponse;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.service.TeamService;
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
@RequestMapping("/api/teams")
@Tag(name = "Team Management", description = "Endpoints for creating teams, joining via invite code, and managing memberships")
@SecurityRequirement(name = "bearerAuth")
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;

    public TeamController(TeamService teamService, UserService userService) {
        this.teamService = teamService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TeamCreateRequest request
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        TeamResponse response = teamService.createTeam(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get team details by team ID")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long id) {
        TeamResponse response = teamService.getTeamById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    @Operation(summary = "Join team using an 8-character invite code")
    public ResponseEntity<TeamResponse> joinTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String inviteCode
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        TeamResponse response = teamService.joinTeamByInviteCode(user.getId(), inviteCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get all teams the current user belongs to")
    public ResponseEntity<List<TeamResponse>> getMyTeams(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        List<TeamResponse> teams = teamService.getUserTeams(user.getId());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Get members of a team")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable Long id) {
        List<TeamMemberResponse> members = teamService.getTeamMembers(id);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @Operation(summary = "Remove member from team")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @PathVariable Long memberId
    ) {
        UserProfileResponse requester = userService.getUserProfileByEmail(userDetails.getUsername());
        teamService.removeMember(id, requester.getId(), memberId);
        return ResponseEntity.noContent().build();
    }
}
