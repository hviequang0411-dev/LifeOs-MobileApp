package com.lifeos.finance.repository;

import com.lifeos.finance.entity.SavingContribution;
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
public interface SavingContributionRepository extends JpaRepository<SavingContribution, UUID> {

    @Query("select e from SavingContribution e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<SavingContribution> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from SavingContribution e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<SavingContribution> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from SavingContribution e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from SavingContribution e where e.userId = :userId and e.id in :ids")
    List<SavingContribution> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("""
            select c from SavingContribution c
            where c.userId = :userId and c.goalId = :goalId and c.deletedAt is null
            order by c.id
            """)
    List<SavingContribution> findLiveByGoal(@Param("userId") UUID userId, @Param("goalId") UUID goalId);

    @Query("""
            select c from SavingContribution c
            where c.userId = :userId and c.goalId = :goalId and c.deletedAt is null
              and (:first = true
                   or c.occurredOn < :afterDate
                   or (c.occurredOn = :afterDate and c.id < :afterId))
            order by c.occurredOn desc, c.id desc
            """)
    List<SavingContribution> page(@Param("userId") UUID userId, @Param("goalId") UUID goalId,
            @Param("first") boolean first, @Param("afterDate") LocalDate afterDate, @Param("afterId") UUID afterId,
            Limit limit);
}
