package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.AIConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversationEntity, Long> {

    List<AIConversationEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
