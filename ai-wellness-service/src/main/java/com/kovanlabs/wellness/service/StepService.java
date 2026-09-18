package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.step.DailyStepResponse;
import com.kovanlabs.wellness.dto.step.StepSyncRequest;

import java.time.LocalDate;

public interface StepService {

    DailyStepResponse syncSteps(Long userId, StepSyncRequest request);

    DailyStepResponse getTodaySteps(Long userId);

    DailyStepResponse getStepsForDate(Long userId, LocalDate date);
}
