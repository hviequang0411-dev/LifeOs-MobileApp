package com.lifeos.project.repository;

import com.lifeos.project.entity.Project;
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

/** Dữ liệu cá nhân: mọi truy vấn theo id đều kèm userId (SECURITY_RULES §7.2); truy vấn thường bỏ bản ghi đã xóa. */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("select p from Project p where p.id = :id and p.userId = :userId and p.deletedAt is null")
    Optional<Project> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: xóa Project khóa dòng trước khi cascade (D80). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id and p.userId = :userId and p.deletedAt is null")
    Optional<Project> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR SHARE: kiểm tham chiếu từ Task, chặn xóa đồng thời cho đến khi transaction kết thúc (D80). */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from Project p where p.id = :id and p.userId = :userId and p.deletedAt is null")
    Optional<Project> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Mọi bản ghi (kể cả đã xóa mềm) của user trong danh sách id — snapshot cho sync (SYNC_PROTOCOL §6). */
    @Query("select p from Project p where p.userId = :userId and p.id in :ids")
    List<Project> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** version hiện tại kể cả khi đã xóa mềm (báo currentVersion khi xung đột). */
    @Query("select p.version from Project p where p.id = :id and p.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    /**
     * Danh sách theo keyset, sắp createdAt giảm dần rồi id giảm dần (D79). archived: "false" | "true" | "all".
     * after* null = trang đầu.
     */
    @Query("""
            select p from Project p
            where p.userId = :userId and p.deletedAt is null
              and (:archived = 'all'
                   or (:archived = 'true' and p.archivedAt is not null)
                   or (:archived = 'false' and p.archivedAt is null))
              and (:first = true
                   or p.createdAt < :afterCreatedAt
                   or (p.createdAt = :afterCreatedAt and p.id < :afterId))
            order by p.createdAt desc, p.id desc
            """)
    List<Project> page(@Param("userId") UUID userId, @Param("archived") String archived,
            @Param("first") boolean first, @Param("afterCreatedAt") Instant afterCreatedAt,
            @Param("afterId") UUID afterId, Limit limit);
}
