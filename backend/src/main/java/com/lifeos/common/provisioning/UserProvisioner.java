package com.lifeos.common.provisioning;

import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu mặc định của một người dùng do module nghiệp vụ sở hữu (ví dụ danh mục thu/chi hệ thống, PHASE_3 D113).
 * identity gọi lúc đăng ký (cùng transaction) và trong job bù khi khởi động; module nghiệp vụ cài đặt — đảo phụ thuộc
 * như ChangeRecorder, identity không biết module nào.
 * <p>
 * Thứ tự khóa D86: {@link #provision} chỉ GHI dữ liệu nghiệp vụ (và flush), KHÔNG gọi ChangeRecorder; người gọi ghi
 * change_logs cho các {@link PendingChange} trả về SAU mọi ghi nghiệp vụ của transaction.
 */
public interface UserProvisioner {

    /**
     * Tạo phần dữ liệu mặc định còn thiếu. Idempotent: gọi lại không tạo trùng.
     *
     * @return thay đổi cần ghi change_logs (CREATE, mutation_id NULL)
     */
    List<PendingChange> provision(UUID userId);

    /** Một bản ghi đồng bộ vừa được tạo, chờ ghi change_logs. */
    record PendingChange(String entityType, UUID entityId, long version) {
    }
}
