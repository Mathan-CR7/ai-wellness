package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.challenge.ChallengeCreateRequest;
import com.kovanlabs.wellness.dto.challenge.ChallengeProgressResponse;
import com.kovanlabs.wellness.dto.challenge.ChallengeResponse;
import com.kovanlabs.wellness.dto.team.TeamCreateRequest;
import com.kovanlabs.wellness.dto.team.TeamLeaderboardResponse;
import com.kovanlabs.wellness.dto.team.TeamResponse;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.dto.auth.UserRegistrationRequest;
import com.kovanlabs.wellness.entity.enums.ChallengeTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Phase6LeaderboardTests {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private ChallengeService challengeService;

    @Test
    @DisplayName("Test Team Real-Time Leaderboard Ranking Sorting")
    void testTeamLeaderboardRanking() {
        UserProfileResponse user1 = userService.registerUser(UserRegistrationRequest.builder()
                .email("leader1@example.com").password("Pass123!").fullName("Leader One").build());

        UserProfileResponse user2 = userService.registerUser(UserRegistrationRequest.builder()
                .email("runner2@example.com").password("Pass123!").fullName("Runner Two").build());

        TeamResponse team = teamService.createTeam(user1.getId(), TeamCreateRequest.builder().name("Speedsters").build());
        teamService.joinTeamByInviteCode(user2.getId(), team.getInviteCode());

        Instant now = Instant.now();
        Instant start = now.minus(2, ChronoUnit.HOURS);

        // User 1 syncs 4000 steps
        activityService.syncActivity(user1.getId(), ActivitySyncRequest.builder()
                .stepCount(4000).distanceMeters(2800.0).caloriesBurned(200.0)
                .startTime(start).endTime(now).sourceDevice("ANDROID_HEALTH_CONNECT").build());

        // User 2 syncs 9000 steps
        activityService.syncActivity(user2.getId(), ActivitySyncRequest.builder()
                .stepCount(9000).distanceMeters(6500.0).caloriesBurned(450.0)
                .startTime(start).endTime(now).sourceDevice("ANDROID_HEALTH_CONNECT").build());

        TeamLeaderboardResponse leaderboard = leaderboardService.getTeamLeaderboard(team.getId(), now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));

        assertNotNull(leaderboard);
        assertEquals(2, leaderboard.getRankings().size());

        // User 2 should be Rank 1 (9000 steps)
        assertEquals(1, leaderboard.getRankings().get(0).getRank());
        assertEquals(user2.getId(), leaderboard.getRankings().get(0).getUserId());
        assertEquals(9000L, leaderboard.getRankings().get(0).getTotalSteps());

        // User 1 should be Rank 2 (4000 steps)
        assertEquals(2, leaderboard.getRankings().get(1).getRank());
        assertEquals(user1.getId(), leaderboard.getRankings().get(1).getUserId());
        assertEquals(4000L, leaderboard.getRankings().get(1).getTotalSteps());
    }

    @Test
    @DisplayName("Test Challenge Progress Percentage Calculation")
    void testChallengeProgressCalculation() {
        UserProfileResponse creator = userService.registerUser(UserRegistrationRequest.builder()
                .email("creator@example.com").password("Pass123!").fullName("Challenge Creator").build());

        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(6, ChronoUnit.DAYS);

        ChallengeResponse challenge = challengeService.createChallenge(creator.getId(), ChallengeCreateRequest.builder()
                .title("10k Step Goal")
                .targetType(ChallengeTargetType.STEPS)
                .targetValue(10000.0)
                .startDate(start)
                .endDate(end)
                .build());

        // Sync 5000 steps
        activityService.syncActivity(creator.getId(), ActivitySyncRequest.builder()
                .stepCount(5000).distanceMeters(3500.0).caloriesBurned(250.0)
                .startTime(Instant.now().minus(2, ChronoUnit.HOURS)).endTime(Instant.now())
                .sourceDevice("ANDROID_HEALTH_CONNECT").build());

        ChallengeProgressResponse progress = challengeService.getChallengeProgress(challenge.getId(), creator.getId());

        assertNotNull(progress);
        assertEquals(5000.0, progress.getCurrentValue());
        assertEquals(50.0, progress.getProgressPercent());
        assertFalse(progress.getIsCompleted());
    }
}
