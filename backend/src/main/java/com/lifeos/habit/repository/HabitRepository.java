package com.lifeos.habit.repository;

import com.lifeos.habit.entity.Habit;
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
public interface HabitRepository extends JpaRepository<Habit, UUID> {

    @Query("select h from Habit h where h.id = :id and h.userId = :userId and h.deletedAt is null")
    Optional<Habit> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * SELECT … FOR UPDATE: xóa Habit khóa dòng trước cascade; ghi check-in cũng khóa dòng Habit để các check-in của một
     * habit tuần tự (upsert theo ngày không đua nhau) và không có check-in mồ côi khi habit bị xóa đồng thời (D80, D86).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habit h where h.id = :id and h.userId = :userId and h.deletedAt is null")
    Optional<Habit> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select h.version from Habit h where h.id = :id and h.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Đếm habit (kể cả lưu trữ, trừ đã xóa) cho giới hạn D237. */
    @Query("select count(h) from Habit h where h.userId = :userId and h.deletedAt is null")
    long countLive(@Param("userId") UUID userId);

    @Query("select h from Habit h where h.userId = :userId and h.id in :ids")
    List<Habit> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** Danh sách theo keyset createdAt giảm dần rồi id giảm dần (D79). archived: false | true | all. */
    @Query("""
            select h from Habit h
            where h.userId = :userId and h.deletedAt is null
              and (:archived = 'all'
                   or (:archived = 'true' and h.archivedAt is not null)
                   or (:archived = 'false' and h.archivedAt is null))
              and (:first = true
                   or h.createdAt < :afterCreatedAt
                   or (h.createdAt = :afterCreatedAt and h.id < :afterId))
            order by h.createdAt desc, h.id desc
            """)
    List<Habit> page(@Param("userId") UUID userId, @Param("archived") String archived,
            @Param("first") boolean first, @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId, Limit limit);

    @Query("select h from Habit h where h.userId = :userId and h.deletedAt is null and h.archivedAt is null")
    List<Habit> findAllActiveUnarchived(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select h from Habit h where h.id = :id and h.userId = :userId and h.deletedAt is null")
    Optional<Habit> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);
}
