package com.kovanlabs.wellness.service.impl;
import com.kovanlabs.wellness.dto.team.LeaderboardEntry;
import com.kovanlabs.wellness.dto.team.TeamLeaderboardResponse;
import com.kovanlabs.wellness.entity.DailyStepEntity;
import com.kovanlabs.wellness.entity.TeamEntity;
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
    )
    {
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

        List<UserEntity> targetUsers = userProvider.findAll();

        List<LeaderboardEntry> entries = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        for (UserEntity user : targetUsers) {
            Long todaySteps = dailyStepRepository.findByUserIdAndDate(user.getId(), today)
                    .map(DailyStepEntity::getSteps)
                    .orElse(0L);

            LocalDate startDate = startTime != null ? startTime.atZone(ZoneId.systemDefault()).toLocalDate() : today.minusDays(7);
            LocalDate endDate = endTime != null ? endTime.atZone(ZoneId.systemDefault()).toLocalDate() : today;

            Long rangeSteps = dailyStepRepository.sumStepsByUserIdAndDateRange(user.getId(), startDate, endDate);
            long totalSteps = Math.max(todaySteps, rangeSteps != null ? rangeSteps : 0L);

            Double distance = totalSteps * 0.75;
            Double calories = totalSteps * 0.04;

            String displayName = user.getFullName() != null && !user.getFullName().isBlank()
                    ? user.getFullName()
                    : user.getEmail();

            entries.add(LeaderboardEntry.builder()
                    .userId(user.getId())
                    .fullName(displayName)
                    .email(user.getEmail())
                    .totalSteps(totalSteps)
                    .totalDistanceMeters(distance)
                    .totalCaloriesBurned(calories)
                    .build());
        }

        entries.sort(Comparator.comparing(LeaderboardEntry::getTotalSteps).reversed());

        for (int i = 0; i < entries.size(); i++)
        {
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
