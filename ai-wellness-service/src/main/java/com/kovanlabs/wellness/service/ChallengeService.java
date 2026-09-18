package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.challenge.ChallengeCreateRequest;
import com.kovanlabs.wellness.dto.challenge.ChallengeLeaderboardResponse;
import com.kovanlabs.wellness.dto.challenge.ChallengeMemberResponse;
import com.kovanlabs.wellness.dto.challenge.ChallengeProgressResponse;
import com.kovanlabs.wellness.dto.challenge.ChallengeResponse;

import java.util.List;

public interface ChallengeService {

    ChallengeResponse createChallenge(Long creatorId, ChallengeCreateRequest request);

    ChallengeResponse getChallengeById(Long challengeId);

    List<ChallengeResponse> getActiveChallenges();

    ChallengeProgressResponse getChallengeProgress(Long challengeId, Long userId);

    void deleteChallenge(Long challengeId, Long requesterId);

    ChallengeMemberResponse joinChallenge(Long challengeId, Long userId);

    void leaveChallenge(Long challengeId, Long userId);

    List<ChallengeMemberResponse> getChallengeMembers(Long challengeId);

    ChallengeLeaderboardResponse getChallengeLeaderboard(Long challengeId);

    void recalculateAndBroadcastLeaderboards(Long userId);
}
