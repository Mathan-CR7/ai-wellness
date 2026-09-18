package com.kovanlabs.wellness.mapper;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.entity.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ActivityMapper {

    ActivityResponse toResponse(ActivityEntity entity);

    List<ActivityResponse> toResponseList(List<ActivityEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "syncedAt", ignore = true)
    ActivityEntity toEntity(ActivitySyncRequest request);
}
