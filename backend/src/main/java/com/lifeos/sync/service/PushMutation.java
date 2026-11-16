package com.lifeos.sync.service;

import com.lifeos.common.sync.Mutation;

/**
 * Một mutation đã qua kiểm tra cấu trúc (SYNC_PROTOCOL §2.1) kèm kích thước payload sau khi serialize UTF-8 (§3 quy tắc
 * 6: > payload-max-bytes → REJECTED PAYLOAD_TOO_LARGE, kiểm trước handler).
 */
public record PushMutation(Mutation mutation, int payloadBytes) {
}
