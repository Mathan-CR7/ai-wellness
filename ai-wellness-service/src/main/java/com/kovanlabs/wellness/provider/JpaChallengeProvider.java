package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.ChallengeEntity;
import com.kovanlabs.wellness.repository.ChallengeRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class JpaChallengeProvider implements ChallengeProvider {

    private final ChallengeRepository challengeRepository;

    public JpaChallengeProvider(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    @Override
    public ChallengeEntity save(ChallengeEntity challenge) {
        return challengeRepository.save(challenge);
    }

    @Override
    public Optional<ChallengeEntity> findById(Long id) {
        return challengeRepository.findById(id);
    }

    @Override
    public List<ChallengeEntity> findActiveChallenges(Instant now) {
        return challengeRepository.findActiveChallenges(now);
    }

    @Override
    public List<ChallengeEntity> findByCreatedBy(Long userId) {
        return challengeRepository.findByCreatedBy(userId);
    }

    @Override
    public void deleteById(Long id) {
        challengeRepository.deleteById(id);
    }
}
