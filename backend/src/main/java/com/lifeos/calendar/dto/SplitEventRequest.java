package com.lifeos.calendar.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /calendar/events/{id}/split ("từ lần này"): cắt chuỗi gốc trước originalStartLocal và tạo chuỗi mới newEventId bắt đầu từ lần đó (trường tùy chọn ghi đè). */
public record SplitEventRequest(@NotNull Long version, @NotNull String originalStartLocal,
        @NotNull UUID newEventId, String title, String description, String location, String startLocal,
        String endLocal) {

    @Override
    public String toString() {
        return "SplitEventRequest[newEventId=" + newEventId + "]";
    }
}
