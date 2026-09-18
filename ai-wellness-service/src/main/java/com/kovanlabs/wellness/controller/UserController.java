package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.user.UpdateProfileRequest;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
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
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing authenticated user profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<UserProfileResponse> getCurrentProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = userService.getUserProfileByEmail(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile metrics")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse current = userService.getUserProfileByEmail(userDetails.getUsername());
        UserProfileResponse updated = userService.updateProfile(current.getId(), request);
        return ResponseEntity.ok(updated);
    }
}
