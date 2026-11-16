package com.lifeos.analytics.repository;

import com.lifeos.analytics.entity.Review;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Review của user. Mọi truy vấn kèm userId. */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("select r from Review r where r.id = :id and r.userId = :userId and r.deletedAt is null")
    Optional<Review> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select r.version from Review r where r.id = :id and r.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select r from Review r where r.userId = :userId and r.periodType = :type and r.periodKey = :key
              and r.deletedAt is null
            """)
    Optional<Review> findLivePeriod(@Param("userId") UUID userId, @Param("type") String type,
            @Param("key") LocalDate key);

    @Query("select r from Review r where r.userId = :userId and r.id in :ids")
    List<Review> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);

    @Query("""
            select r from Review r where r.userId = :userId and r.deletedAt is null
              and (:type is null or r.periodType = :type)
              and (:first = true or r.periodKey < :afterKey or (r.periodKey = :afterKey and r.id < :afterId))
            order by r.periodKey desc, r.id desc
            """)
    List<Review> page(@Param("userId") UUID userId, @Param("type") String type, @Param("first") boolean first,
            @Param("afterKey") LocalDate afterKey, @Param("afterId") UUID afterId, Limit limit);
}
