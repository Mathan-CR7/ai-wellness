package com.kovanlabs.wellness.dto.challenge;

import com.kovanlabs.wellness.entity.enums.ChallengeTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeProgressResponse {

    private Long challengeId;

    private Long userId;

    private String challengeTitle;

    private ChallengeTargetType targetType;

    private Double targetValue;

    private Double currentValue;

    private Double progressPercent;

    private Boolean isCompleted;
}
