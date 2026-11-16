package com.lifeos.ai.dto;

import java.time.LocalDate;
import java.util.List;

public record AiUsageResponse(LocalDate from, LocalDate to, List<AiUsageDay> days, AiUsageTotals totals) {
}
