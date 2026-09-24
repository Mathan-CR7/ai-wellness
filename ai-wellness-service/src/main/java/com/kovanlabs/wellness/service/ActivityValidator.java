package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.config.AppProperties;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ActivityValidator {

    private final AppProperties appProperties;

    public ActivityValidator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void validate(ActivitySyncRequest request) {
        int minSteps = appProperties.activity().minValidSteps();
        int maxSteps = appProperties.activity().maxDailySteps();
        long steps = request.getEffectiveStepCount();

        if (steps < minSteps) {
            throw new IllegalArgumentException(
                    "Step count (" + steps + ") is below minimum valid threshold (" + minSteps + ")."
            );
        }

        if (steps > maxSteps) {
            throw new IllegalArgumentException(
                    "Step count (" + steps + ") exceeds maximum daily plausible threshold (" + maxSteps + ")."
            );
        }

        Instant start = request.getParsedStartTime();
        Instant end = request.getParsedEndTime();

        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Activity end time cannot be before start time.");
        }
    }
}
