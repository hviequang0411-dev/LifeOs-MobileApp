package com.lifeos.routine.repository;

import com.lifeos.routine.entity.RoutineStepLog;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Kết quả bước trong lượt chạy. Mọi truy vấn kèm userId. */
public interface RoutineStepLogRepository extends JpaRepository<RoutineStepLog, UUID> {

    @Query("select s from RoutineStepLog s where s.id = :id and s.userId = :userId and s.deletedAt is null")
    Optional<RoutineStepLog> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select s.version from RoutineStepLog s where s.id = :id and s.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select s from RoutineStepLog s
            where s.routineLogId = :logId and s.stepId = :stepId and s.userId = :userId and s.deletedAt is null
            """)
    Optional<RoutineStepLog> findActiveFor(@Param("logId") UUID logId, @Param("stepId") UUID stepId,
            @Param("userId") UUID userId);

    /** Kết quả còn sống của các lượt, theo thứ tự id (cascade D86, đếm hoàn tất). */
    @Query("""
            select s from RoutineStepLog s
            where s.userId = :userId and s.routineLogId in :logIds and s.deletedAt is null
            order by s.id
            """)
    List<RoutineStepLog> findLiveByLogs(@Param("userId") UUID userId, @Param("logIds") Collection<UUID> logIds);

    @Query("select count(s) from RoutineStepLog s where s.routineLogId = :logId and s.deletedAt is null")
    long countLive(@Param("logId") UUID logId);

    @Query("select s from RoutineStepLog s where s.userId = :userId and s.id in :ids")
    List<RoutineStepLog> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
