package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import java.time.ZoneId;

/**
 * Timezone phải là tên vùng IANA (DATABASE_RULES §8.1, SECURITY_RULES §9.7). Chỉ chấp nhận ID có trong danh sách
 * vùng của ZoneId: ZoneId.of còn nhận offset như "+07:00" hay "UTC+7", không phải tên IANA.
 */
public final class TimeZones {

    public static final String DEFAULT = "Asia/Ho_Chi_Minh";

    private TimeZones() {
    }

    /** @return timezone hợp lệ; null → DEFAULT */
    public static String requireIanaOrDefault(String timezone, String field) {
        if (timezone == null) {
            return DEFAULT;
        }
        if (!ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, "INVALID_VALUE",
                    "Múi giờ không hợp lệ (cần tên IANA, ví dụ Asia/Ho_Chi_Minh).");
        }
        return timezone;
    }
}
