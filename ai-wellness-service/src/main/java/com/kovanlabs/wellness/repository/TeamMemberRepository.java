package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMemberEntity, Long> {

    List<TeamMemberEntity> findByTeamId(Long teamId);

    List<TeamMemberEntity> findByUserId(Long userId);

    Optional<TeamMemberEntity> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);
}
