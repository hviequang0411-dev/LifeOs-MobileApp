package com.lifeos.routine.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Tỷ lệ routine trên [from, to] (D245); completionRate null khi scheduled = 0. */
public record RoutineStatsResponse(UUID routineId, LocalDate from, LocalDate to, int scheduled, int completed,
        int partial, BigDecimal completionRate) {
}
