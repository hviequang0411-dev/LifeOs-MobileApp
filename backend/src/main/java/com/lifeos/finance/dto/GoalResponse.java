package com.lifeos.finance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Mục tiêu tiết kiệm (D111). status ACHIEVED chỉ do server đặt. */
public record GoalResponse(UUID id, String name, String currency, String targetAmount, LocalDate targetOn,
        String status, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "GoalResponse[id=" + id + ", version=" + version + "]";
    }
}
