package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.challenge.*;
import com.kovanlabs.wellness.entity.ChallengeEntity;
import com.kovanlabs.wellness.entity.ChallengeMemberEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.entity.enums.ChallengeTargetType;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.exception.UnauthorizedException;
import com.kovanlabs.wellness.mapper.ChallengeMapper;
import com.kovanlabs.wellness.provider.ActivityProvider;
import com.kovanlabs.wellness.provider.ChallengeProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.repository.ChallengeMemberRepository;
import com.kovanlabs.wellness.repository.DailyStepRepository;
import com.kovanlabs.wellness.service.ChallengeService;
import com.kovanlabs.wellness.service.WebSocketLeaderboardPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChallengeServiceImpl implements ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeServiceImpl.class);
    private final ChallengeProvider challengeProvider;
    private final UserProvider userProvider;
    private final ActivityProvider activityProvider;
    private final ChallengeMapper challengeMapper;
    private final ChallengeMemberRepository challengeMemberRepository;
    private final DailyStepRepository dailyStepRepository;
    private final WebSocketLeaderboardPublisher leaderboardPublisher;

    public ChallengeServiceImpl(
            ChallengeProvider challengeProvider,
            UserProvider userProvider,
            ActivityProvider activityProvider,
            ChallengeMapper challengeMapper,
            ChallengeMemberRepository challengeMemberRepository,
            DailyStepRepository dailyStepRepository,
            WebSocketLeaderboardPublisher leaderboardPublisher
    ) {
        this.challengeProvider = challengeProvider;
        this.userProvider = userProvider;
        this.activityProvider = activityProvider;
        this.challengeMapper = challengeMapper;
        this.challengeMemberRepository = challengeMemberRepository;
        this.dailyStepRepository = dailyStepRepository;
        this.leaderboardPublisher = leaderboardPublisher;
    }

    @Override
    public ChallengeResponse createChallenge(Long creatorId, ChallengeCreateRequest request) {
        if (userProvider.findById(creatorId).isEmpty()) {
            throw new ResourceNotFoundException("Creator user not found with id: " + creatorId);
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Challenge end date cannot be before start date.");
        }

        ChallengeEntity entity = challengeMapper.toEntity(request);
        entity.setCreatedBy(creatorId);

        ChallengeEntity savedChallenge = challengeProvider.save(entity);

        // Creator automatically joins as first member
        joinChallenge(savedChallenge.getId(), creatorId);

        return challengeMapper.toResponse(savedChallenge);
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeResponse getChallengeById(Long challengeId) {
        ChallengeEntity challenge = challengeProvider.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + challengeId));
        return challengeMapper.toResponse(challenge);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponse> getActiveChallenges() {
        List<ChallengeEntity> activeChallenges = challengeProvider.findActiveChallenges(Instant.now());
        return challengeMapper.toResponseList(activeChallenges);
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeProgressResponse getChallengeProgress(Long challengeId, Long userId) {
        ChallengeEntity challenge = challengeProvider.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + challengeId));

        if (userProvider.findById(userId).isEmpty()) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        if (challenge.getTargetValue() == null || challenge.getTargetValue() <= 0.0) {
            throw new IllegalStateException("Challenge target value must be greater than zero.");
        }

        LocalDate startDate = challenge.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = challenge.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate();

        Long steps = dailyStepRepository.sumStepsByUserIdAndDateRange(userId, startDate, endDate);
        if (steps == null || steps == 0L) {
            Long activitySteps = activityProvider.sumSteps(userId, challenge.getStartDate(), challenge.getEndDate());
            steps = activitySteps != null ? activitySteps : 0L;
        }

        double currentValue = steps.doubleValue();
        double progressPercent = Math.min(100.0, (currentValue / challenge.getTargetValue()) * 100.0);
        boolean isCompleted = currentValue >= challenge.getTargetValue();

        return ChallengeProgressResponse.builder()
                .challengeId(challengeId)
                .userId(userId)
                .challengeTitle(challenge.getTitle())
                .targetType(challenge.getTargetType())
                .targetValue(challenge.getTargetValue())
                .currentValue(Math.round(currentValue * 100.0) / 100.0)
                .progressPercent(Math.round(progressPercent * 100.0) / 100.0)
                .isCompleted(isCompleted)
                .build();
    }

    @Override
    public void deleteChallenge(Long challengeId, Long requesterId) {
        ChallengeEntity challenge = challengeProvider.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + challengeId));

        if (!challenge.getCreatedBy().equals(requesterId))
        {
            throw new UnauthorizedException("Only challenge creator can delete the challenge.");
        }
        challengeProvider.deleteById(challengeId);
    }

    @Override
    public ChallengeMemberResponse joinChallenge(Long challengeId, Long userId) {
        if (challengeProvider.findById(challengeId).isEmpty()) {
            throw new ResourceNotFoundException("Challenge not found with id: " + challengeId);
        }

        UserEntity user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (challengeMemberRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            log.info("User id={} is already a member of challenge id={}", userId, challengeId);
            ChallengeMemberEntity existing = challengeMemberRepository.findByChallengeIdAndUserId(challengeId, userId).get();
            return toMemberResponse(existing, user);
        }

        ChallengeMemberEntity member = ChallengeMemberEntity.builder()
                .challengeId(challengeId)
                .userId(userId)
                .build();

        ChallengeMemberEntity saved = challengeMemberRepository.save(member);
        recalculateAndBroadcastChallengeLeaderboard(challengeId);

        return toMemberResponse(saved, user);
    }

    @Override
    public void leaveChallenge(Long challengeId, Long userId) {
        if (!challengeMemberRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            throw new ResourceNotFoundException("User is not a member of challenge id: " + challengeId);
        }
        challengeMemberRepository.deleteByChallengeIdAndUserId(challengeId, userId);
        recalculateAndBroadcastChallengeLeaderboard(challengeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChallengeMemberResponse> getChallengeMembers(Long challengeId) {
        if (challengeProvider.findById(challengeId).isEmpty())
        {
            throw new ResourceNotFoundException("Challenge not found with id: " + challengeId);
        }

        List<ChallengeMemberEntity> members = challengeMemberRepository.findByChallengeId(challengeId);
        return members.stream().map(m -> {
            UserEntity user = userProvider.findById(m.getUserId()).orElse(null);
            return toMemberResponse(m, user);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChallengeLeaderboardResponse getChallengeLeaderboard(Long challengeId) {
        ChallengeEntity challenge = challengeProvider.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + challengeId));

        List<ChallengeMemberEntity> members = challengeMemberRepository.findByChallengeId(challengeId);
        LocalDate startDate = challenge.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = challenge.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate();

        List<LeaderboardEntryDto> entries = new ArrayList<>();

        for (ChallengeMemberEntity m : members) {
            UserEntity user = userProvider.findById(m.getUserId()).orElse(null);
            String name = user != null ? user.getFullName() : "User #" + m.getUserId();

            Long totalSteps = dailyStepRepository.sumStepsByUserIdAndDateRange(m.getUserId(), startDate, endDate);
            if (totalSteps == null || totalSteps == 0L) {
                Long actSteps = activityProvider.sumSteps(m.getUserId(), challenge.getStartDate(), challenge.getEndDate());
                totalSteps = actSteps != null ? actSteps : 0L;
            }

            double target = challenge.getTargetValue() != null ? challenge.getTargetValue() : 10000.0;
            double progress = Math.min(100.0, (totalSteps.doubleValue() / target) * 100.0);

            entries.add(LeaderboardEntryDto.builder()
                    .userId(m.getUserId())
                    .fullName(name)
                    .totalSteps(totalSteps)
                    .progressPercentage(Math.round(progress * 10.0) / 10.0)
                    .build());
        }

        entries.sort((a, b) -> Long.compare(b.getTotalSteps(), a.getTotalSteps()));

        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        return ChallengeLeaderboardResponse.builder()
                .challengeId(challengeId)
                .challengeTitle(challenge.getTitle())
                .targetValue(challenge.getTargetValue())
                .leaderboard(entries)
                .calculatedAt(Instant.now())
                .build();
    }

    @Override
    public void recalculateAndBroadcastLeaderboards(Long userId) {
        List<ChallengeMemberEntity> memberships = challengeMemberRepository.findByUserId(userId);
        for (ChallengeMemberEntity m : memberships) {
            recalculateAndBroadcastChallengeLeaderboard(m.getChallengeId());
        }
    }

    private void recalculateAndBroadcastChallengeLeaderboard(Long challengeId)
    {
        try {
            ChallengeLeaderboardResponse leaderboard = getChallengeLeaderboard(challengeId);
            leaderboardPublisher.publishChallengeLeaderboardUpdate(challengeId, leaderboard);
        } catch (Exception e) {
            log.warn("Failed to recalculate and broadcast leaderboard for challengeId={}: {}", challengeId, e.getMessage(), e);
        }
    }



    private ChallengeMemberResponse toMemberResponse(ChallengeMemberEntity entity, UserEntity user) {
        return ChallengeMemberResponse.builder()
                .id(entity.getId())
                .challengeId(entity.getChallengeId())
                .userId(entity.getUserId())
                .fullName(user != null ? user.getFullName() : "User #" + entity.getUserId())
                .email(user != null ? user.getEmail() : "")
                .joinedAt(entity.getJoinedAt())
                .build();
    }
}