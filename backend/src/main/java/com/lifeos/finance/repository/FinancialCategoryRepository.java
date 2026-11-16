package com.lifeos.finance.repository;

import com.lifeos.finance.entity.FinancialCategory;
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
public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, UUID> {

    @Query("select e from FinancialCategory e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialCategory> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from FinancialCategory e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialCategory> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from FinancialCategory e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from FinancialCategory e where e.userId = :userId and e.id in :ids")
    List<FinancialCategory> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /**
     * SELECT … FOR SHARE: bản ghi được THAM CHIẾU bởi một lệnh ghi khác (giao dịch → tài khoản/danh mục…). Chặn xóa/đổi
     * tiền tệ đồng thời (bên đó khóa FOR UPDATE rồi mới kiểm "đang được dùng") nhưng không tuần tự hóa các lệnh ghi
     * tham chiếu với nhau.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from FinancialCategory e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialCategory> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Danh sách theo keyset createdAt giảm dần rồi id giảm dần (D79). archived: false | true | all. */
    @Query("""
            select e from FinancialCategory e
            where e.userId = :userId and e.deletedAt is null
              and (:archived = 'all'
                   or (:archived = 'true' and e.archivedAt is not null)
                   or (:archived = 'false' and e.archivedAt is null))
              and (:kind is null or e.kind = :kind)
              and (:first = true
                   or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<FinancialCategory> page(@Param("userId") UUID userId, @Param("archived") String archived, @Param("kind") String kind,
            @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);

    @Query("""
            select count(c) > 0 from FinancialCategory c
            where c.userId = :userId and c.deletedAt is null and c.kind = :kind and lower(c.name) = lower(:name)
              and c.id <> :exceptId
            """)
    boolean nameTaken(@Param("userId") UUID userId, @Param("kind") String kind, @Param("name") String name,
            @Param("exceptId") UUID exceptId);

    @Query("select c from FinancialCategory c where c.userId = :userId and c.deletedAt is null and c.systemKey is not null")
    List<FinancialCategory> findSystem(@Param("userId") UUID userId);

    /** Danh mục ADJUSTMENT (kể cả đã xóa) của user: loại khỏi ngân sách và báo cáo thu/chi (D119). */
    @Query("select c.id from FinancialCategory c where c.userId = :userId and c.systemKey = 'ADJUSTMENT'")
    List<UUID> findAdjustmentIds(@Param("userId") UUID userId);
}
