package com.lifeos.routine.dto;

import java.util.List;

/** Lượt chạy kèm kết quả từng bước (lịch sử, FR-RTN-04). */
public record LogView(LogResponse log, List<StepLogResponse> steps) {
}
