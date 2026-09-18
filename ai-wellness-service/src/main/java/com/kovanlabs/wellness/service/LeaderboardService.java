package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.team.TeamLeaderboardResponse;

import java.time.Instant;

public interface LeaderboardService {

    TeamLeaderboardResponse getTeamLeaderboard(Long teamId, Instant startTime, Instant endTime);
}
