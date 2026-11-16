package com.lifeos.note.repository;

import com.lifeos.note.entity.Note;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Ghi chú. Mọi truy vấn theo id kèm userId (SECURITY_RULES §7.2). */
public interface NoteRepository extends JpaRepository<Note, UUID> {

    @Query("select n from Note n where n.id = :id and n.userId = :userId and n.deletedAt is null")
    Optional<Note> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    /** FOR UPDATE: mọi ghi note_tag/note_link và xóa Note khóa Note trước (tuần tự theo ghi chú, D86). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Note n where n.id = :id and n.userId = :userId and n.deletedAt is null")
    Optional<Note> findActiveForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select n.version from Note n where n.id = :id and n.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select count(n) from Note n where n.userId = :userId and n.deletedAt is null and n.pinned = true")
    long countPinned(@Param("userId") UUID userId);

    @Query("select n from Note n where n.userId = :userId and n.id in :ids")
    List<Note> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
