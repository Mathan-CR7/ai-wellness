package com.kovanlabs.wellness.config;

import com.kovanlabs.wellness.entity.*;
import com.kovanlabs.wellness.entity.enums.ChallengeTargetType;
import com.kovanlabs.wellness.entity.enums.TeamMemberRole;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Seeds initial demo data for local development testing so the web dashboard
 * and leaderboards immediately present real step metrics, active challenges, and team data.
 */
@Component
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserProvider userProvider;
    private final DailyStepRepository dailyStepRepository;
    private final ActivityRepository activityRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeMemberRepository challengeMemberRepository;

    public DevDataSeeder(
            UserProvider userProvider,
            DailyStepRepository dailyStepRepository,
            ActivityRepository activityRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            ChallengeRepository challengeRepository,
            ChallengeMemberRepository challengeMemberRepository
    ) {
        this.userProvider = userProvider;
        this.dailyStepRepository = dailyStepRepository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.challengeRepository = challengeRepository;
        this.challengeMemberRepository = challengeMemberRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking & seeding development activity & challenge data...");

        List<UserEntity> users = userProvider.findAll();
        if (users.isEmpty()) {
            log.info("No registered users found yet for seeding.");
            return;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        int[] past7DaysSteps = {6800, 8100, 7200, 9500, 6400, 8200, 7450};

        for (UserEntity user : users) {
            Long userId = user.getId();

            // Seed past 7 days of step data if missing
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                long stepCount = past7DaysSteps[6 - i];

                if (dailyStepRepository.findByUserIdAndDate(userId, date).isEmpty()) {
                    dailyStepRepository.save(DailyStepEntity.builder()
                            .userId(userId)
                            .date(date)
                            .steps(stepCount)
                            .build());

                    Instant startTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant().plus(8, ChronoUnit.HOURS);
                    Instant endTime = startTime.plus(10, ChronoUnit.HOURS);

                    activityRepository.save(ActivityEntity.builder()
                            .userId(userId)
                            .stepCount((int) stepCount)
                            .distanceMeters(stepCount * 0.762)
                            .caloriesBurned(stepCount * 0.04)
                            .startTime(startTime)
                            .endTime(endTime)
                            .sourceDevice("ANDROID_HEALTH_CONNECT")
                            .build());
                }
            }
        }

        // Seed Team "Alpha Squad" if none exists
        UserEntity primaryUser = users.get(0);
        TeamEntity team;
        if (teamRepository.findAll().isEmpty()) {
            team = teamRepository.save(TeamEntity.builder()
                    .name("Alpha Squad")
                    .description("The elite corporate step and wellness challenge squad!")
                    .inviteCode("ALPHA100")
                    .ownerId(primaryUser.getId())
                    .build());

            log.info("Created default team: Alpha Squad (Invite Code: ALPHA100)");
        } else {
            team = teamRepository.findAll().get(0);
        }

        // Add all users to team if not already members
        for (UserEntity u : users) {
            if (!teamMemberRepository.existsByTeamIdAndUserId(team.getId(), u.getId())) {
                teamMemberRepository.save(TeamMemberEntity.builder()
                        .teamId(team.getId())
                        .userId(u.getId())
                        .role(u.getId().equals(primaryUser.getId()) ? TeamMemberRole.LEADER : TeamMemberRole.MEMBER)
                        .joinedAt(Instant.now())
                        .build());
            }
        }

        // Seed Active Challenge "100K Step Monthly Marathon" if none exists
        ChallengeEntity challenge;
        Instant now = Instant.now();
        if (challengeRepository.findAll().isEmpty()) {
            challenge = challengeRepository.save(ChallengeEntity.builder()
                    .title("100K Step Monthly Marathon")
                    .description("Reach 100,000 steps this month to boost your endurance and claim the top podium spot!")
                    .targetType(ChallengeTargetType.STEPS)
                    .targetValue(100000.0)
                    .startDate(now.minus(5, ChronoUnit.DAYS))
                    .endDate(now.plus(25, ChronoUnit.DAYS))
                    .createdBy(primaryUser.getId())
                    .build());

            log.info("Created default challenge: 100K Step Monthly Marathon");
        } else {
            challenge = challengeRepository.findAll().get(0);
        }

        // Enroll all users in the challenge
        for (UserEntity u : users) {
            if (!challengeMemberRepository.existsByChallengeIdAndUserId(challenge.getId(), u.getId())) {
                challengeMemberRepository.save(ChallengeMemberEntity.builder()
                        .challengeId(challenge.getId())
                        .userId(u.getId())
                        .joinedAt(Instant.now())
                        .build());
            }
        }

        log.info("Dev data seeding completed successfully!");
    }
}
