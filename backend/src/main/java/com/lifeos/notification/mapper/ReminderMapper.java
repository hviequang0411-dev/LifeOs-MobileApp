package com.lifeos.notification.mapper;

import com.lifeos.notification.dto.ReminderResponse;
import com.lifeos.notification.entity.Reminder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Mapper Reminder → DTO (D5). repeatRule (jsonb) → Map; localTime "HH:mm". */
@Mapper(uses = NotificationJson.class)
public interface ReminderMapper {

    @Mapping(target = "localTime", dateFormat = "HH:mm")
    ReminderResponse toResponse(Reminder reminder);
}
