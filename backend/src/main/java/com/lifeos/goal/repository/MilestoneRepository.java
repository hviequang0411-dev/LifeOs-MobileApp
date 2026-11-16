package com.lifeos.goal.repository;

import com.lifeos.goal.entity.Milestone;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Milestone. Mọi truy vấn kèm userId. */
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    @Query("select m from Milestone m where m.id = :id and m.userId = :userId and m.deletedAt is null")
    Optional<Milestone> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select m from Milestone m where m.id = :id and m.userId = :userId and m.deletedAt is null")
    Optional<Milestone> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select m.version from Milestone m where m.id = :id and m.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select m from Milestone m where m.userId = :userId and m.goalId = :goalId and m.deletedAt is null
            order by m.id
            """)
    List<Milestone> findLiveByGoal(@Param("userId") UUID userId, @Param("goalId") UUID goalId);

    @Query("""
            select m from Milestone m where m.userId = :userId and m.goalId in :goalIds and m.deletedAt is null
            order by m.position
            """)
    List<Milestone> findLiveByGoalsOrdered(@Param("userId") UUID userId, @Param("goalIds") Collection<UUID> goalIds);

    @Query("select coalesce(max(m.position), 0) from Milestone m where m.goalId = :goalId and m.deletedAt is null")
    long maxPosition(@Param("goalId") UUID goalId);

    @Query("""
            select count(m) > 0 from Milestone m
            where m.goalId = :goalId and m.position = :position and m.deletedAt is null and m.id <> :excludeId
            """)
    boolean positionTaken(@Param("goalId") UUID goalId, @Param("position") long position,
            @Param("excludeId") UUID excludeId);

    @Query("select count(m) from Milestone m where m.goalId = :goalId and m.deletedAt is null")
    long countLive(@Param("goalId") UUID goalId);

    @Query("select m from Milestone m where m.userId = :userId and m.id in :ids")
    List<Milestone> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
