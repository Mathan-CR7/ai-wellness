package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.config.AppProperties;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import org.springframework.stereotype.Component;

@Component
public class ActivityValidator {

    private final AppProperties appProperties;

    public ActivityValidator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void validate(ActivitySyncRequest request) {
        int minSteps = appProperties.activity().minValidSteps();
        int maxSteps = appProperties.activity().maxDailySteps();

        if (request.getStepCount() < minSteps) {
            throw new IllegalArgumentException(
                    "Step count (" + request.getStepCount() + ") is below minimum valid threshold (" + minSteps + ")."
            );
        }

        if (request.getStepCount() > maxSteps) {
            throw new IllegalArgumentException(
                    "Step count (" + request.getStepCount() + ") exceeds maximum daily plausible threshold (" + maxSteps + ")."
            );
        }

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("Activity end time cannot be before start time.");
        }
    }
}
