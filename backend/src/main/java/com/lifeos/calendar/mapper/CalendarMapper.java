package com.lifeos.calendar.mapper;

import com.lifeos.calendar.dto.EventResponse;
import com.lifeos.calendar.dto.ExceptionResponse;
import com.lifeos.calendar.dto.TimeBlockResponse;
import com.lifeos.calendar.entity.CalendarEvent;
import com.lifeos.calendar.entity.EventException;
import com.lifeos.calendar.entity.TimeBlock;
import org.mapstruct.Mapper;

/** Mapper entity → DTO của module calendar (D5). recurrence (jsonb) → Map qua {@link JsonColumns}. */
@Mapper(uses = JsonColumns.class)
public interface CalendarMapper {

    EventResponse toResponse(CalendarEvent event);

    ExceptionResponse toResponse(EventException exception);

    TimeBlockResponse toResponse(TimeBlock block);
}
