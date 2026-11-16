package com.lifeos.routine.repository;

import com.lifeos.routine.entity.Routine;
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

/** Routine của user. Mọi truy vấn theo id kèm userId (SECURITY_RULES §7.2). */
public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    @Query("select r from Routine r where r.id = :id and r.userId = :userId and r.deletedAt is null")
    Optional<Routine> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** FOR UPDATE: xóa Routine khóa trước cascade; ghi bước/lượt khóa Routine để tuần tự theo routine (D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Routine r where r.id = :id and r.userId = :userId and r.deletedAt is null")
    Optional<Routine> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select r.version from Routine r where r.id = :id and r.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select count(r) from Routine r where r.userId = :userId and r.deletedAt is null")
    long countLive(@Param("userId") UUID userId);

    @Query("select r from Routine r where r.userId = :userId and r.id in :ids")
    List<Routine> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("""
            select r from Routine r
            where r.userId = :userId and r.deletedAt is null
              and (:archived = 'all'
                   or (:archived = 'true' and r.archivedAt is not null)
                   or (:archived = 'false' and r.archivedAt is null))
              and (:first = true
                   or r.createdAt < :afterCreatedAt
                   or (r.createdAt = :afterCreatedAt and r.id < :afterId))
            order by r.createdAt desc, r.id desc
            """)
    List<Routine> page(@Param("userId") UUID userId, @Param("archived") String archived,
            @Param("first") boolean first, @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId, Limit limit);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
    @org.springframework.data.jpa.repository.Query(
            "select r from Routine r where r.id = :id and r.userId = :userId and r.deletedAt is null")
    java.util.Optional<com.lifeos.routine.entity.Routine> findActiveForShare(
            @org.springframework.data.repository.query.Param("id") java.util.UUID id,
            @org.springframework.data.repository.query.Param("userId") java.util.UUID userId);
}
