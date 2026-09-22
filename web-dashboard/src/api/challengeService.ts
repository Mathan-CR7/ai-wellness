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
    const response = await apiClient.post<ChallengeResponse>('/api/challenges', data);
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
