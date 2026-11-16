package com.lifeos.finance.dto;

import java.util.UUID;

/** Trạng thái entity Finance cho snapshot sync (kể cả tombstone). response là DTO REST của entity. */
public record FinanceSyncState(Object response, UUID id, long version, boolean deleted) {
}
