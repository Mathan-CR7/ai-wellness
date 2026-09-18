package com.kovanlabs.wellness.mapper;

import com.kovanlabs.wellness.dto.exercise.ExerciseLogRequest;
import com.kovanlabs.wellness.dto.exercise.ExerciseResponse;
import com.kovanlabs.wellness.entity.ExerciseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExerciseMapper {

    ExerciseResponse toResponse(ExerciseEntity entity);

    List<ExerciseResponse> toResponseList(List<ExerciseEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    ExerciseEntity toEntity(ExerciseLogRequest request);
}
