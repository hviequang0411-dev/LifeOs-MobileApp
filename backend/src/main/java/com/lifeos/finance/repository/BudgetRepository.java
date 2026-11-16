package com.lifeos.finance.repository;

import com.lifeos.finance.entity.Budget;
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
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @Query("select e from Budget e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<Budget> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Budget e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<Budget> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from Budget e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from Budget e where e.userId = :userId and e.id in :ids")
    List<Budget> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** Danh sách theo keyset createdAt giảm dần rồi id giảm dần (D79). archived: false | true | all. */
    @Query("""
            select e from Budget e
            where e.userId = :userId and e.deletedAt is null
              and (:archived = 'all'
                   or (:archived = 'true' and e.archivedAt is not null)
                   or (:archived = 'false' and e.archivedAt is null))
              and (:first = true
                   or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<Budget> page(@Param("userId") UUID userId, @Param("archived") String archived, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);

    @Query("""
            select count(b) > 0 from Budget b
            where b.userId = :userId and b.categoryId = :categoryId and b.deletedAt is null
            """)
    boolean existsLiveForCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);

    /** Ngân sách đang hoạt động cùng (danh mục hoặc tổng, tiền tệ) — D109, trừ chính nó. */
    @Query("""
            select count(b) > 0 from Budget b
            where b.userId = :userId and b.deletedAt is null and b.archivedAt is null and b.currency = :currency
              and ((:categoryId is null and b.categoryId is null) or b.categoryId = :categoryId)
              and b.id <> :exceptId
            """)
    boolean activeExists(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId,
            @Param("currency") String currency, @Param("exceptId") UUID exceptId);

    /**
     * Ngân sách đang hoạt động bị một khoản chi ảnh hưởng (tổng chi cùng tiền tệ, hoặc đúng danh mục), khóa FOR UPDATE
     * theo id tăng dần (thứ tự khóa cố định, tránh deadlock) để kiểm ngưỡng tuần tự (FIN-13).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from Budget b
            where b.userId = :userId and b.deletedAt is null and b.archivedAt is null and b.currency = :currency
              and (b.categoryId is null or b.categoryId = :categoryId)
            order by b.id
            """)
    List<Budget> lockAffected(@Param("userId") UUID userId, @Param("currency") String currency,
            @Param("categoryId") UUID categoryId);

    @Query("""
            select b from Budget b
            where b.userId = :userId and b.deletedAt is null and b.archivedAt is null
            order by b.createdAt desc, b.id desc
            """)
    List<Budget> findAllActive(@Param("userId") UUID userId);
}
