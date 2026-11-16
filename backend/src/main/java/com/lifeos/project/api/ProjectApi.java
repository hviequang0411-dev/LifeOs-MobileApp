package com.lifeos.project.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Mặt tiền công khai của module project (JAVA_SPRING_RULES §4.3–4.4): module task kiểm Project tồn tại và thuộc user
 * qua đây, không truy cập repository/entity của project. Project của user khác, không tồn tại hay đã xóa đều trả rỗng
 * (không phân biệt). Bên gọi tự chọn mã lỗi: 404 khi là tài nguyên của URL, 422 khi là tham chiếu trong body (D61).
 */
public interface ProjectApi {

    /** Đọc thường (vd GET /projects/{id}/progress). */
    Optional<ProjectRef> findActiveOwned(UUID userId, UUID projectId);

    /**
     * Kiểm tham chiếu khi ghi Task: khóa dòng Project bằng FOR SHARE đến hết transaction của bên gọi, để việc xóa
     * Project đồng thời (FOR UPDATE) phải chờ — không tạo Task trỏ tới Project vừa xóa (D80). Bắt buộc gọi trong
     * transaction. Thứ tự khóa: luôn Project trước Category.
     */
    Optional<ProjectRef> lockActiveOwned(UUID userId, UUID projectId);
}
