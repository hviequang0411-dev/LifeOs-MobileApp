package com.lifeos.finance.repository;

import com.lifeos.finance.entity.AccountTransfer;
import jakarta.persistence.LockModeType;
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
public interface AccountTransferRepository extends JpaRepository<AccountTransfer, UUID> {

    @Query("select e from AccountTransfer e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<AccountTransfer> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AccountTransfer e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<AccountTransfer> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from AccountTransfer e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from AccountTransfer e where e.userId = :userId and e.id in :ids")
    List<AccountTransfer> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("""
            select count(x) > 0 from AccountTransfer x
            where x.userId = :userId and x.deletedAt is null
              and (x.fromAccountId = :accountId or x.toAccountId = :accountId)
            """)
    boolean existsLiveForAccount(@Param("userId") UUID userId, @Param("accountId") UUID accountId);

    /** Danh sách theo keyset occurredOn giảm dần rồi id giảm dần; accountId lọc cả hai chiều. */
    @Query("""
            select x from AccountTransfer x
            where x.userId = :userId and x.deletedAt is null
              and (:accountId is null or x.fromAccountId = :accountId or x.toAccountId = :accountId)
              and x.occurredOn >= :from and x.occurredOn <= :to
              and (:first = true
                   or x.occurredOn < :afterDate
                   or (x.occurredOn = :afterDate and x.id < :afterId))
            order by x.occurredOn desc, x.id desc
            """)
    List<AccountTransfer> page(@Param("userId") UUID userId, @Param("accountId") UUID accountId,
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("first") boolean first,
            @Param("afterDate") LocalDate afterDate, @Param("afterId") UUID afterId, Limit limit);
}
