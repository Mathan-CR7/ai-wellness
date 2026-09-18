package com.kovanlabs.wellness.dto.auth;

import com.kovanlabs.wellness.dto.user.UserProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private UserProfileResponse user;
}
