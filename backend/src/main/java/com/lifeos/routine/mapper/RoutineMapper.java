package com.lifeos.routine.mapper;

import com.lifeos.routine.dto.LogResponse;
import com.lifeos.routine.dto.RoutineResponse;
import com.lifeos.routine.dto.StepLogResponse;
import com.lifeos.routine.dto.StepResponse;
import com.lifeos.routine.entity.Routine;
import com.lifeos.routine.entity.RoutineLog;
import com.lifeos.routine.entity.RoutineStep;
import com.lifeos.routine.entity.RoutineStepLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Mapper entity → DTO của module routine (D5). */
@Mapper
public interface RoutineMapper {

    @Mapping(target = "reminderTime", dateFormat = "HH:mm")
    RoutineResponse toResponse(Routine routine);

    StepResponse toResponse(RoutineStep step);

    LogResponse toResponse(RoutineLog log);

    StepLogResponse toResponse(RoutineStepLog stepLog);
}
