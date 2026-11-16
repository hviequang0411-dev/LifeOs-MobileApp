package com.lifeos.common.sync;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapter giữa module sync và module nghiệp vụ (SYSTEM_ARCHITECTURE.md §3.3). Handler chỉ dịch Mutation thành
 * lệnh của Application Service cùng module; KHÔNG chứa business rule, KHÔNG mở transaction (module sync mở T1).
 */
public interface SyncEntityHandler {

    /** Ví dụ: "task", "project", "category", "user_settings". */
    String entityType();

    /**
     * Thao tác có được phép với entityType này không (SYNC_PROTOCOL §3.4: user_settings chỉ UPDATE). Không được phép
     * → REJECTED SYNC_OPERATION_NOT_ALLOWED, kiểm trước baseVersion của CREATE (thứ tự D93).
     */
    default boolean supports(ChangeOperation operation) {
        return true;
    }

    /**
     * Giới hạn byte payload của một mutation loại này (SYNC_PROTOCOL §8). Mặc định giới hạn chung; PHASE_6 D181 nâng
     * riêng cho note (192 KB). Body 1 MB và 50 mutation/yêu cầu không đổi.
     */
    default int maxPayloadBytes(int defaultMaxBytes) {
        return defaultMaxBytes;
    }

    /**
     * Gọi Application Service trong transaction T1 của module sync. Thành công trả {@link MutationOutcome.Applied};
     * lỗi nghiệp vụ của service được NÉM ra nguyên dạng (BusinessException) để module sync ánh xạ sau khi T1 rollback
     * (SYNC_PROTOCOL §5). Trường payload sai kiểu được đưa vào lệnh dưới dạng {@code Patch.invalid} và chỉ nổ ở bước
     * validation của service, giữ thứ tự D96 (tồn tại → version → giá trị).
     */
    MutationOutcome apply(UUID userId, Mutation mutation);

    /** Trạng thái hiện tại (kể cả tombstone) của các entity thuộc userId; id không thuộc user thì không trả. */
    List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds);

    /**
     * Bảng PostgreSQL của entity (có cột chuẩn đồng bộ id, user_id, version, deleted_at). Module sync chỉ dùng để liệt kê
     * id còn sống (snapshot D152), xóa cứng tombstone hết hạn (D156) và kiểm tra nhất quán (D172) — không đọc cột
     * nghiệp vụ; dữ liệu luôn lấy qua {@link #loadSnapshots}.
     */
    String table();
}
