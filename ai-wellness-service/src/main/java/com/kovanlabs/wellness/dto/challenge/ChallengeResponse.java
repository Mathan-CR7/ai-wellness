package com.kovanlabs.wellness.dto.challenge;

import com.kovanlabs.wellness.entity.enums.ChallengeTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeResponse {

    private Long id;

    private String title;

    private String description;

    private ChallengeTargetType targetType;

    private Double targetValue;

    private Instant startDate;

    private Instant endDate;

    private Long createdBy;

    private Instant createdAt;
}
