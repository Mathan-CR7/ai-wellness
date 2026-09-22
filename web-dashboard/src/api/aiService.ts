import { apiClient } from './apiClient';
import { AIChatResponse } from '../types';

export const aiService = {
  chat: async (message: string, conversationId?: number): Promise<AIChatResponse> => {
    const response = await apiClient.post<AIChatResponse>('/api/ai/chat', {
      message,
      conversationId,
    });
    return response.data;
  },

  getConversations: async (): Promise<any[]> => {
    const response = await apiClient.get<any[]>('/api/ai/conversations');
    return response.data;
  },

  getConversationMessages: async (conversationId: number): Promise<any[]> => {
    const response = await apiClient.get<any[]>(`/api/ai/conversations/${conversationId}/messages`);
    return response.data;
  },
};
