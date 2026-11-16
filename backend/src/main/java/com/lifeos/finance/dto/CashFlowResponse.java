package com.lifeos.finance.dto;

import java.time.LocalDate;
import java.util.List;

/** GET /reports/cash-flow (FR-FIN-10): chuỗi theo kỳ, đủ mọi kỳ kể cả không có giao dịch. */
public record CashFlowResponse(String currency, String granularity, LocalDate from, LocalDate to, String income,
        String expense, String net, List<Bucket> buckets) {

    public record Bucket(LocalDate periodStart, String income, String expense, String net) {
    }
}
