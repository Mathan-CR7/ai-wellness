package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.team.TeamCreateRequest;
import com.kovanlabs.wellness.dto.team.TeamMemberResponse;
import com.kovanlabs.wellness.dto.team.TeamResponse;

import java.util.List;

public interface TeamService {

    TeamResponse createTeam(Long ownerId, TeamCreateRequest request);

    TeamResponse getTeamById(Long teamId);

    TeamResponse joinTeamByInviteCode(Long userId, String inviteCode);

    List<TeamMemberResponse> getTeamMembers(Long teamId);

    List<TeamResponse> getUserTeams(Long userId);

    void removeMember(Long teamId, Long requesterId, Long memberId);
}
