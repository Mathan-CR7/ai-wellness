package com.kovanlabs.wellness.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeLeaderboardResponse {

    private Long challengeId;
    private String challengeTitle;
    private Double targetValue;
    private List<LeaderboardEntryDto> leaderboard;
    private Instant calculatedAt;
}
