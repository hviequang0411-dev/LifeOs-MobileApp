package com.lifeos.routine.repository;

import com.lifeos.routine.entity.RoutineStep;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Bước routine. Mọi truy vấn kèm userId. */
public interface RoutineStepRepository extends JpaRepository<RoutineStep, UUID> {

    @Query("select s from RoutineStep s where s.id = :id and s.userId = :userId and s.deletedAt is null")
    Optional<RoutineStep> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Bước của user kể cả đã xóa mềm (D243: kết quả bước tham chiếu cả bước đã xóa). */
    @Query("select s from RoutineStep s where s.id = :id and s.userId = :userId")
    Optional<RoutineStep> findOwned(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select s.version from RoutineStep s where s.id = :id and s.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Bước còn sống của một routine theo thứ tự id (cascade, D86) hoặc vị trí (hiển thị). */
    @Query("""
            select s from RoutineStep s where s.routineId = :routineId and s.userId = :userId and s.deletedAt is null
            order by s.id
            """)
    List<RoutineStep> findLiveByRoutine(@Param("userId") UUID userId, @Param("routineId") UUID routineId);

    @Query("""
            select s from RoutineStep s
            where s.userId = :userId and s.routineId in :routineIds and s.deletedAt is null
            order by s.position
            """)
    List<RoutineStep> findLiveByRoutinesOrdered(@Param("userId") UUID userId,
            @Param("routineIds") Collection<UUID> routineIds);

    @Query("""
            select coalesce(max(s.position), 0) from RoutineStep s
            where s.routineId = :routineId and s.deletedAt is null
            """)
    long maxPosition(@Param("routineId") UUID routineId);

    @Query("""
            select count(s) > 0 from RoutineStep s
            where s.routineId = :routineId and s.position = :position and s.deletedAt is null and s.id <> :excludeId
            """)
    boolean positionTaken(@Param("routineId") UUID routineId, @Param("position") long position,
            @Param("excludeId") UUID excludeId);

    @Query("select count(s) from RoutineStep s where s.routineId = :routineId and s.deletedAt is null")
    long countLive(@Param("routineId") UUID routineId);

    @Query("select s from RoutineStep s where s.userId = :userId and s.id in :ids")
    List<RoutineStep> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
