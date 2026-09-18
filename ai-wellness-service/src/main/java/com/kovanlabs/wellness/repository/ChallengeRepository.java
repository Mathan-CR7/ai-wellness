package com.kovanlabs.wellness.repository;

import com.kovanlabs.wellness.entity.ChallengeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<ChallengeEntity, Long> {

    /**
     * Returns challenges that have started (startDate <= now) AND not yet ended (endDate > now).
     * The previous query only checked endDate which incorrectly included future challenges.
     */
    @Query("SELECT c FROM ChallengeEntity c WHERE c.startDate <= :now AND c.endDate > :now ORDER BY c.startDate ASC")
    List<ChallengeEntity> findActiveChallenges(@Param("now") Instant now);

    List<ChallengeEntity> findByCreatedBy(Long createdBy);
}
