package com.kovanlabs.wellness.dto.challenge;

import com.kovanlabs.wellness.entity.enums.ChallengeTargetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Target type is required")
    private ChallengeTargetType targetType;

    @NotNull(message = "Target value is required")
    @Min(value = 1, message = "Target value must be greater than 0")
    private Double targetValue;

    @NotNull(message = "Start date is required")
    private Instant startDate;

    @NotNull(message = "End date is required")
    private Instant endDate;
}
