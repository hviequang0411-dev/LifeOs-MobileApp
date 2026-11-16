package com.lifeos.finance.repository;

import com.lifeos.finance.entity.RecurringTransaction;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
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
public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, UUID> {

    @Query("select e from RecurringTransaction e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<RecurringTransaction> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RecurringTransaction e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<RecurringTransaction> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from RecurringTransaction e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from RecurringTransaction e where e.userId = :userId and e.id in :ids")
    List<RecurringTransaction> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** Danh sách theo keyset createdAt giảm dần rồi id giảm dần (D79). archived: false | true | all. */
    @Query("""
            select e from RecurringTransaction e
            where e.userId = :userId and e.deletedAt is null
              
              and (:first = true
                   or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<RecurringTransaction> page(@Param("userId") UUID userId, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);

    @Query("""
            select count(r) > 0 from RecurringTransaction r
            where r.userId = :userId and r.accountId = :accountId and r.deletedAt is null
            """)
    boolean existsLiveForAccount(@Param("userId") UUID userId, @Param("accountId") UUID accountId);

    /**
     * Đợt sửa F6: khóa FOR UPDATE các định kỳ ACTIVE của một tài khoản (id tăng dần) để tạm dừng khi lưu trữ tài khoản.
     * Gọi TRƯỚC khi khóa tài khoản — cùng thứ tự "định kỳ → tài khoản" với lệnh sửa định kỳ và job.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from RecurringTransaction r
            where r.userId = :userId and r.accountId = :accountId and r.status = 'ACTIVE' and r.deletedAt is null
            order by r.id
            """)
    List<RecurringTransaction> lockActiveForAccount(@Param("userId") UUID userId, @Param("accountId") UUID accountId);

    @Query("""
            select count(r) > 0 from RecurringTransaction r
            where r.userId = :userId and r.categoryId = :categoryId and r.deletedAt is null
            """)
    boolean existsLiveForCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);

    /**
     * Ứng viên cho job (§7): ACTIVE, chưa xóa, next_run_on ≤ ngày muộn nhất có thể là "hôm nay" ở mọi múi giờ (UTC + 1
     * ngày), chưa quá end_on. "Hôm nay" theo múi giờ của chủ được kiểm lại trong transaction sinh giao dịch.
     */
    @Query("""
            select r.id from RecurringTransaction r
            where r.status = 'ACTIVE' and r.deletedAt is null and r.nextRunOn <= :latest
              and (r.endOn is null or r.nextRunOn <= r.endOn)
            order by r.nextRunOn, r.id
            """)
    List<UUID> findDueIds(@Param("latest") LocalDate latest, Limit limit);

    /** Khóa một bản ghi định kỳ cho job, bỏ qua nếu luồng khác đang giữ (FOR UPDATE SKIP LOCKED, §7). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(
            @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select r from RecurringTransaction r where r.id = :id and r.deletedAt is null")
    Optional<RecurringTransaction> lockForJob(@Param("id") UUID id);
}
