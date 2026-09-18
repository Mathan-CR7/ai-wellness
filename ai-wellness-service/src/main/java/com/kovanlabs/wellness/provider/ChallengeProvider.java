package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.ChallengeEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction layer for challenge data access.
 */
public interface ChallengeProvider {

    ChallengeEntity save(ChallengeEntity challenge);

    Optional<ChallengeEntity> findById(Long id);

    List<ChallengeEntity> findActiveChallenges(Instant now);

    List<ChallengeEntity> findByCreatedBy(Long userId);

    void deleteById(Long id);
}
