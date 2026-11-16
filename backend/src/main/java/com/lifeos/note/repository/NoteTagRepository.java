package com.lifeos.note.repository;

import com.lifeos.note.entity.NoteTag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Liên kết ghi chú ↔ thẻ (D184). Mọi truy vấn kèm userId. */
public interface NoteTagRepository extends JpaRepository<NoteTag, UUID> {

    @Query("select x from NoteTag x where x.id = :id and x.userId = :userId and x.deletedAt is null")
    Optional<NoteTag> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select x.version from NoteTag x where x.id = :id and x.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select x from NoteTag x where x.userId = :userId and x.noteId = :noteId and x.tagId = :tagId
              and x.deletedAt is null
            """)
    Optional<NoteTag> findLivePair(@Param("userId") UUID userId, @Param("noteId") UUID noteId,
            @Param("tagId") UUID tagId);

    @Query("select x from NoteTag x where x.userId = :userId and x.noteId = :noteId and x.deletedAt is null order by x.id")
    List<NoteTag> findLiveByNote(@Param("userId") UUID userId, @Param("noteId") UUID noteId);

    @Query("select x from NoteTag x where x.userId = :userId and x.noteId in :noteIds and x.deletedAt is null")
    List<NoteTag> findLiveByNotes(@Param("userId") UUID userId, @Param("noteIds") Collection<UUID> noteIds);

    @Query("select x from NoteTag x where x.userId = :userId and x.tagId = :tagId and x.deletedAt is null order by x.id")
    List<NoteTag> findLiveByTag(@Param("userId") UUID userId, @Param("tagId") UUID tagId);

    @Query("select count(x) from NoteTag x where x.noteId = :noteId and x.deletedAt is null")
    long countLiveByNote(@Param("noteId") UUID noteId);

    @Query("select x from NoteTag x where x.userId = :userId and x.id in :ids")
    List<NoteTag> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
