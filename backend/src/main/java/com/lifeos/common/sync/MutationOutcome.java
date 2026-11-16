package com.lifeos.common.sync;

import com.lifeos.common.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kết quả áp dụng một mutation (SYNC_PROTOCOL §2.2, D83; Q1 Giai đoạn 3). DUPLICATE không phải outcome: đó là phản
 * hồi phát lại kết quả đã lưu, do module sync dựng.
 */
public sealed interface MutationOutcome {

    /** Đã áp dụng (hoặc không đổi gì, §4.4); entityVersion là version của entity sau thao tác. */
    record Applied(long entityVersion) implements MutationOutcome {
    }

    /**
     * baseVersion lệch hoặc entity đã là tombstone khi UPDATE; serverSnapshot là trạng thái server lúc đó. reason khác
     * null chỉ ở CREATE trùng khóa tự nhiên (Phase 2 D235: {@link #ALREADY_EXISTS}) — khi đó serverSnapshot là bản ghi
     * ĐANG tồn tại (id khác id của mutation).
     */
    record Conflict(EntitySnapshot serverSnapshot, String reason) implements MutationOutcome {

        public static final String ALREADY_EXISTS = "ALREADY_EXISTS";

        public Conflict {
            Objects.requireNonNull(serverSnapshot, "serverSnapshot");
        }

        public Conflict(EntitySnapshot serverSnapshot) {
            this(serverSnapshot, null);
        }
    }

    /**
     * Bị từ chối vì lý do nghiệp vụ. fieldErrors ({@code [{field, code}]}) chỉ có khi errorCode = VALIDATION_FAILED;
     * message cho client lấy từ {@link ErrorCode#title()} (câu chung, không chứa dữ liệu người dùng).
     */
    record Rejected(ErrorCode errorCode, List<Map<String, Object>> fieldErrors) implements MutationOutcome {
        public Rejected {
            Objects.requireNonNull(errorCode, "errorCode");
            fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
        }

        public Rejected(ErrorCode errorCode) {
            this(errorCode, List.of());
        }
    }
}
