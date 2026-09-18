package com.kovanlabs.wellness.mapper;

import com.kovanlabs.wellness.dto.challenge.ChallengeCreateRequest;
import com.kovanlabs.wellness.dto.challenge.ChallengeResponse;
import com.kovanlabs.wellness.entity.ChallengeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChallengeMapper {

    ChallengeResponse toResponse(ChallengeEntity entity);

    List<ChallengeResponse> toResponseList(List<ChallengeEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ChallengeEntity toEntity(ChallengeCreateRequest request);
}
