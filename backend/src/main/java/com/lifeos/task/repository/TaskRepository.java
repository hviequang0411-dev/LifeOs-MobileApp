package com.lifeos.task.repository;

import com.lifeos.task.entity.Task;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Dữ liệu cá nhân: mọi truy vấn đều kèm userId (SECURITY_RULES §7.2); truy vấn thường bỏ bản ghi đã xóa. */
public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Query("select t from Task t where t.id = :id and t.userId = :userId and t.deletedAt is null")
    Optional<Task> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** version hiện tại kể cả khi đã xóa mềm (báo currentVersion khi xung đột). */
    @Query("select t.version from Task t where t.id = :id and t.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Tiến độ Project (FR-PJ-05, Q6 DATABASE_DESIGN §6): Task đã xóa không tính. */
    @Query("""
            select new com.lifeos.task.repository.ProjectProgressCounts(count(t),
                   sum(case when t.status = com.lifeos.task.entity.TaskStatus.DONE then 1L else 0L end))
            from Task t
            where t.userId = :userId and t.projectId = :projectId and t.deletedAt is null
            """)
    ProjectProgressCounts progress(@Param("userId") UUID userId, @Param("projectId") UUID projectId);

    /** Task còn sống của một Project, cho cascade khi xóa Project (Q7 DATABASE_DESIGN §6, ix_tasks_project). */
    @Query("select t from Task t where t.userId = :userId and t.projectId = :projectId and t.deletedAt is null"
            + " order by t.id")
    List<Task> findLiveByProject(@Param("userId") UUID userId, @Param("projectId") UUID projectId);

    /** Task còn sống của một Category, cho cascade khi xóa Category (ix_tasks_category). */
    @Query("select t from Task t where t.userId = :userId and t.categoryId = :categoryId and t.deletedAt is null"
            + " order by t.id")
    List<Task> findLiveByCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);

    /** Mọi bản ghi (kể cả đã xóa mềm) của user trong danh sách id — snapshot cho sync (SYNC_PROTOCOL §6). */
    @Query("select t from Task t where t.userId = :userId and t.id in :ids")
    List<Task> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** FOR SHARE: dữ liệu con của module khác (time block, reminder) tham chiếu Task (TaskApi.lockForShare). */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @Query("select t from Task t where t.id = :id and t.userId = :userId and t.deletedAt is null")
    java.util.Optional<Task> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select t from Task t
            where t.userId = :userId and t.deletedAt is null and t.dueDate >= :from and t.dueDate <= :to
            order by t.dueDate, t.id
            """)
    List<Task> dueBetween(@Param("userId") UUID userId, @Param("from") java.time.LocalDate from,
            @Param("to") java.time.LocalDate to, org.springframework.data.domain.Limit limit);

    @Query("select t from Task t where t.userId = :userId and t.goalId = :goalId and t.deletedAt is null order by t.id")
    List<Task> findLiveByGoal(@Param("userId") UUID userId, @Param("goalId") UUID goalId);

    @Query("""
            select t from Task t where t.userId = :userId and t.milestoneId = :milestoneId and t.deletedAt is null
            order by t.id
            """)
    List<Task> findLiveByMilestone(@Param("userId") UUID userId, @Param("milestoneId") UUID milestoneId);

    /** Hàng (goalId, milestoneId, done, total) cho tiến độ Goal. */
    @Query("""
            select t.goalId, t.milestoneId,
                   sum(case when t.status = com.lifeos.task.entity.TaskStatus.DONE then 1 else 0 end), count(t)
            from Task t
            where t.userId = :userId and t.deletedAt is null and t.goalId in :goalIds
            group by t.goalId, t.milestoneId
            """)
    List<Object[]> goalCounts(@Param("userId") UUID userId, @Param("goalIds") Collection<UUID> goalIds);

    @Query("""
            select t from Task t where t.userId = :userId and t.goalId = :goalId and t.deletedAt is null
              and (:first = true or t.id > :afterId)
            order by t.id
            """)
    List<Task> pageByGoal(@Param("userId") UUID userId, @Param("goalId") UUID goalId, @Param("first") boolean first,
            @Param("afterId") UUID afterId, org.springframework.data.domain.Limit limit);

    /** PHASE_7 D198: (createdAt, completedAt, dueDate, dueTime, projectId, categoryId) của Task hoàn thành trong kỳ. */
    @Query("""
            select t.createdAt, t.completedAt, t.dueDate, t.dueTime, t.projectId, t.categoryId from Task t
            where t.userId = :userId and t.deletedAt is null and t.completedAt >= :from and t.completedAt < :to
            order by t.completedAt
            """)
    List<Object[]> completedBetween(@Param("userId") UUID userId, @Param("from") java.time.Instant from,
            @Param("to") java.time.Instant to, org.springframework.data.domain.Limit limit);

    @Query("""
            select count(t) from Task t where t.userId = :userId and t.deletedAt is null
              and t.status = com.lifeos.task.entity.TaskStatus.OPEN and t.dueDate < :today
            """)
    long overdueOpen(@Param("userId") UUID userId, @Param("today") java.time.LocalDate today);
}
