package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.auth.UserRegistrationRequest;
import com.kovanlabs.wellness.dto.user.UpdateProfileRequest;
import com.kovanlabs.wellness.dto.user.UserProfileResponse;

public interface UserService {

    UserProfileResponse registerUser(UserRegistrationRequest request);

    UserProfileResponse getUserProfile(Long userId);

    UserProfileResponse getUserProfileByEmail(String email);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}
