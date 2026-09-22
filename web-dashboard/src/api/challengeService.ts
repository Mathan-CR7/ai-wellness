import { apiClient } from './apiClient';
import {
  ChallengeResponse,
  ChallengeMemberResponse,
  ChallengeLeaderboardResponse,
  ChallengeProgressResponse,
} from '../types';

export const challengeService = {
  getActiveChallenges: async (): Promise<ChallengeResponse[]> => {
    const response = await apiClient.get<ChallengeResponse[]>('/api/challenges/active');
    return response.data;
  },

  getChallengeById: async (id: number): Promise<ChallengeResponse> => {
    const response = await apiClient.get<ChallengeResponse>(`/api/challenges/${id}`);
    return response.data;
  },

  createChallenge: async (data: {
    title: string;
    description: string;
    targetSteps: number;
    startDate: string;
    endDate: string;
  }): Promise<ChallengeResponse> => {
    // Format start and end date to ISO Instant format expected by Spring Boot
    const startIso = data.startDate.includes('T')
      ? new Date(data.startDate).toISOString()
      : new Date(`${data.startDate}T00:00:00Z`).toISOString();
      
    const endIso = data.endDate.includes('T')
      ? new Date(data.endDate).toISOString()
      : new Date(`${data.endDate}T23:59:59Z`).toISOString();

    const payload = {
      title: data.title,
      description: data.description,
      targetType: 'STEPS',
      targetValue: Number(data.targetSteps),
      startDate: startIso,
      endDate: endIso,
    };

    const response = await apiClient.post<ChallengeResponse>('/api/challenges', payload);
    return response.data;
  },

  joinChallenge: async (id: number): Promise<ChallengeMemberResponse> => {
    const response = await apiClient.post<ChallengeMemberResponse>(`/api/challenges/${id}/join`);
    return response.data;
  },

  leaveChallenge: async (id: number): Promise<void> => {
    await apiClient.delete(`/api/challenges/${id}/leave`);
  },

  getChallengeMembers: async (id: number): Promise<ChallengeMemberResponse[]> => {
    const response = await apiClient.get<ChallengeMemberResponse[]>(`/api/challenges/${id}/members`);
    return response.data;
  },

  getChallengeLeaderboard: async (id: number): Promise<ChallengeLeaderboardResponse> => {
    const response = await apiClient.get<ChallengeLeaderboardResponse>(`/api/challenges/${id}/leaderboard`);
    return response.data;
  },

  getChallengeProgress: async (id: number): Promise<ChallengeProgressResponse> => {
    const response = await apiClient.get<ChallengeProgressResponse>(`/api/challenges/${id}/progress`);
    return response.data;
  },
};
