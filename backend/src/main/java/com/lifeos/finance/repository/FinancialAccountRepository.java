package com.lifeos.finance.repository;

import com.lifeos.finance.entity.FinancialAccount;
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
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, UUID> {

    @Query("select e from FinancialAccount e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialAccount> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from FinancialAccount e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialAccount> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from FinancialAccount e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from FinancialAccount e where e.userId = :userId and e.id in :ids")
    List<FinancialAccount> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /**
     * SELECT … FOR SHARE: bản ghi được THAM CHIẾU bởi một lệnh ghi khác (giao dịch → tài khoản/danh mục…). Chặn xóa/đổi
     * tiền tệ đồng thời (bên đó khóa FOR UPDATE rồi mới kiểm "đang được dùng") nhưng không tuần tự hóa các lệnh ghi
     * tham chiếu với nhau.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from FinancialAccount e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialAccount> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Danh sách theo keyset createdAt giảm dần rồi id giảm dần (D79). archived: false | true | all. */
    @Query("""
            select e from FinancialAccount e
            where e.userId = :userId and e.deletedAt is null
              and (:archived = 'all'
                   or (:archived = 'true' and e.archivedAt is not null)
                   or (:archived = 'false' and e.archivedAt is null))
              and (:first = true
                   or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<FinancialAccount> page(@Param("userId") UUID userId, @Param("archived") String archived, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);

    @Query("select a from FinancialAccount a where a.userId = :userId and a.deletedAt is null")
    List<FinancialAccount> findAllLive(@Param("userId") UUID userId);

    @Query("""
            select count(a) > 0 from FinancialAccount a
            where a.userId = :userId and a.deletedAt is null and lower(a.name) = lower(:name) and a.id <> :exceptId
            """)
    boolean nameTaken(@Param("userId") UUID userId, @Param("name") String name, @Param("exceptId") UUID exceptId);
}
