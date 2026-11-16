package com.lifeos.calendar.repository;

import com.lifeos.calendar.entity.CalendarEvent;
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
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    @Query("select e from CalendarEvent e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<CalendarEvent> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CalendarEvent e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<CalendarEvent> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from CalendarEvent e where e.id = :id and e.userId = :userId and e.deletedAt is null")
    Optional<CalendarEvent> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e.version from CalendarEvent e where e.id = :id and e.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select e from CalendarEvent e where e.userId = :userId and e.id in :ids")
    List<CalendarEvent> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("select count(e) from CalendarEvent e where e.userId = :userId and e.deletedAt is null")
    long countLive(@Param("userId") UUID userId);

    /** Danh sách keyset createdAt giảm dần rồi id giảm dần (D79). */
    @Query("""
            select e from CalendarEvent e
            where e.userId = :userId and e.deletedAt is null
              and (:first = true or e.createdAt < :afterCreatedAt
                   or (e.createdAt = :afterCreatedAt and e.id < :afterId))
            order by e.createdAt desc, e.id desc
            """)
    List<CalendarEvent> page(@Param("userId") UUID userId, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);

    /** Sự kiện không lặp giao với [from, to) (chỉ mục ix_calendar_events_user_start). */
    @Query("""
            select e from CalendarEvent e
            where e.userId = :userId and e.deletedAt is null and e.recurrence is null
              and e.startAt < :to and e.endAt > :from
            order by e.startAt, e.id
            """)
    List<CalendarEvent> singleBetween(@Param("userId") UUID userId, @Param("from") Instant from,
            @Param("to") Instant to);

    /** Sự kiện lặp có thể có lần xuất hiện trong [from, to). */
    @Query("""
            select e from CalendarEvent e
            where e.userId = :userId and e.deletedAt is null and e.recurrence is not null
              and e.startAt < :to and (e.recurrenceUntil is null or e.recurrenceUntil > :from)
            order by e.startAt, e.id
            """)
    List<CalendarEvent> recurringBefore(@Param("userId") UUID userId, @Param("from") Instant from,
            @Param("to") Instant to);
}
