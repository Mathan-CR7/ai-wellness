package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySummaryResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.auth.UserRegistrationRequest;
import com.kovanlabs.wellness.dto.team.TeamCreateRequest;
import com.kovanlabs.wellness.dto.team.TeamResponse;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.exception.ActivityDataNotAvailableException;
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
class Phase3ServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private TeamService teamService;

    @Test
    @DisplayName("Test User Registration and Profile Retrieval")
    void testUserRegistrationAndProfile() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("john.doe@example.com")
                .password("SecurePassword123!")
                .fullName("John Doe")
                .weightKg(75.5)
                .heightCm(178.0)
                .build();

        UserProfileResponse profile = userService.registerUser(request);
        assertNotNull(profile.getId());
        assertEquals("john.doe@example.com", profile.getEmail());
        assertEquals("John Doe", profile.getFullName());
        assertEquals(75.5, profile.getWeightKg());

        UserProfileResponse fetched = userService.getUserProfile(profile.getId());
        assertEquals(profile.getId(), fetched.getId());
    }

    @Test
    @DisplayName("Test Duplicate Email Registration Rejection")
    void testDuplicateEmailRegistration() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("duplicate@example.com")
                .password("Password123!")
                .fullName("Duplicate User")
                .build();

        userService.registerUser(request);

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }

    @Test
    @DisplayName("Test Health Connect Activity Sync and Aggregate Summary")
    void testActivitySyncAndSummary() {
        UserRegistrationRequest userReq = UserRegistrationRequest.builder()
                .email("runner@example.com")
                .password("Password123!")
                .fullName("Runner Person")
                .build();
        UserProfileResponse user = userService.registerUser(userReq);

        Instant now = Instant.now();
        Instant start = now.minus(1, ChronoUnit.HOURS);

        ActivitySyncRequest syncReq = ActivitySyncRequest.builder()
                .stepCount(5000)
                .distanceMeters(3500.0)
                .caloriesBurned(250.0)
                .startTime(start)
                .endTime(now)
                .sourceDevice("ANDROID_HEALTH_CONNECT")
                .build();

        ActivityResponse synced = activityService.syncActivity(user.getId(), syncReq);
        assertNotNull(synced.getId());
        assertEquals(5000, synced.getStepCount());

        ActivitySummaryResponse summary = activityService.getActivitySummary(user.getId(), start.minus(5, ChronoUnit.MINUTES), now.plus(5, ChronoUnit.MINUTES));
        assertEquals(5000L, summary.getTotalSteps());
        assertEquals(3500.0, summary.getTotalDistanceMeters());
        assertEquals(250.0, summary.getTotalCaloriesBurned());
        assertEquals(1, summary.getActivityRecordCount());
    }

    @Test
    @DisplayName("Test 0% Fake Fallback: ActivityDataNotAvailableException thrown when user has no activity logs")
    void testNoActivityDataThrowsException() {
        UserRegistrationRequest userReq = UserRegistrationRequest.builder()
                .email("inactive@example.com")
                .password("Password123!")
                .fullName("Inactive Person")
                .build();
        UserProfileResponse user = userService.registerUser(userReq);

        assertThrows(ActivityDataNotAvailableException.class, () -> activityService.getUserActivities(user.getId()));
    }

    @Test
    @DisplayName("Test Team Creation with Random 8-Char Invite Code and Owner Membership")
    void testTeamCreation() {
        UserRegistrationRequest userReq = UserRegistrationRequest.builder()
                .email("teamleader@example.com")
                .password("Password123!")
                .fullName("Team Leader")
                .build();
        UserProfileResponse owner = userService.registerUser(userReq);

        TeamCreateRequest teamReq = TeamCreateRequest.builder()
                .name("Morning Runners")
                .description("Daily 5k runners group")
                .build();

        TeamResponse team = teamService.createTeam(owner.getId(), teamReq);
        assertNotNull(team.getId());
        assertEquals("Morning Runners", team.getName());
        assertNotNull(team.getInviteCode());
        assertEquals(8, team.getInviteCode().length());
        assertEquals(1, team.getMemberCount());
    }

    @Test
    @DisplayName("Test Joining Team by Invite Code")
    void testJoinTeamByInviteCode() {
        UserProfileResponse leader = userService.registerUser(UserRegistrationRequest.builder()
                .email("leader@example.com").password("Pass123!").fullName("Leader").build());

        UserProfileResponse member = userService.registerUser(UserRegistrationRequest.builder()
                .email("member@example.com").password("Pass123!").fullName("Member").build());

        TeamResponse team = teamService.createTeam(leader.getId(), TeamCreateRequest.builder().name("Fit Squad").build());

        TeamResponse joinedTeam = teamService.joinTeamByInviteCode(member.getId(), team.getInviteCode());
        assertEquals(2, joinedTeam.getMemberCount());
    }
}
