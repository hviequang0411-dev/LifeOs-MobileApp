package com.lifeos.finance.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** GET /reports/monthly (FR-FIN-10, UC-FIN-08): một tiền tệ; loại trừ chuyển khoản, tombstone và ADJUSTMENT (D119). */
public record MonthlyReportResponse(String month, String currency, String income, String expense, String net,
        List<CategoryTotal> byCategory, List<AccountTotal> byAccount, List<DayTotal> daily) {

    /** categoryId null = chưa phân loại. */
    public record CategoryTotal(UUID categoryId, String kind, String total) {
    }

    public record AccountTotal(UUID accountId, String income, String expense) {
    }

    public record DayTotal(LocalDate date, String income, String expense) {
    }
}
