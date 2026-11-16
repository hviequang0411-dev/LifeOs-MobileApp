package com.lifeos.finance.repository;

import com.lifeos.finance.entity.SavingGoal;
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
public interface SavingGoalRepository extends JpaRepository<SavingGoal, UUID> {

    @Query("select e from SavingGoal e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<SavingGoal> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: ghi/xóa cần kiểm điều kiện trên dữ liệu con hoặc tuần tự hóa (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from SavingGoal e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<SavingGoal> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from SavingGoal e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from SavingGoal e where e.userId = :userId and e.id in :ids")
    List<SavingGoal> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** Danh sách theo keyset createdAt giảm dần rồi id giảm dần (D79). archived: false | true | all. */
    @Query("""
            select e from SavingGoal e
            where e.userId = :userId and e.deletedAt is null
              and (:status is null or e.status = :status)
              and (:first = true
                   or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<SavingGoal> page(@Param("userId") UUID userId, @Param("status") String status, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);
}
