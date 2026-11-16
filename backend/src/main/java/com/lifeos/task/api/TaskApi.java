package com.lifeos.task.api;

import java.util.Optional;
import java.util.UUID;

/** API công khai của module task (chỉ đọc). Mọi truy vấn theo userId (IDOR). */
public interface TaskApi {

    /** Task còn sống của user; rỗng nếu không có / đã xóa / của người khác. */
    Optional<TaskSchedule> schedule(UUID userId, UUID taskId);

    /**
     * Như {@link #schedule} nhưng khóa dòng Task FOR SHARE đến hết transaction của bên gọi: dữ liệu con (time block,
     * reminder) tạo song song với xóa Task không bị mồ côi (D80, D86). Phải gọi trong transaction.
     */
    Optional<TaskSchedule> lockForShare(UUID userId, UUID taskId);

    /** Task còn sống có hạn trong [from, to] (cửa sổ lịch hợp nhất D145), tối đa {@code limit}. */
    java.util.List<TaskDue> dueBetween(UUID userId, java.time.LocalDate from, java.time.LocalDate to, int limit);

    /** Tiêu đề Task để hiển thị nội dung thông báo (D138: chỉ dùng khi người dùng bật hiện chi tiết). */
    Optional<String> title(UUID userId, UUID taskId);

    /**
     * Cascade xóa Goal (D186): gỡ goal_id/milestone_id của mọi Task còn sống thuộc goal, version +1; ghi nghiệp vụ và
     * flush, change_logs UPDATE (mutation_id NULL) được GOM vào {@code changes} để module goal ghi sau cùng (D86).
     */
    void detachFromGoal(java.util.UUID userId, java.util.UUID goalId, com.lifeos.common.sync.PendingChanges changes);

    /** Cascade xóa Milestone: gỡ milestone_id (giữ goal_id) của Task thuộc milestone. */
    void detachFromMilestone(java.util.UUID userId, java.util.UUID milestoneId,
            com.lifeos.common.sync.PendingChanges changes);

    /** Đếm Task theo (goal, milestone) cho các goal (D185). */
    java.util.List<GoalTaskCount> goalCounts(java.util.UUID userId, java.util.Collection<java.util.UUID> goalIds);
}
