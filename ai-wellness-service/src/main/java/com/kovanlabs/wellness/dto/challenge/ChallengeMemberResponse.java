package com.kovanlabs.wellness.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeMemberResponse {

    private Long id;
    private Long challengeId;
    private Long userId;
    private String fullName;
    private String email;
    private Instant joinedAt;
}
