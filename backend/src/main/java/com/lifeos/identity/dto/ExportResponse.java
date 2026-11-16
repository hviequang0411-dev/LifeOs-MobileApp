package com.lifeos.identity.dto;

import java.time.Instant;
import java.util.UUID;

/** Trạng thái một yêu cầu xuất dữ liệu (D204): PENDING → RUNNING → READY | FAILED → EXPIRED. */
public record ExportResponse(UUID id, String status, Instant requestedAt, Instant finishedAt, Instant expiresAt,
        Long sizeBytes, String errorCode, int downloadsRemaining) {
}
