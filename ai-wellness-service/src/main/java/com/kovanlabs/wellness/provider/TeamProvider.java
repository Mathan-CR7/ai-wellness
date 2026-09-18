package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.TeamEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction layer for team management data access.
 */
public interface TeamProvider {

    TeamEntity saveTeam(TeamEntity team);

    Optional<TeamEntity> findTeamById(Long id);

    Optional<TeamEntity> findTeamByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);

    void deleteTeamById(Long id);

    TeamMemberEntity addMember(TeamMemberEntity member);

    List<TeamMemberEntity> findMembersByTeamId(Long teamId);

    List<TeamMemberEntity> findTeamsByUserId(Long userId);

    Optional<TeamMemberEntity> findMember(Long teamId, Long userId);

    boolean isMember(Long teamId, Long userId);

    void removeMember(Long memberId);
}
