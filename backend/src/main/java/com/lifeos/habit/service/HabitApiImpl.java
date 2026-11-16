package com.lifeos.habit.service;

import com.lifeos.habit.api.HabitApi;
import com.lifeos.habit.repository.HabitRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link HabitApi}. */
@Service
class HabitApiImpl implements HabitApi {

    private final HabitRepository habits;

    HabitApiImpl(HabitRepository habits) {
        this.habits = habits;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockForShare(UUID userId, UUID habitId) {
        return habits.findActiveForShare(habitId, userId).isPresent();
    }
}
