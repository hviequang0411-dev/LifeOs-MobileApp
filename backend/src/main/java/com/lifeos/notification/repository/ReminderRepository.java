package com.lifeos.notification.repository;

import com.lifeos.notification.entity.Reminder;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Dữ liệu cá nhân: mọi truy vấn theo id đều kèm userId (SECURITY_RULES §7.2). */
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    @Query("select e from Reminder e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<Reminder> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Reminder e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<Reminder> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from Reminder e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<Reminder> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from Reminder e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from Reminder e where e.userId = :userId and e.id in :ids")
    List<Reminder> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("select count(e) from Reminder e where e.userId = :userId and e.deletedAt is null")
    long countLive(@Param("userId") UUID userId);

    /** Danh sách keyset createdAt giảm dần rồi id giảm dần (D79). */
    @Query("""
            select e from Reminder e
            where e.userId = :userId and e.deletedAt is null
              and (:first = true or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<Reminder> page(@Param("userId") UUID userId, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);

    @Query("""
            select r from Reminder r
            where r.userId = :userId and r.targetType = :type and r.targetId = :targetId and r.deletedAt is null
            order by r.id
            """)
    List<Reminder> findLiveByTarget(@Param("userId") UUID userId, @Param("type") String type,
            @Param("targetId") UUID targetId);

    @Query("""
            select count(r) from Reminder r
            where r.userId = :userId and r.targetType = :type and r.targetId = :targetId and r.deletedAt is null
            """)
    long countLiveByTarget(@Param("userId") UUID userId, @Param("type") String type, @Param("targetId") UUID targetId);

    @Query("select count(r) from Reminder r where r.userId = :userId and r.deletedAt is null and r.status = 'ACTIVE'")
    long countActive(@Param("userId") UUID userId);

    /** Reminder phụ thuộc múi giờ người dùng (LOCAL_AT và theo hạn Task) — tính lại khi đổi múi giờ (D134). */
    @Query("""
            select r from Reminder r
            where r.userId = :userId and r.deletedAt is null and r.status = 'ACTIVE'
              and (r.mode = 'LOCAL_AT' or r.targetType = 'TASK')
            order by r.id
            """)
    List<Reminder> findLocalAtActive(@Param("userId") UUID userId);

    /** Đến hạn (D131): ACTIVE, next_fire_at ≤ now. */
    @Query("""
            select r.id from Reminder r
            where r.status = 'ACTIVE' and r.deletedAt is null and r.nextFireAt <= :now order by r.nextFireAt, r.id
            """)
    List<UUID> findDueIds(@Param("now") Instant now, Limit limit);

    /** Khóa reminder cho job, bỏ qua nếu luồng khác đang giữ (FOR UPDATE SKIP LOCKED, D131). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(
            @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from Reminder r where r.id = :id and r.deletedAt is null")
    Optional<Reminder> lockForJob(@Param("id") UUID id);

    /** Reminder LOCAL_AT chưa có next_fire_at (vd tạo bởi migration V7 từ reminder_time của Habit/Routine). */
    @Query("""
            select r.id from Reminder r
            where r.status = 'ACTIVE' and r.deletedAt is null and r.nextFireAt is null and r.mode = 'LOCAL_AT'
            order by r.id
            """)
    List<UUID> findUnscheduledIds(Limit limit);
}
