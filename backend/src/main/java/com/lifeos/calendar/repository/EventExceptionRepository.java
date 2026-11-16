package com.lifeos.calendar.repository;

import com.lifeos.calendar.entity.EventException;
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
public interface EventExceptionRepository extends JpaRepository<EventException, UUID> {

    @Query("select e from EventException e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<EventException> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EventException e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<EventException> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from EventException e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<EventException> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from EventException e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from EventException e where e.userId = :userId and e.id in :ids")
    List<EventException> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("select count(e) from EventException e where e.userId = :userId and e.deletedAt is null")
    long countLive(@Param("userId") UUID userId);

    /** Danh sách keyset createdAt giảm dần rồi id giảm dần (D79). */
    @Query("""
            select e from EventException e
            where e.userId = :userId and e.deletedAt is null
              and (:first = true or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<EventException> page(@Param("userId") UUID userId, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);

    @Query("""
            select x from EventException x
            where x.userId = :userId and x.eventId = :eventId and x.deletedAt is null order by x.id
            """)
    List<EventException> findLiveByEvent(@Param("userId") UUID userId, @Param("eventId") UUID eventId);

    @Query("""
            select x from EventException x
            where x.userId = :userId and x.eventId in :eventIds and x.deletedAt is null order by x.id
            """)
    List<EventException> findLiveByEvents(@Param("userId") UUID userId, @Param("eventIds") Collection<UUID> eventIds);

    @Query("""
            select x from EventException x
            where x.eventId = :eventId and x.originalStartLocal = :original and x.deletedAt is null
            """)
    Optional<EventException> findLiveAt(@Param("eventId") UUID eventId,
            @Param("original") java.time.LocalDateTime original);
}
