package com.lifeos.routine.dto;

import java.util.List;

/** GET /routines/{id}: Routine kèm các bước còn sống theo vị trí. */
public record RoutineDetail(RoutineResponse routine, List<StepResponse> steps) {
}
