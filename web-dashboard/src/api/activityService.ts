import { apiClient } from './apiClient';
import { DailyStepResponse, ActivityResponse, ActivitySummaryResponse, ActivityTrendResponse } from '../types';

export const activityService = {
  getTodaySteps: async (): Promise<DailyStepResponse> => {
    const response = await apiClient.get<DailyStepResponse>('/api/steps/today');
    return response.data;
  },

  syncSteps: async (steps: number, date?: string, sourceDevice = 'Web Dashboard'): Promise<DailyStepResponse> => {
    const response = await apiClient.post<DailyStepResponse>('/api/steps/sync', {
      steps,
      date: date || new Date().toISOString().split('T')[0],
      sourceDevice,
    });
    return response.data;
  },

  getMyActivities: async (): Promise<ActivityResponse[]> => {
    const response = await apiClient.get<ActivityResponse[]>('/api/activities/my');
    return response.data;
  },

  getActivitySummary: async (startTime: string, endTime: string): Promise<ActivitySummaryResponse> => {
    const response = await apiClient.get<ActivitySummaryResponse>('/api/activities/summary', {
      params: { startTime, endTime },
    });
    return response.data;
  },

  getActivityTrends: async (): Promise<ActivityTrendResponse> => {
    const response = await apiClient.get<ActivityTrendResponse>('/api/activities/trends');
    return response.data;
  },
};
