import { apiClient } from './apiClient';
import { TeamResponse, TeamMemberResponse } from '../types';

export const teamService = {
  getMyTeams: async (): Promise<TeamResponse[]> => {
    const response = await apiClient.get<TeamResponse[]>('/api/teams/my');
    return response.data;
  },

  getTeamById: async (id: number): Promise<TeamResponse> => {
    const response = await apiClient.get<TeamResponse>(`/api/teams/${id}`);
    return response.data;
  },

  createTeam: async (data: { name: string; description?: string }): Promise<TeamResponse> => {
    const response = await apiClient.post<TeamResponse>('/api/teams', data);
    return response.data;
  },

  joinTeamByInviteCode: async (inviteCode: string): Promise<TeamResponse> => {
    const response = await apiClient.post<TeamResponse>('/api/teams/join', null, {
      params: { inviteCode },
    });
    return response.data;
  },

  getTeamMembers: async (id: number): Promise<TeamMemberResponse[]> => {
    const response = await apiClient.get<TeamMemberResponse[]>(`/api/teams/${id}/members`);
    return response.data;
  },

  removeMember: async (teamId: number, memberId: number): Promise<void> => {
    await apiClient.delete(`/api/teams/${teamId}/members/${memberId}`);
  },
};
