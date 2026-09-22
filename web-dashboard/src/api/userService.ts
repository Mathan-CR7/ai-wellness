import { apiClient } from './apiClient';
import { UserProfile } from '../types';

export const userService = {
  getCurrentProfile: async (): Promise<UserProfile> => {
    const response = await apiClient.get<UserProfile>('/api/users/me');
    return response.data;
  },

  updateProfile: async (data: {
    fullName?: string;
    dailyStepGoal?: number;
    weightKg?: number;
    heightCm?: number;
  }): Promise<UserProfile> => {
    const response = await apiClient.put<UserProfile>('/api/users/me', data);
    return response.data;
  },
};
