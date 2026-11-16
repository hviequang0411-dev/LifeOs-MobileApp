package com.lifeos.note.repository;

import com.lifeos.note.entity.Tag;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Thẻ. Mọi truy vấn kèm userId. */
public interface TagRepository extends JpaRepository<Tag, UUID> {

    @Query("select t from Tag t where t.id = :id and t.userId = :userId and t.deletedAt is null")
    Optional<Tag> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tag t where t.id = :id and t.userId = :userId and t.deletedAt is null")
    Optional<Tag> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select t from Tag t where t.id = :id and t.userId = :userId and t.deletedAt is null")
    Optional<Tag> findActiveForShare(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select t.version from Tag t where t.id = :id and t.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select t from Tag t where t.userId = :userId and t.deletedAt is null and lower(t.name) = lower(:name)
              and t.id <> :excludeId
            """)
    Optional<Tag> findLiveByName(@Param("userId") UUID userId, @Param("name") String name,
            @Param("excludeId") UUID excludeId);

    @Query("select t from Tag t where t.userId = :userId and t.deletedAt is null order by lower(t.name), t.id")
    List<Tag> findLive(@Param("userId") UUID userId);

    @Query("select t from Tag t where t.userId = :userId and t.deletedAt is null and t.id in :ids")
    List<Tag> findLiveIn(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("select t from Tag t where t.userId = :userId and t.id in :ids")
    List<Tag> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
