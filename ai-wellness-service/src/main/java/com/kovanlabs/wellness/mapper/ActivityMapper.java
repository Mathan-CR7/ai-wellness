package com.kovanlabs.wellness.mapper;

import com.kovanlabs.wellness.dto.activity.ActivityResponse;
import com.kovanlabs.wellness.dto.activity.ActivitySyncRequest;
import com.kovanlabs.wellness.entity.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ActivityMapper {

    ActivityResponse toResponse(ActivityEntity entity);

    List<ActivityResponse> toResponseList(List<ActivityEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "syncedAt", ignore = true)
    @Mapping(target = "stepCount", expression = "java(request.getEffectiveStepCount() != null ? request.getEffectiveStepCount().intValue() : 0)")
    @Mapping(target = "startTime", qualifiedByName = "mapObjectToInstant", source = "startTime")
    @Mapping(target = "endTime", qualifiedByName = "mapObjectToInstant", source = "endTime")
    ActivityEntity toEntity(ActivitySyncRequest request);

    @Named("mapObjectToInstant")
    default Instant mapObjectToInstant(Object obj) {
        if (obj == null) return Instant.now();
        if (obj instanceof Instant) return (Instant) obj;
        if (obj instanceof Number) return Instant.ofEpochMilli(((Number) obj).longValue());
        String str = obj.toString();
        try {
            return Instant.parse(str);
        } catch (Exception e) {
            try {
                return LocalDate.parse(str.substring(0, 10)).atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {}
        }
        return Instant.now();
    }
}
