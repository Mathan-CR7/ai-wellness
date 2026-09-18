package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.team.LeaderboardEntry;
import com.kovanlabs.wellness.dto.team.TeamLeaderboardResponse;
import com.kovanlabs.wellness.entity.TeamEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.provider.ActivityProvider;
import com.kovanlabs.wellness.provider.TeamProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.service.LeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class LeaderboardServiceImpl implements LeaderboardService {

    private final TeamProvider teamProvider;
    private final UserProvider userProvider;
    private final ActivityProvider activityProvider;

    public LeaderboardServiceImpl(TeamProvider teamProvider, UserProvider userProvider, ActivityProvider activityProvider) {
        this.teamProvider = teamProvider;
        this.userProvider = userProvider;
        this.activityProvider = activityProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamLeaderboardResponse getTeamLeaderboard(Long teamId, Instant startTime, Instant endTime) {
        TeamEntity team = teamProvider.findTeamById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));

        List<TeamMemberEntity> members = teamProvider.findMembersByTeamId(teamId);
        List<LeaderboardEntry> entries = new ArrayList<>();

        for (TeamMemberEntity member : members) {
            UserEntity user = userProvider.findById(member.getUserId()).orElse(null);
            if (user == null) continue;

            Long steps = activityProvider.sumSteps(user.getId(), startTime, endTime);
            Double distance = activityProvider.sumDistance(user.getId(), startTime, endTime);
            Double calories = activityProvider.sumCalories(user.getId(), startTime, endTime);

            entries.add(LeaderboardEntry.builder()
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .totalSteps(steps != null ? steps : 0L)
                    .totalDistanceMeters(distance != null ? distance : 0.0)
                    .totalCaloriesBurned(calories != null ? calories : 0.0)
                    .build());
        }
        entries.sort(Comparator.comparing(LeaderboardEntry::getTotalSteps).reversed());

        for (int i = 0; i < entries.size(); i++)
        {
            entries.get(i).setRank(i + 1);
        }
        return TeamLeaderboardResponse.builder()
                .teamId(teamId)
                .teamName(team.getName())
                .periodStart(startTime)
                .periodEnd(endTime)
                .rankings(entries)
                .build();
    }
}
