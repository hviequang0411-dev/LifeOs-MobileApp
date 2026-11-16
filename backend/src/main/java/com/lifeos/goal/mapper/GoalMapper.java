package com.lifeos.goal.mapper;

import com.lifeos.goal.dto.GoalResponse;
import com.lifeos.goal.dto.MilestoneResponse;
import com.lifeos.goal.entity.Goal;
import com.lifeos.goal.entity.Milestone;
import org.mapstruct.Mapper;

/** Mapper entity → DTO của module goal (D5). */
@Mapper
public interface GoalMapper {

    GoalResponse toResponse(Goal goal);

    MilestoneResponse toResponse(Milestone milestone);
}
