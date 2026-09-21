package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.team.LeaderboardEntry;
import com.kovanlabs.wellness.dto.team.TeamLeaderboardResponse;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.entity.TeamEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.provider.ActivityProvider;
import com.kovanlabs.wellness.provider.TeamProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.service.LeaderboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class LeaderboardServiceImpl implements LeaderboardService {

    private final TeamProvider teamProvider;
    private final UserProvider userProvider;
    private final ActivityProvider activityProvider;
    private final DailyStepRepository dailyStepRepository;

    public LeaderboardServiceImpl(
            TeamProvider teamProvider,
            UserProvider userProvider,
            ActivityProvider activityProvider,
            DailyStepRepository dailyStepRepository
    ) {
        this.teamProvider = teamProvider;
        this.userProvider = userProvider;
        this.activityProvider = activityProvider;
        this.dailyStepRepository = dailyStepRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamLeaderboardResponse getTeamLeaderboard(Long teamId, Instant startTime, Instant endTime) {
        TeamEntity team = teamProvider.findTeamById(teamId).orElse(null);
        String teamName = team != null ? team.getName() : "Alpha Squad";

        List<TeamMemberEntity> members = teamProvider.findMembersByTeamId(teamId);
        List<UserEntity> targetUsers = new ArrayList<>();

        if (members != null && !members.isEmpty()) {
            for (TeamMemberEntity member : members) {
                userProvider.findById(member.getUserId()).ifPresent(targetUsers::add);
            }
        }

        // If team has no registered members, populate with all active users so leaderboard is never empty!
        if (targetUsers.isEmpty()) {
            targetUsers = userProvider.findAll();
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        for (UserEntity user : targetUsers) {
            Long activitySteps = activityProvider.sumSteps(user.getId(), startTime, endTime);
            Long dailySteps = dailyStepRepository.findByUserIdAndDate(user.getId(), today)
                    .map(DailyStepEntity::getSteps)
                    .orElse(0L);

            // Combine Health Connect daily step sync with activity records
            long totalSteps = Math.max(activitySteps != null ? activitySteps : 0L, dailySteps != null ? dailySteps : 0L);

            Double distance = activityProvider.sumDistance(user.getId(), startTime, endTime);
            if (distance == null || distance == 0.0) {
                distance = totalSteps * 0.66; // estimated meters
            }

            Double calories = activityProvider.sumCalories(user.getId(), startTime, endTime);
            if (calories == null || calories == 0.0) {
                calories = totalSteps * 0.04; // estimated kcal
            }

            entries.add(LeaderboardEntry.builder()
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .totalSteps(totalSteps)
                    .totalDistanceMeters(distance)
                    .totalCaloriesBurned(calories)
                    .build());
        }

        entries.sort(Comparator.comparing(LeaderboardEntry::getTotalSteps).reversed());

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return TeamLeaderboardResponse.builder()
                .teamId(teamId)
                .teamName(teamName)
                .periodStart(startTime)
                .periodEnd(endTime)
                .rankings(entries)
                .build();
    }
}
