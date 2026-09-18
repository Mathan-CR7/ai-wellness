package com.kovanlabs.wellness.mapper;

import com.kovanlabs.wellness.dto.team.TeamCreateRequest;
import com.kovanlabs.wellness.dto.team.TeamMemberResponse;
import com.kovanlabs.wellness.dto.team.TeamResponse;
import com.kovanlabs.wellness.entity.TeamEntity;
import com.kovanlabs.wellness.entity.TeamMemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TeamMapper {

    @Mapping(target = "memberCount", ignore = true)
    TeamResponse toResponse(TeamEntity entity);

    List<TeamResponse> toResponseList(List<TeamEntity> entities);

    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "email", ignore = true)
    TeamMemberResponse toMemberResponse(TeamMemberEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inviteCode", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TeamEntity toEntity(TeamCreateRequest request);
}
