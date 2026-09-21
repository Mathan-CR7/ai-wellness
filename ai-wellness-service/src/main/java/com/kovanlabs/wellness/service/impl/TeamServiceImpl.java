package com.kovanlabs.wellness.service.impl;

import com.kovanlabs.wellness.dto.team.TeamCreateRequest;
import com.kovanlabs.wellness.dto.team.TeamMemberResponse;
import com.kovanlabs.wellness.dto.team.TeamResponse;
import com.kovanlabs.wellness.entity.TeamEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import com.kovanlabs.wellness.entity.UserEntity;
import com.kovanlabs.wellness.entity.enums.TeamMemberRole;
import com.kovanlabs.wellness.exception.ResourceNotFoundException;
import com.kovanlabs.wellness.exception.UnauthorizedException;
import com.kovanlabs.wellness.mapper.TeamMapper;
import com.kovanlabs.wellness.provider.TeamProvider;
import com.kovanlabs.wellness.provider.UserProvider;
import com.kovanlabs.wellness.service.TeamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamServiceImpl implements TeamService
{
    private final TeamProvider teamProvider;
    private final UserProvider userProvider;
    private final TeamMapper teamMapper;

    public TeamServiceImpl(TeamProvider teamProvider, UserProvider userProvider, TeamMapper teamMapper) {
        this.teamProvider = teamProvider;
        this.userProvider = userProvider;
        this.teamMapper = teamMapper;
    }

    @Override
    public TeamResponse createTeam(Long ownerId, TeamCreateRequest request) {
        UserEntity owner = userProvider.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner user not found with id: " + ownerId));

        String inviteCode;
        int attempts = 0;
        do {
            if (attempts++ > 10) {
                throw new IllegalStateException("Failed to generate a unique invite code. Please try again.");
            }
            inviteCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (teamProvider.existsByInviteCode(inviteCode));

        TeamEntity team = teamMapper.toEntity(request);
        team.setOwnerId(ownerId);
        team.setInviteCode(inviteCode);

        TeamEntity savedTeam = teamProvider.saveTeam(team);

        TeamMemberEntity ownerMember = TeamMemberEntity.builder()
                .teamId(savedTeam.getId())
                .userId(ownerId)
                .role(TeamMemberRole.LEADER)
                .build();
        teamProvider.addMember(ownerMember);

        TeamResponse response = teamMapper.toResponse(savedTeam);
        response.setMemberCount(1);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long teamId) {
        TeamEntity team = teamProvider.findTeamById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));
        List<TeamMemberEntity> members = teamProvider.findMembersByTeamId(teamId);

        TeamResponse response = teamMapper.toResponse(team);
        response.setMemberCount(members.size());
        return response;
    }

    @Override
    public TeamResponse joinTeamByInviteCode(Long userId, String inviteCode) {
        UserEntity user = userProvider.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        TeamEntity team = teamProvider.findTeamByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid team invite code: " + inviteCode));

        if (teamProvider.isMember(team.getId(), userId)) {
            throw new IllegalArgumentException("User is already a member of team: " + team.getName());
        }

        TeamMemberEntity newMember = TeamMemberEntity.builder()
                .teamId(team.getId())
                .userId(userId)
                .role(TeamMemberRole.MEMBER)
                .build();
        teamProvider.addMember(newMember);

        List<TeamMemberEntity> members = teamProvider.findMembersByTeamId(team.getId());
        TeamResponse response = teamMapper.toResponse(team);
        response.setMemberCount(members.size());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(Long teamId) {
        if (teamProvider.findTeamById(teamId).isEmpty()) {
            throw new ResourceNotFoundException("Team not found with id: " + teamId);
        }

        List<TeamMemberEntity> members = teamProvider.findMembersByTeamId(teamId);
        return members.stream().map(member -> {
            TeamMemberResponse resp = teamMapper.toMemberResponse(member);
            userProvider.findById(member.getUserId()).ifPresent(user -> {
                resp.setFullName(user.getFullName());
                resp.setEmail(user.getEmail());
            });
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getUserTeams(Long userId) {
        List<TeamMemberEntity> memberships = teamProvider.findTeamsByUserId(userId);
        return memberships.stream()
                .map(m -> teamProvider.findTeamById(m.getTeamId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(team -> {
                    TeamResponse resp = teamMapper.toResponse(team);
                    resp.setMemberCount(teamProvider.findMembersByTeamId(team.getId()).size());
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void removeMember(Long teamId, Long requesterId, Long memberId) {
        TeamEntity team = teamProvider.findTeamById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + teamId));

        if (team.getOwnerId().equals(memberId)) {
            throw new IllegalArgumentException("Team owner cannot remove themselves. Transfer ownership first.");
        }

        if (!team.getOwnerId().equals(requesterId) && !requesterId.equals(memberId)) {
            throw new UnauthorizedException("Only team owner can remove other members from the team.");
        }

        TeamMemberEntity member = teamProvider.findMember(teamId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team."));

        teamProvider.removeMember(member.getId());
    }
}
