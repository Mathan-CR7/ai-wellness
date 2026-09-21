package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.exercise.ExerciseLogRequest;
import com.kovanlabs.wellness.dto.exercise.ExerciseResponse;
import com.kovanlabs.wellness.entity.ExerciseEntity;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.mapper.ExerciseMapper;
import com.kovanlabs.wellness.provider.ExerciseProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.service.ExerciseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseProvider exerciseProvider;
    private final UserProvider userProvider;
    private final ExerciseMapper exerciseMapper;

    public ExerciseServiceImpl(ExerciseProvider exerciseProvider, UserProvider userProvider, ExerciseMapper exerciseMapper) {
        this.exerciseProvider = exerciseProvider;
        this.userProvider = userProvider;
        this.exerciseMapper = exerciseMapper;
    }

    @Override
    public ExerciseResponse logExercise(Long userId, ExerciseLogRequest request) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        ExerciseEntity entity = exerciseMapper.toEntity(request);
        entity.setUserId(userId);

        ExerciseEntity savedExercise = exerciseProvider.save(entity);
        return exerciseMapper.toResponse(savedExercise);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getUserExercises(Long userId) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<ExerciseEntity> exercises = exerciseProvider.findByUserId(userId);
        return exerciseMapper.toResponseList(exercises);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExerciseResponse> getUserExercisesByDateRange(Long userId, Instant start, Instant end) {
        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        if (start != null && end != null && start.isAfter(end))
        {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        List<ExerciseEntity> exercises = exerciseProvider.findByUserIdAndDateRange(userId, start, end);
        return exerciseMapper.toResponseList(exercises);
    }
}
