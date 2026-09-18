package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySummaryResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.service.ActivityService;
import com.kovanlabs.wellness.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@Tag(name = "Activity Management", description = "Endpoints for Health Connect physical activity synchronization and trend analytics")
@SecurityRequirement(name = "bearerAuth")
public class ActivityController {

    private final ActivityService activityService;
    private final UserService userService;

    public ActivityController(ActivityService activityService, UserService userService) {
        this.activityService = activityService;
        this.userService = userService;
    }

    @PostMapping("/sync")
    @Operation(summary = "Synchronize physical activity record from Android Health Connect")
    public ResponseEntity<ActivityResponse> syncActivity(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ActivitySyncRequest request
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        ActivityResponse response = activityService.syncActivity(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get physical activity history for current user")
    public ResponseEntity<List<ActivityResponse>> getMyActivities(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        List<ActivityResponse> activities = activityService.getUserActivities(user.getId());
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregated activity metrics for date range")
    public ResponseEntity<ActivitySummaryResponse> getActivitySummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        ActivitySummaryResponse summary = activityService.getActivitySummary(user.getId(), startTime, endTime);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/trends")
    @Operation(summary = "Get 7-day moving averages, completion rates, and active streak days")
    public ResponseEntity<ActivityTrendResponse> getActivityTrends(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        ActivityTrendResponse trends = activityService.getActivityTrends(user.getId());
        return ResponseEntity.ok(trends);
    }
}
