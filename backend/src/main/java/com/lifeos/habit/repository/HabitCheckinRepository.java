package com.lifeos.habit.repository;

import com.lifeos.habit.entity.HabitCheckin;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Check-in của habit. Mọi truy vấn kèm userId. */
public interface HabitCheckinRepository extends JpaRepository<HabitCheckin, UUID> {

    @Query("select c from HabitCheckin c where c.id = :id and c.userId = :userId and c.deletedAt is null")
    Optional<HabitCheckin> findActive(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("""
            select c from HabitCheckin c
            where c.habitId = :habitId and c.localDate = :date and c.userId = :userId and c.deletedAt is null
            """)
    Optional<HabitCheckin> findActiveOn(@Param("habitId") UUID habitId, @Param("date") LocalDate date,
            @Param("userId") UUID userId);

    @Query("select c.version from HabitCheckin c where c.id = :id and c.userId = :userId")
    Optional<Long> findVersion(@Param("id") UUID id, @Param("userId") UUID userId);

    /** Cascade xóa Habit: tải HẾT check-in còn sống theo thứ tự id (D86: thứ tự khóa ổn định). */
    @Query("""
            select c from HabitCheckin c where c.habitId = :habitId and c.userId = :userId and c.deletedAt is null
            order by c.id
            """)
    List<HabitCheckin> findLiveByHabit(@Param("userId") UUID userId, @Param("habitId") UUID habitId);

    /** Check-in còn sống của nhiều habit (chuỗi/thống kê). */
    @Query("""
            select c from HabitCheckin c where c.userId = :userId and c.habitId in :habitIds and c.deletedAt is null
            """)
    List<HabitCheckin> findLiveByHabits(@Param("userId") UUID userId, @Param("habitIds") Collection<UUID> habitIds);

    /** Lịch sử theo khoảng ngày, mới nhất trước; phân trang bằng ngày (khóa duy nhất trong habit). */
    @Query("""
            select c from HabitCheckin c
            where c.userId = :userId and c.habitId = :habitId and c.deletedAt is null
              and c.localDate >= :from and c.localDate <= :to
              and (:first = true or c.localDate < :before)
            order by c.localDate desc
            """)
    List<HabitCheckin> history(@Param("userId") UUID userId, @Param("habitId") UUID habitId,
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("first") boolean first,
            @Param("before") LocalDate before, Limit limit);

    @Query("select c from HabitCheckin c where c.userId = :userId and c.id in :ids")
    List<HabitCheckin> findAllOwned(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
