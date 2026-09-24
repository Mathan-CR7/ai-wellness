package com.kovanlabs.wellness.dto.step;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepSyncRequest {

    @JsonAlias({"steps", "step_count", "stepCount", "totalSteps"})
    @Min(value = 0, message = "Step count cannot be negative")
    private Long steps;

    private LocalDate date;

    private String sourceDevice;
}
