package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.ai.AIChatRequest;
import com.kovanlabs.wellness.dto.ai.AIChatResponse;
import com.kovanlabs.wellness.entity.AIConversationEntity;
import com.kovanlabs.wellness.entity.AIMessageEntity;

import java.util.List;

public interface AiService {

    AIChatResponse processChat(Long userId, AIChatRequest request);

    List<AIConversationEntity> getUserConversations(Long userId);

    List<AIMessageEntity> getConversationMessages(Long conversationId, Long userId);
}
