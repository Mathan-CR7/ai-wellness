package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.step.DailyStepResponse;
import com.kovanlabs.wellness.dto.step.StepSyncRequest;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.service.StepService;
import com.kovanlabs.wellness.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/steps")
@Tag(name = "Step Synchronization", description = "Idempotent step synchronization endpoints for Android Health Connect")
@SecurityRequirement(name = "bearerAuth")
public class StepController {

    private final StepService stepService;
    private final UserService userService;

    public StepController(StepService stepService, UserService userService) {
        this.stepService = stepService;
        this.userService = userService;
    }

    @PostMapping("/sync")
    @Operation(summary = "Idempotently sync Health Connect step records for authenticated user")
    public ResponseEntity<DailyStepResponse> syncSteps(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StepSyncRequest request
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        DailyStepResponse response = stepService.syncSteps(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    @Operation(summary = "Get current authenticated user's step count for today")
    public ResponseEntity<DailyStepResponse> getTodaySteps(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        DailyStepResponse response = stepService.getTodaySteps(user.getId());
        return ResponseEntity.ok(response);
    }
}
