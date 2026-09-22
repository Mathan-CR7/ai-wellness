import { apiClient } from './apiClient';
import { ExerciseResponse } from '../types';

export const exerciseService = {
  getMyExercises: async (): Promise<ExerciseResponse[]> => {
    const response = await apiClient.get<ExerciseResponse[]>('/api/exercises/my');
    return response.data;
  },

  logExercise: async (data: {
    exerciseType: string;
    durationMinutes: number;
    caloriesBurned: number;
    notes?: string;
    loggedAt?: string;
  }): Promise<ExerciseResponse> => {
    const payload = {
      exerciseType: data.exerciseType,
      durationMinutes: Number(data.durationMinutes),
      caloriesBurned: Number(data.caloriesBurned),
      timestamp: data.loggedAt || new Date().toISOString(),
    };
    const response = await apiClient.post<ExerciseResponse>('/api/exercises', payload);
    return response.data;
  },
};
