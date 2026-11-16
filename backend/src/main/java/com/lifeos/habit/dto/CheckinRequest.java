package com.lifeos.habit.dto;

import java.util.UUID;

/**
 * PUT /habits/{id}/checkins/{date} {id?, status, note?}: upsert idempotent theo (habit, ngày). id tùy chọn: dùng khi tạo
 * mới (client tạo offline). Giá trị kiểm ở service (dùng chung với sync). toString không in ghi chú.
 */
public record CheckinRequest(UUID id, String status, String note) {

    @Override
    public String toString() {
        return "CheckinRequest[id=" + id + "]";
    }
}
