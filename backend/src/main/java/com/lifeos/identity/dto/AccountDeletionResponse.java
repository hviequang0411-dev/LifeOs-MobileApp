package com.lifeos.identity.dto;

import java.time.Instant;

/** Trạng thái chờ xóa: deletionScheduledAt = thời điểm xóa cứng (yêu cầu + 7 ngày); hủy được trước thời điểm đó. */
public record AccountDeletionResponse(Instant deletionScheduledAt, boolean cancellable) {
}
