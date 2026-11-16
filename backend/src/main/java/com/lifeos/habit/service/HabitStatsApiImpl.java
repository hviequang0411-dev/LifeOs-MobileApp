package com.lifeos.habit.service;

import com.lifeos.habit.api.HabitStatsApi;
import com.lifeos.habit.dto.HabitStatsResponse;
import com.lifeos.habit.entity.Habit;
import com.lifeos.habit.repository.HabitRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link HabitStatsApi} bằng chính HabitQueryService.statsAll (không định nghĩa lại chỉ số). */
@Service
class HabitStatsApiImpl implements HabitStatsApi {

    private final HabitQueryService queries;
    private final HabitRepository habits;

    HabitStatsApiImpl(HabitQueryService queries, HabitRepository habits) {
        this.queries = queries;
        this.habits = habits;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitPeriod> stats(UUID userId, LocalDate from, LocalDate to) {
        Map<UUID, String> names = habits.findAllActiveUnarchived(userId).stream()
                .collect(Collectors.toMap(Habit::getId, Habit::getName));
        List<HabitStatsResponse> stats = queries.statsAll(userId, from.toString(), to.toString());
        return stats.stream().map(s -> new HabitPeriod(s.habitId(), names.get(s.habitId()), s.scheduled(), s.done(),
                s.skipped(), s.missed(), s.completionRate(), s.currentStreak(), s.longestStreak())).toList();
    }
}
