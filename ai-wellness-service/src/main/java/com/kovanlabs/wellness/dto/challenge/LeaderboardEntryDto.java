package com.kovanlabs.wellness.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntryDto {

    private Integer rank;
    private Long userId;
    private String fullName;
    private Long totalSteps;
    private Double progressPercentage;
}
