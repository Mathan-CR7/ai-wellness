package com.kovanlabs.wellness.dto.team;

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
public class TeamLeaderboardResponse {

    private Long teamId;

    private String teamName;

    private Instant periodStart;

    private Instant periodEnd;

    private List<LeaderboardEntry> rankings;
}
