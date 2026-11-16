package com.lifeos.calendar.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/** POST /calendar/events. Có giờ: startLocal/endLocal; cả ngày (allDay): startDate/endDate. */
public record CreateEventRequest(@NotNull UUID id, @NotNull String title, String description, String location,
        Boolean allDay, @NotNull String timezone, String startLocal, String endLocal, String startDate,
        String endDate, Map<String, Object> recurrence) {

    @Override
    public String toString() {
        return "CreateEventRequest[id=" + id + "]";
    }
}
