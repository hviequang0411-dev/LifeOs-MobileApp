package com.lifeos.common.goal;

import java.util.UUID;

/**
 * Đảo phụ thuộc task → goal (PHASE_6 D186): module task kiểm liên kết goalId/milestoneId của Task qua interface này,
 * module goal cài đặt. Gọi trong transaction ghi; mục tiêu/cột mốc được khóa FOR SHARE (không mồ côi khi bị xóa đồng
 * thời). Tham chiếu sai (không tồn tại, đã xóa, của user khác) → ValidationException 422 GOAL_NOT_FOUND /
 * MILESTONE_NOT_FOUND; milestone không thuộc goal → GOAL_LINK_MISMATCH.
 */
public interface GoalLinks {

    /** Liên kết đã kiểm: goalId hiệu lực (từ milestone nếu có) và milestoneId. */
    record Link(UUID goalId, UUID milestoneId) {
    }

    Link resolve(UUID userId, UUID goalIdOrNull, UUID milestoneIdOrNull);

    /** Goal còn sống của user (cho GET /goals/{id}/tasks trả 404 đúng). */
    boolean goalExists(UUID userId, UUID goalId);
}
