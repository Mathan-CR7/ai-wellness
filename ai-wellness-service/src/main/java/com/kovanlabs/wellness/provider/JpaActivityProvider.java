package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.ActivityEntity;
import com.kovanlabs.wellness.repository.ActivityRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaActivityProvider implements ActivityProvider {

    private final ActivityRepository activityRepository;

    public JpaActivityProvider(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Override
    public ActivityEntity save(ActivityEntity activity) {
        return activityRepository.save(activity);
    }

    @Override
    public List<ActivityEntity> saveAll(List<ActivityEntity> activities) {
        return activityRepository.saveAll(activities);
    }

    @Override
    public Optional<ActivityEntity> findById(Long id) {
        return activityRepository.findById(id);
    }

    @Override
    public List<ActivityEntity> findByUserId(Long userId) {
        return activityRepository.findByUserIdOrderByStartTimeDesc(userId);
    }

    @Override
    public List<ActivityEntity> findByUserIdAndDateRange(Long userId, Instant startTime, Instant endTime) {
        return activityRepository.findByUserIdAndStartTimeGreaterThanEqualAndEndTimeLessThanEqualOrderByStartTimeAsc(
                userId, startTime, endTime);
    }

    @Override
    public Long sumSteps(Long userId, Instant startTime, Instant endTime) {
        return activityRepository.sumStepCountByUserIdAndDateRange(userId, startTime, endTime);
    }

    @Override
    public Double sumDistance(Long userId, Instant startTime, Instant endTime) {
        return activityRepository.sumDistanceMetersByUserIdAndDateRange(userId, startTime, endTime);
    }

    @Override
    public Double sumCalories(Long userId, Instant startTime, Instant endTime) {
        return activityRepository.sumCaloriesBurnedByUserIdAndDateRange(userId, startTime, endTime);
    }
}
