package com.kovanlabs.wellness.provider;

import com.kovanlabs.wellness.entity.TeamEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import com.kovanlabs.wellness.repository.TeamMemberRepository;
import com.kovanlabs.wellness.repository.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaTeamProvider implements TeamProvider {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public JpaTeamProvider(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Override
    public TeamEntity saveTeam(TeamEntity team) {
        return teamRepository.save(team);
    }

    @Override
    public Optional<TeamEntity> findTeamById(Long id) {
        return teamRepository.findById(id);
    }

    @Override
    public Optional<TeamEntity> findTeamByInviteCode(String inviteCode) {
        return teamRepository.findByInviteCode(inviteCode);
    }

    @Override
    public boolean existsByInviteCode(String inviteCode) {
        return teamRepository.existsByInviteCode(inviteCode);
    }

    @Override
    public void deleteTeamById(Long id) {
        teamRepository.deleteById(id);
    }

    @Override
    public TeamMemberEntity addMember(TeamMemberEntity member) {
        return teamMemberRepository.save(member);
    }

    @Override
    public List<TeamMemberEntity> findMembersByTeamId(Long teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }

    @Override
    public List<TeamMemberEntity> findTeamsByUserId(Long userId) {
        return teamMemberRepository.findByUserId(userId);
    }

    @Override
    public Optional<TeamMemberEntity> findMember(Long teamId, Long userId) {
        return teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
    }

    @Override
    public boolean isMember(Long teamId, Long userId) {
        return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    @Override
    public void removeMember(Long memberId) {
        teamMemberRepository.deleteById(memberId);
    }
}
