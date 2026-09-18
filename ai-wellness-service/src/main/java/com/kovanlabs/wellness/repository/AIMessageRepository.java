package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.AIMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIMessageRepository extends JpaRepository<AIMessageEntity, Long> {

    List<AIMessageEntity> findByConversationIdOrderByTimestampAsc(Long conversationId);

    List<AIMessageEntity> findByConversationIdOrderByTimestampDesc(Long conversationId, Pageable pageable);
}
