package com.kovanlabs.wellness.dto.step;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepSyncRequest {

    @JsonAlias({"steps", "step_count", "stepCount", "totalSteps"})
    private Long steps;

    @JsonAlias({"stepCount", "step_count"})
    private Integer stepCount;

    private Object date;

    private String sourceDevice;

    public Long getEffectiveSteps() {
        if (steps != null && steps > 0) return steps;
        if (stepCount != null && stepCount > 0) return stepCount.longValue();
        return 0L;
    }

    public LocalDate getParsedDate() {
        if (date == null) return LocalDate.now(ZoneId.systemDefault());
        if (date instanceof LocalDate) return (LocalDate) date;
        String str = date.toString();
        try {
            if (str.length() >= 10) {
                return LocalDate.parse(str.substring(0, 10));
            }
        } catch (Exception ignored) {}
        return LocalDate.now(ZoneId.systemDefault());
    }
}
