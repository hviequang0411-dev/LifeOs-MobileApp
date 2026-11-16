package com.lifeos.sync.dto;

import java.util.List;

/** Phản hồi 200 của POST /sync/push: results đúng thứ tự mutations (SYSTEM_ARCHITECTURE §4.1). */
public record PushResponse(List<MutationResult> results) {
}
