package com.lifeos.routine.dto;

import java.util.UUID;

/** PUT /routines/{id}/logs/{date} {id?, totalSteps}: Start idempotent theo (routine, ngày) (D242). */
public record StartLogRequest(UUID id, Integer totalSteps) {
}
