package com.kovanlabs.wellness.dto.activity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivitySyncRequest {

    @JsonAlias({"steps", "step_count", "stepCount", "totalSteps"})
    private Integer stepCount;

    @JsonAlias({"steps", "step_count", "stepCount"})
    private Long steps;

    @JsonAlias({"distanceMeters", "distance_meters", "distance"})
    private Double distanceMeters;

    @JsonAlias({"caloriesBurned", "calories_burned", "calories"})
    private Double caloriesBurned;

    @JsonAlias({"startTime", "start_time", "timestamp"})
    private Object startTime;

    @JsonAlias({"endTime", "end_time"})
    private Object endTime;

    @Builder.Default
    private String sourceDevice = "ANDROID_HEALTH_CONNECT";

    public Long getEffectiveStepCount() {
        if (stepCount != null && stepCount > 0) return stepCount.longValue();
        if (steps != null && steps > 0) return steps;
        return 0L;
    }

    public Instant getParsedStartTime() {
        if (startTime == null) return Instant.now();
        if (startTime instanceof Instant) return (Instant) startTime;
        if (startTime instanceof Number) return Instant.ofEpochMilli(((Number) startTime).longValue());
        String str = startTime.toString();
        try {
            return Instant.parse(str);
        } catch (Exception e) {
            try {
                return LocalDate.parse(str.substring(0, 10)).atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {}
        }
        return Instant.now();
    }

    public Instant getParsedEndTime() {
        if (endTime == null) return Instant.now();
        if (endTime instanceof Instant) return (Instant) endTime;
        if (endTime instanceof Number) return Instant.ofEpochMilli(((Number) endTime).longValue());
        String str = endTime.toString();
        try {
            return Instant.parse(str);
        } catch (Exception e) {
            try {
                return LocalDate.parse(str.substring(0, 10)).atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {}
        }
        return Instant.now();
    }
}
