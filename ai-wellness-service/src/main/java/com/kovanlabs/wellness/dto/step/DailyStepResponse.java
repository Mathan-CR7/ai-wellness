package com.kovanlabs.wellness.dto.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStepResponse {

    private Long id;
    private Long userId;
    private LocalDate date;
    private Long steps;
    private Double distanceMeters;
    private Double caloriesBurned;
    private Instant createdAt;
    private Instant updatedAt;
}
