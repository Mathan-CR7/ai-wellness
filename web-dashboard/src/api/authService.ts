import { apiClient } from './apiClient';
import { AuthResponse } from '../types';

export const authService = {
  login: async (credentials: { email: string; password: string }): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/api/auth/login', credentials);
    return response.data;
  },

  register: async (data: { fullName: string; email: string; password: string }): Promise<AuthResponse> => {
    const response = await apiClient.post<AuthResponse>('/api/auth/register', data);
    return response.data;
  },
};
