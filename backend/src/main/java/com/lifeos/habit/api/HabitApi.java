package com.lifeos.habit.api;

import java.util.UUID;

/** API công khai của module habit (chỉ đọc). */
public interface HabitApi {

    /** Habit còn sống của user (khóa FOR SHARE khi gọi trong transaction ghi — reminder không mồ côi). */
    boolean lockForShare(UUID userId, UUID habitId);
}
