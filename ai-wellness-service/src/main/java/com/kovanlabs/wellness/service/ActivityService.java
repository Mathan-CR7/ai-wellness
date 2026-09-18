package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySummaryResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.dto.activity.ActivityTrendResponse;

import java.time.Instant;
import java.util.List;

public interface ActivityService {

    ActivityResponse syncActivity(Long userId, ActivitySyncRequest request);

    List<ActivityResponse> getUserActivities(Long userId);

    ActivitySummaryResponse getActivitySummary(Long userId, Instant startTime, Instant endTime);

    ActivityTrendResponse getActivityTrends(Long userId);
}
