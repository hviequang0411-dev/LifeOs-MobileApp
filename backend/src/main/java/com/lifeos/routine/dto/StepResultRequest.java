package com.lifeos.routine.dto;

import java.util.UUID;

/** PUT /routine-logs/{logId}/steps/{stepId} {id?, status}: hoàn thành/bỏ qua bước (D243). */
public record StepResultRequest(UUID id, String status) {
}
