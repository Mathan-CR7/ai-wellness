package com.kovanlabs.wellness.dto.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityUpdateMessage {
    private Long userId;
    private String userEmail;
    private String date;
    private Long steps;
    private Double distanceMeters;
    private Double caloriesBurned;
    private Instant timestamp;
}
