import { apiClient } from './apiClient';
import { TeamLeaderboardResponse } from '../types';

export const leaderboardService = {
  getTeamLeaderboard: async (teamId: number, startTime?: string, endTime?: string): Promise<TeamLeaderboardResponse> => {
    const response = await apiClient.get<TeamLeaderboardResponse>(`/api/teams/${teamId}/leaderboard`, {
      params: { startTime, endTime },
    });
    return response.data;
  },
};
