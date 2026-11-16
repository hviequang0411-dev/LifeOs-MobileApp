package com.lifeos.task.repository;

import com.lifeos.task.entity.Task;
import com.lifeos.task.entity.TaskPriority;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Danh sách Task theo keyset (API_CONVENTIONS §7, D59, D79) bằng JPA Criteria (D30: JPA cho bảng domain). Luôn lọc
 * user_id và deleted_at IS NULL ở tầng repository (SECURITY_RULES §7.7).
 * <p>
 * Thứ tự: khóa sắp xếp rồi id CÙNG chiều. priority theo hạng nghiệp vụ NONE(0) < LOW(1) < MEDIUM(2) < HIGH(3) (DB lưu
 * chuỗi nên dùng CASE). dueDate null luôn ở CUỐI ở cả hai chiều: sắp thêm theo "dueDate IS NULL" trước, và điều kiện
 * keyset chia hai vùng (có ngày / không ngày).
 */
@Repository
public class TaskListRepository {

    private final EntityManager em;

    public TaskListRepository(EntityManager em) {
        this.em = em;
    }

    /** Trả tối đa limit + 1 bản ghi (bên gọi dùng bản dư để biết hasMore). */
    public List<Task> page(TaskListCriteria c) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Task> q = cb.createQuery(Task.class);
        Root<Task> t = q.from(Task.class);

        List<Predicate> where = new ArrayList<>();
        where.add(cb.equal(t.get("userId"), c.userId()));
        where.add(cb.isNull(t.get("deletedAt")));
        if (c.status() != null) {
            where.add(cb.equal(t.get("status"), c.status()));
        }
        if (c.projectId() != null) {
            where.add(cb.equal(t.get("projectId"), c.projectId()));
        }
        if (Boolean.TRUE.equals(c.inbox())) {
            where.add(cb.isNull(t.get("projectId")));
        } else if (Boolean.FALSE.equals(c.inbox())) {
            where.add(cb.isNotNull(t.get("projectId")));
        }
        if (c.categoryId() != null) {
            where.add(cb.equal(t.get("categoryId"), c.categoryId()));
        }
        if (c.priority() != null) {
            where.add(cb.equal(t.get("priority"), c.priority()));
        }
        Path<LocalDate> dueDate = t.get("dueDate");
        if (c.dueFrom() != null) {
            where.add(cb.greaterThanOrEqualTo(dueDate, c.dueFrom()));
        }
        if (c.dueTo() != null) {
            where.add(cb.lessThanOrEqualTo(dueDate, c.dueTo()));
        }

        Path<UUID> id = t.get("id");
        List<Order> order = new ArrayList<>();
        if ("dueDate".equals(c.sortField())) {
            Expression<Integer> nullsLast = cb.<Integer>selectCase().when(cb.isNull(dueDate), 1).otherwise(0);
            order.add(cb.asc(nullsLast));
            order.add(c.ascending() ? cb.asc(dueDate) : cb.desc(dueDate));
            if (c.afterId() != null) {
                where.add(dueDateKeyset(cb, dueDate, id, (LocalDate) c.afterKey(), c.afterId(), c.ascending()));
            }
        } else {
            Expression<? extends Comparable> key = sortKey(cb, t, c.sortField());
            order.add(c.ascending() ? cb.asc(key) : cb.desc(key));
            if (c.afterId() != null) {
                where.add(keyset(cb, key, id, (Comparable) c.afterKey(), c.afterId(), c.ascending()));
            }
        }
        order.add(c.ascending() ? cb.asc(id) : cb.desc(id));

        q.select(t).where(where.toArray(Predicate[]::new)).orderBy(order);
        return em.createQuery(q).setMaxResults(c.limit() + 1).getResultList();
    }

    /** Khóa sắp xếp không null: createdAt, updatedAt, hoặc hạng priority. */
    @SuppressWarnings("rawtypes")
    private static Expression<? extends Comparable> sortKey(CriteriaBuilder cb, Root<Task> t, String field) {
        return switch (field) {
            case "createdAt" -> t.<Instant>get("createdAt");
            case "updatedAt" -> t.<Instant>get("updatedAt");
            case "priority" -> priorityRank(cb, t.get("priority"));
            default -> throw new IllegalArgumentException("sort: " + field);
        };
    }

    /** Hạng nghiệp vụ của priority: NONE 0 < LOW 1 < MEDIUM 2 < HIGH 3 (D79). */
    public static Expression<Integer> priorityRank(CriteriaBuilder cb, Path<TaskPriority> priority) {
        CriteriaBuilder.SimpleCase<TaskPriority, Integer> rank = cb.selectCase(priority);
        for (TaskPriority p : TaskPriority.values()) {
            rank = rank.when(p, p.ordinal());
        }
        return rank.otherwise(0);
    }

    /** (key, id) đứng SAU (afterKey, afterId) theo chiều đã chọn. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Predicate keyset(CriteriaBuilder cb, Expression key, Path<UUID> id, Comparable afterKey,
            UUID afterId, boolean ascending) {
        if (ascending) {
            return cb.or(cb.greaterThan(key, afterKey),
                    cb.and(cb.equal(key, afterKey), cb.greaterThan(id, afterId)));
        }
        return cb.or(cb.lessThan(key, afterKey), cb.and(cb.equal(key, afterKey), cb.lessThan(id, afterId)));
    }

    /**
     * Keyset cho dueDate với null cuối: nếu bản ghi cuối có ngày → các bản có ngày đứng sau nó, rồi TOÀN BỘ bản không
     * ngày; nếu bản ghi cuối không ngày → chỉ các bản không ngày có id đứng sau.
     */
    private static Predicate dueDateKeyset(CriteriaBuilder cb, Path<LocalDate> dueDate, Path<UUID> id,
            LocalDate afterKey, UUID afterId, boolean ascending) {
        Predicate idAfter = ascending ? cb.greaterThan(id, afterId) : cb.lessThan(id, afterId);
        if (afterKey == null) {
            return cb.and(cb.isNull(dueDate), idAfter);
        }
        Predicate dateAfter = ascending ? cb.greaterThan(dueDate, afterKey) : cb.lessThan(dueDate, afterKey);
        return cb.or(
                cb.and(cb.isNotNull(dueDate), cb.or(dateAfter, cb.and(cb.equal(dueDate, afterKey), idAfter))),
                cb.isNull(dueDate));
    }
}
