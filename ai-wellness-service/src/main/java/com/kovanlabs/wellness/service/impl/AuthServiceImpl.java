package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.auth.AuthResponse;
import com.kovanlabs.wellness.dto.auth.LoginRequest;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.exception.UnauthorizedException;
import com.kovanlabs.wellness.mapper.UserMapper;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.security.JwtTokenProvider;
import com.kovanlabs.wellness.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserProvider userProvider;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserProvider userProvider, UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userProvider = userProvider;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userProvider.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String token = tokenProvider.generateToken(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());
        UserProfileResponse userProfile = userMapper.toProfileResponse(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userProfile)
                .build();
    }
}
