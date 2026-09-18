package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.ChallengeMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeMemberRepository extends JpaRepository<ChallengeMemberEntity, Long> {

    List<ChallengeMemberEntity> findByChallengeId(Long challengeId);

    List<ChallengeMemberEntity> findByUserId(Long userId);

    Optional<ChallengeMemberEntity> findByChallengeIdAndUserId(Long challengeId, Long userId);

    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);

    void deleteByChallengeIdAndUserId(Long challengeId, Long userId);
}
