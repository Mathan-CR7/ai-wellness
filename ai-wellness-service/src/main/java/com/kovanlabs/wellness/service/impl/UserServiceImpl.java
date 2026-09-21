package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.auth.UserRegistrationRequest;
import com.kovanlabs.wellness.dto.user.UpdateProfileRequest;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.entity.enums.UserRole;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.mapper.UserMapper;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserProvider userProvider;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserProvider userProvider, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userProvider = userProvider;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileResponse registerUser(UserRegistrationRequest request) {
        if (userProvider.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists.");
        }

        UserEntity user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.ROLE_USER);

        UserEntity savedUser = userProvider.save(user);
        return userMapper.toProfileResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        UserEntity user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfileByEmail(String email) {
        UserEntity user = userProvider.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toProfileResponse(user);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserEntity user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setFullName(request.getFullName());
        if (request.getWeightKg() != null) {
            user.setWeightKg(request.getWeightKg());
        }
        if (request.getHeightCm() != null)
        {
            user.setHeightCm(request.getHeightCm());
        }
        
        UserEntity updatedUser = userProvider.save(user);
        return userMapper.toProfileResponse(updatedUser);
    }
}
