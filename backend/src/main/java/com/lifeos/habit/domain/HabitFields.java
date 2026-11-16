package com.lifeos.habit.domain;

import java.time.LocalTime;

/** Toàn bộ trường chỉnh sửa được của một habit, đã chuẩn hóa và kiểm tra (D230, D252). toString không in tên/mô tả. */
public record HabitFields(String name, String description, String color, String icon, HabitSchedule schedule,
        LocalTime reminderTime) {

    @Override
    public String toString() {
        return "HabitFields[scheduleType=" + schedule.type() + "]";
    }
}
