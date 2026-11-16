package com.lifeos.common.exception;

import java.util.UUID;

/**
 * CREATE trùng khóa tự nhiên với một bản ghi ĐANG hoạt động (Phase 2 D235: check-in cùng habit + ngày; tương tự lượt
 * chạy routine theo ngày, kết quả bước theo lượt). Mang id của bản ghi đang tồn tại để sync trả CONFLICT
 * {@code ALREADY_EXISTS} kèm snapshot của bản đó. REST upsert theo ngày không gặp lỗi này.
 */
public class NaturalKeyConflictException extends ConflictException {

    private final UUID existingId;

    public NaturalKeyConflictException(UUID existingId) {
        this(ErrorCode.ENTITY_ALREADY_EXISTS, existingId);
    }

    /** Mã REST riêng (vd PHASE_6 TAG_NAME_TAKEN 409); sync vẫn trả CONFLICT ALREADY_EXISTS. */
    public NaturalKeyConflictException(ErrorCode errorCode, UUID existingId) {
        super(errorCode);
        this.existingId = existingId;
    }

    public UUID existingId() {
        return existingId;
    }
}
