package com.lifeos.identity.repository;

import com.lifeos.identity.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** email truyền vào đã chuẩn hóa; lower(...) để dùng chỉ mục ux_users_email_lower (Q8). */
    @Query("select u from User u where lower(u.email) = :email")
    Optional<User> findByNormalizedEmail(@Param("email") String normalizedEmail);

    /** id người dùng theo thứ tự tăng dần sau {@code after} (job bù dữ liệu mặc định, keyset). */
    @Query("select u.id from User u where u.id > :after order by u.id")
    List<UUID> idsAfter(@Param("after") UUID after, Limit limit);
}
