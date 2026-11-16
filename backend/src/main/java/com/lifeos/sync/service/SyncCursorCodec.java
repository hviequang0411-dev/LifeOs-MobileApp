package com.lifeos.sync.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Cursor pull (SYNC_RULES §9.3, SYNC_PROTOCOL §6 mục 3, D89): base64url (không padding) của chuỗi số thập phân
 * {@code seq}; với client là chuỗi mờ. Vắng hoặc rỗng = 0. Không giải mã được, âm, không phải số, hoặc vượt long → 400
 * VALIDATION_FAILED (fieldError cursor, INVALID_VALUE).
 */
public final class SyncCursorCodec {

    private static final Pattern DIGITS = Pattern.compile("^[0-9]{1,19}$");

    private SyncCursorCodec() {
    }

    public static String encode(long seq) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(seq).getBytes(StandardCharsets.US_ASCII));
    }

    public static long decode(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return 0;
        }
        try {
            String text = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII);
            if (DIGITS.matcher(text).matches()) {
                return Long.parseLong(text);
            }
        } catch (IllegalArgumentException e) {
            // rơi xuống lỗi chung (NumberFormatException là IllegalArgumentException)
        }
        throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                "Con trỏ đồng bộ không hợp lệ.");
    }
}
