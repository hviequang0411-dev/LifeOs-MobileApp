package com.lifeos.goal.repository;

import com.lifeos.goal.entity.Goal;
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

/** Goal của user. Mọi truy vấn theo id kèm userId (SECURITY_RULES §7.2). */
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    @Query("select g from Goal g where g.id = :id and g.userId = :userId and g.deletedAt is null")
    Optional<Goal> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** FOR UPDATE: xóa Goal và ghi milestone khóa Goal trước (thứ tự goal → milestone → task, D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goal g where g.id = :id and g.userId = :userId and g.deletedAt is null")
    Optional<Goal> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select g from Goal g where g.id = :id and g.userId = :userId and g.deletedAt is null")
    Optional<Goal> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select g.version from Goal g where g.id = :id and g.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select g from Goal g where g.userId = :userId and g.id in :ids")
    List<Goal> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("""
            select g from Goal g
            where g.userId = :userId and g.deletedAt is null
              and (:status is null or g.status = :status)
              and (:first = true
                   or g.createdAt < :afterCreatedAt
                   or (g.createdAt = :afterCreatedAt and g.id < :afterId))
            order by g.createdAt desc, g.id desc
            """)
    List<Goal> page(@Param("userId") UUID userId, @Param("status") String status, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);
}
