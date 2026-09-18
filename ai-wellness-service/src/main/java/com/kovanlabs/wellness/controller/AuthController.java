package com.kovanlabs.wellness.controller;

import com.kovanlabs.wellness.dto.auth.AuthResponse;
import com.kovanlabs.wellness.dto.auth.LoginRequest;
import com.kovanlabs.wellness.dto.auth.UserRegistrationRequest;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.security.JwtTokenProvider;
import com.kovanlabs.wellness.service.AuthService;
import com.kovanlabs.wellness.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration and authentication endpoints")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserService userService, AuthService authService, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.authService = authService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates a new user record with BCrypt password hashing and returns JWT access token.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
        UserProfileResponse userProfile = userService.registerUser(request);
        String token = tokenProvider.generateToken(userProfile.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(userProfile.getEmail());

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userProfile)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Verifies user email and password, returning JWT access token upon success.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
