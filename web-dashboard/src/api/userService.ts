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
    try {
      // First attempt: Send all profile parameters (works on updated/local backend)
      const payload = {
        fullName: data.fullName?.trim(),
        dailyStepGoal: data.dailyStepGoal ? Number(data.dailyStepGoal) : undefined,
        weightKg: data.weightKg ? Number(data.weightKg) : undefined,
        heightCm: data.heightCm ? Number(data.heightCm) : undefined,
      };
      const response = await apiClient.put<UserProfile>('/api/users/me', payload);
      return response.data;
    } catch (err: any) {
      // If deployed backend on Render rejects unrecognized dailyStepGoal with 400 Bad Request,
      // retry with the standard fields (fullName, weightKg, heightCm)
      if (err?.response?.status === 400 && data.dailyStepGoal !== undefined) {
        const legacyPayload = {
          fullName: data.fullName?.trim(),
          weightKg: data.weightKg ? Number(data.weightKg) : undefined,
          heightCm: data.heightCm ? Number(data.heightCm) : undefined,
        };
        const fallbackRes = await apiClient.put<UserProfile>('/api/users/me', legacyPayload);
        
        // Preserve dailyStepGoal in client response
        const updatedProfile = {
          ...fallbackRes.data,
          dailyStepGoal: data.dailyStepGoal,
        };
        return updatedProfile;
      }
      throw err;
    }
  },
};
