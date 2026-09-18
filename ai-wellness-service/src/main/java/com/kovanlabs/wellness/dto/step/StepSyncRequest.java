package com.kovanlabs.wellness.dto.step;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepSyncRequest {

    @NotNull(message = "Step count is required")
    @Min(value = 0, message = "Step count cannot be negative")
    private Long steps;

    private LocalDate date;

    private String sourceDevice;
}
