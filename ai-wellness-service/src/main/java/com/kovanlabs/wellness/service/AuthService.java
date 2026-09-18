package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.auth.AuthResponse;
import com.kovanlabs.wellness.dto.auth.LoginRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
