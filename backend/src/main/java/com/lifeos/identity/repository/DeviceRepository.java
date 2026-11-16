package com.lifeos.identity.repository;

import com.lifeos.identity.entity.Device;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Dữ liệu cá nhân: mọi truy vấn theo id đều kèm userId (SECURITY_RULES §7.2). */
public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByIdAndUserId(UUID id, UUID userId);

    List<Device> findByUserIdOrderByCreatedAtDescIdAsc(UUID userId);

    /** Lần hoạt động cuối khi refresh (không tải entity). */
    @Modifying(flushAutomatically = true)
    @Query("update Device d set d.lastSeenAt = :now where d.id = :id and d.userId = :userId")
    int touch(@Param("id") UUID id, @Param("userId") UUID userId, @Param("now") Instant now);

    /** Thiết bị bị thu hồi gần đây: nạp lại tập thu hồi trong bộ nhớ khi khởi động (D45). */
    @Query("select d from Device d where d.revokedAt is not null and d.revokedAt > :since")
    List<Device> findRevokedSince(@Param("since") Instant since);
}
