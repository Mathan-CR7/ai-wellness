package com.kovanlabs.wellness.dto.user;

import com.kovanlabs.wellness.entity.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {

    private Long id;

    private String email;

    private String fullName;

    private Double weightKg;

    private Double heightCm;

    private UserRole role;

    private Instant createdAt;
}
