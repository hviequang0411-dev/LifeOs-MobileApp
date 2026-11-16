package com.lifeos.finance.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * API thống kê chỉ đọc của module finance (PHASE_7 D197, D199): đúng quy tắc Phase 3 — loại trừ chuyển khoản, tombstone
 * và ADJUSTMENT, luôn tách theo tiền tệ, số tiền là chuỗi NUMERIC chính xác.
 */
public interface FinanceStatsApi {

    record Totals(String currency, String income, String expense, String net) {
    }

    record Bucket(LocalDate periodStart, String income, String expense, String net) {
    }

    /** categoryId null = chưa phân loại. */
    record CategoryShare(UUID categoryId, String total) {
    }

    record Flow(String currency, String income, String expense, String net, List<Bucket> buckets,
            List<CategoryShare> topExpenseCategories) {
    }

    record BudgetUse(UUID budgetId, UUID categoryId, String currency, String spent, String limit, BigDecimal ratio) {
    }

    /** Tổng thu/chi theo từng tiền tệ có giao dịch trong [from, to]. */
    List<Totals> totals(UUID userId, LocalDate from, LocalDate to);

    /** Dòng tiền một tiền tệ theo granularity (day|week|month), kèm top 10 danh mục chi. */
    Flow flow(UUID userId, String currency, LocalDate from, LocalDate to, String granularity);

    /** Mức dùng mọi ngân sách chưa lưu trữ trong tháng. */
    List<BudgetUse> budgets(UUID userId, YearMonth month);
}
