package com.lifeos.habit.mapper;

import com.lifeos.habit.dto.CheckinResponse;
import com.lifeos.habit.dto.HabitResponse;
import com.lifeos.habit.entity.Habit;
import com.lifeos.habit.entity.HabitCheckin;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Mapper entity → DTO của module habit (D5: MapStruct, unmappedTargetPolicy = ERROR). */
@Mapper
public interface HabitMapper {

    @Mapping(target = "reminderTime", dateFormat = "HH:mm")
    HabitResponse toResponse(Habit habit);

    CheckinResponse toResponse(HabitCheckin checkin);
}
