package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByUserIdOrderByTimestampDesc(Long userId);
}
