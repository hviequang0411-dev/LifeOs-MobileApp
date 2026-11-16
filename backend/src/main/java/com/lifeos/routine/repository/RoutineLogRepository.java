package com.lifeos.routine.repository;

import com.lifeos.routine.entity.RoutineLog;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lượt chạy routine. Mọi truy vấn kèm userId. */
public interface RoutineLogRepository extends JpaRepository<RoutineLog, UUID> {

    @Query("select l from RoutineLog l where l.id = :id and l.userId = :userId and l.deletedAt is null")
    Optional<RoutineLog> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** FOR UPDATE: ghi kết quả bước khóa lượt để tính completedAt tuần tự (D242). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from RoutineLog l where l.id = :id and l.userId = :userId and l.deletedAt is null")
    Optional<RoutineLog> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select l.version from RoutineLog l where l.id = :id and l.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select l from RoutineLog l
            where l.routineId = :routineId and l.localDate = :date and l.userId = :userId and l.deletedAt is null
            """)
    Optional<RoutineLog> findActiveOn(@Param("routineId") UUID routineId, @Param("date") LocalDate date,
            @Param("userId") UUID userId);

    @Query("""
            select l from RoutineLog l where l.routineId = :routineId and l.userId = :userId and l.deletedAt is null
            order by l.id
            """)
    List<RoutineLog> findLiveByRoutine(@Param("userId") UUID userId, @Param("routineId") UUID routineId);

    @Query("""
            select l from RoutineLog l
            where l.userId = :userId and l.routineId = :routineId and l.deletedAt is null
              and l.localDate >= :from and l.localDate <= :to
            order by l.localDate desc
            """)
    List<RoutineLog> range(@Param("userId") UUID userId, @Param("routineId") UUID routineId,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select l from RoutineLog l where l.userId = :userId and l.id in :ids")
    List<RoutineLog> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
