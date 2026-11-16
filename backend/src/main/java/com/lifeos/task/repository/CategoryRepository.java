package com.lifeos.task.repository;

import com.lifeos.task.entity.Category;
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
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("select c from Category c where c.id = :id and c.userId = :userId and c.deletedAt is null")
    Optional<Category> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR UPDATE: xóa Category khóa dòng trước khi cascade (D80). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Category c where c.id = :id and c.userId = :userId and c.deletedAt is null")
    Optional<Category> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    /** SELECT … FOR SHARE: kiểm tham chiếu từ Task (D80; luôn khóa Project trước Category). */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select c from Category c where c.id = :id and c.userId = :userId and c.deletedAt is null")
    Optional<Category> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Mọi bản ghi (kể cả đã xóa mềm) của user trong danh sách id — snapshot cho sync (SYNC_PROTOCOL §6). */
    @Query("select c from Category c where c.userId = :userId and c.id in :ids")
    List<Category> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    /** version hiện tại kể cả khi đã xóa mềm (báo currentVersion khi xung đột). */
    @Query("select c.version from Category c where c.id = :id and c.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Tên đã được dùng bởi Category CÒN SỐNG khác của user? So bằng lower() của DB (D78), khớp ux_categories_user_name. */
    @Query("""
            select count(c) > 0 from Category c
            where c.userId = :userId and c.deletedAt is null and lower(c.name) = lower(:name) and c.id <> :excludeId
            """)
    boolean nameTaken(@Param("userId") UUID userId, @Param("name") String name, @Param("excludeId") UUID excludeId);

    /** Danh sách theo keyset, createdAt giảm dần rồi id giảm dần (D79). */
    @Query("""
            select c from Category c
            where c.userId = :userId and c.deletedAt is null
              and (:first = true
                   or c.createdAt < :afterCreatedAt
                   or (c.createdAt = :afterCreatedAt and c.id < :afterId))
            order by c.createdAt desc, c.id desc
            """)
    List<Category> page(@Param("userId") UUID userId, @Param("first") boolean first,
            @Param("afterCreatedAt") Instant afterCreatedAt, @Param("afterId") UUID afterId, Limit limit);
}
