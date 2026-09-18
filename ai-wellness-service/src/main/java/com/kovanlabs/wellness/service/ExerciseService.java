package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.exercise.ExerciseLogRequest;
import com.kovanlabs.wellness.dto.exercise.ExerciseResponse;

import java.time.Instant;
import java.util.List;

public interface ExerciseService {

    ExerciseResponse logExercise(Long userId, ExerciseLogRequest request);

    List<ExerciseResponse> getUserExercises(Long userId);

    List<ExerciseResponse> getUserExercisesByDateRange(Long userId, Instant start, Instant end);
}
