package com.lifeos.finance.repository;

import com.lifeos.finance.entity.FinancialTransaction;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Dữ liệu cá nhân: mọi truy vấn theo id đều kèm userId (SECURITY_RULES §7.2). */
public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {

    @Query("select e from FinancialTransaction e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialTransaction> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from FinancialTransaction e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<FinancialTransaction> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from FinancialTransaction e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from FinancialTransaction e where e.userId = :userId and e.id in :ids")
    List<FinancialTransaction> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("""
            select count(t) > 0 from FinancialTransaction t
            where t.userId = :userId and t.accountId = :accountId and t.deletedAt is null
            """)
    boolean existsLiveForAccount(@Param("userId") UUID userId, @Param("accountId") UUID accountId);

    @Query("""
            select count(t) > 0 from FinancialTransaction t
            where t.userId = :userId and t.categoryId = :categoryId and t.deletedAt is null
            """)
    boolean existsLiveForCategory(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId);
}
