package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.team.TeamLeaderboardResponse;
import com.kovanlabs.wellness.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Leaderboard", description = "Real-time team leaderboard rankings")
@SecurityRequirement(name = "bearerAuth")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/{id}/leaderboard")
    @Operation(summary = "Get real-time step rankings for team members within date range")
    public ResponseEntity<TeamLeaderboardResponse> getTeamLeaderboard(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    ) {
        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = startTime != null ? startTime : end.minus(7, ChronoUnit.DAYS);

        TeamLeaderboardResponse response = leaderboardService.getTeamLeaderboard(id, start, end);
        return ResponseEntity.ok(response);
    }
}
