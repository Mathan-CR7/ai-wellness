package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.exercise.ExerciseLogRequest;
import com.kovanlabs.wellness.dto.exercise.ExerciseResponse;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.service.ExerciseService;
import com.kovanlabs.wellness.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
@Tag(name = "Exercise Management", description = "Endpoints for logging workout sessions")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final UserService userService;

    public ExerciseController(ExerciseService exerciseService, UserService userService) {
        this.exerciseService = exerciseService;
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Log a workout exercise session")
    public ResponseEntity<ExerciseResponse> logExercise(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ExerciseLogRequest request
    ) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        ExerciseResponse response = exerciseService.logExercise(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Get exercise history for current user")
    public ResponseEntity<List<ExerciseResponse>> getMyExercises(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse user = userService.getUserProfileByEmail(userDetails.getUsername());
        List<ExerciseResponse> exercises = exerciseService.getUserExercises(user.getId());
        return ResponseEntity.ok(exercises);
    }
}
