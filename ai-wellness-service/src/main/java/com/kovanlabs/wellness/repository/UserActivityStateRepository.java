package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.UserActivityStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserActivityStateRepository extends JpaRepository<UserActivityStateEntity, Long> {

    Optional<UserActivityStateEntity> findByUserId(Long userId);
}
