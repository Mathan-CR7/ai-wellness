package com.kovanlabs.wellness.dto.team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaderboardEntry {

    private Integer rank;

    private Long userId;

    private String fullName;

    private String email;

    private Long totalSteps;

    private Double totalDistanceMeters;

    private Double totalCaloriesBurned;
}
