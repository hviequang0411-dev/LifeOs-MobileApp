package com.lifeos.identity.repository;

import com.lifeos.identity.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /** Tra theo hash: token thô chính là credential nên không có điều kiện user_id ở bước này. */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Vô hiệu mọi token còn hiệu lực của user trước khi cấp token mới (SECURITY_RULES §3.4). */
    @Modifying(flushAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now where t.userId = :userId and t.usedAt is null")
    int invalidateActive(@Param("userId") UUID userId, @Param("now") Instant now);

    /** Dùng token nguyên tử: trả 1 chỉ khi token chưa dùng và còn hạn. */
    @Modifying(flushAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now"
            + " where t.id = :id and t.usedAt is null and t.expiresAt > :now")
    int consume(@Param("id") UUID id, @Param("now") Instant now);

    /** Job dọn (SYSTEM_ARCHITECTURE §3.12): xóa token hết hạn trước {@code cutoff}. */
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
