package com.lifeos.note.repository;

import com.lifeos.note.entity.NoteLink;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Liên kết ghi chú → Task/Goal (D189). Mọi truy vấn kèm userId. */
public interface NoteLinkRepository extends JpaRepository<NoteLink, UUID> {

    @Query("select x from NoteLink x where x.id = :id and x.userId = :userId and x.deletedAt is null")
    Optional<NoteLink> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select x.version from NoteLink x where x.id = :id and x.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select x from NoteLink x where x.userId = :userId and x.noteId = :noteId and x.targetType = :targetType
              and x.targetId = :targetId and x.deletedAt is null
            """)
    Optional<NoteLink> findLiveTriple(@Param("userId") UUID userId, @Param("noteId") UUID noteId,
            @Param("targetType") String targetType, @Param("targetId") UUID targetId);

    @Query("select x from NoteLink x where x.userId = :userId and x.noteId = :noteId and x.deletedAt is null order by x.id")
    List<NoteLink> findLiveByNote(@Param("userId") UUID userId, @Param("noteId") UUID noteId);

    @Query("""
            select x from NoteLink x where x.userId = :userId and x.targetType = :targetType and x.targetId = :targetId
              and x.deletedAt is null order by x.id
            """)
    List<NoteLink> findLiveByTarget(@Param("userId") UUID userId, @Param("targetType") String targetType,
            @Param("targetId") UUID targetId);

    @Query("select count(x) from NoteLink x where x.noteId = :noteId and x.deletedAt is null")
    long countLiveByNote(@Param("noteId") UUID noteId);

    @Query("select x from NoteLink x where x.userId = :userId and x.id in :ids")
    List<NoteLink> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
