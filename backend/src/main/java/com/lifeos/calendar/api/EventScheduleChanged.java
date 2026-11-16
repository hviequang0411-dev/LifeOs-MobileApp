package com.lifeos.calendar.api;

import java.util.UUID;

/** Sự kiện miền: giờ/quy tắc lặp/ngoại lệ của Event đổi; notification tính lại reminder (cùng transaction). */
public record EventScheduleChanged(UUID userId, UUID eventId) {
}
