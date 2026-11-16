package com.lifeos.finance.service;

import com.lifeos.common.outbox.OutboxWriter;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.domain.DefaultCategories;
import com.lifeos.finance.entity.Budget;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.repository.BudgetRepository;
import com.lifeos.finance.repository.FinanceJdbc;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Ngưỡng ngân sách 80% / 100% (UC-FIN-06, FR-FIN-13, D116, FIN-13). Gọi trong transaction ghi khoản chi, SAU khi khoản
 * chi đã flush và TRƯỚC ChangeRecorder (D86):
 * <ol>
 *   <li>Chỉ khoản chi của THÁNG HIỆN TẠI theo múi giờ người dùng [PROPOSED]; khoản chi ADJUSTMENT không tính (D119).</li>
 *   <li>Khóa FOR UPDATE các ngân sách đang hoạt động bị ảnh hưởng theo id tăng dần — các lệnh ghi đồng thời kiểm ngưỡng
 *       tuần tự, nên lệnh sau luôn thấy khoản chi đã commit của lệnh trước.</li>
 *   <li>Mức dùng ≥ ngưỡng → chèn budget_alerts (budget, tháng, ngưỡng) ON CONFLICT DO NOTHING; chỉ lần chèn đầu ghi
 *       outbox_events BUDGET_THRESHOLD. Retry/ghi lặp không tạo sự kiện thứ hai.</li>
 * </ol>
 * Payload theo D149 (schema tối thiểu): userId, category, templateKey, variables — KHÔNG có số tiền, tên danh mục hay
 * ghi chú (hiện chi tiết trên màn hình khóa tắt, BACKEND_MASTER_PLAN §4).
 */
@Component
class BudgetAlerts {

    static final String EVENT_TYPE = "BUDGET_THRESHOLD";
    static final int[] THRESHOLDS = {80, 100};

    private final BudgetRepository budgets;
    private final FinanceJdbc jdbc;
    private final OutboxWriter outbox;
    private final UserCalendar calendar;
    private final Clock clock;

    BudgetAlerts(BudgetRepository budgets, FinanceJdbc jdbc, OutboxWriter outbox, UserCalendar calendar, Clock clock) {
        this.budgets = budgets;
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.calendar = calendar;
        this.clock = clock;
    }

    void afterExpense(UUID userId, String kind, String currency, FinancialCategory categoryOrNull,
            LocalDate occurredOn) {
        if (!FinanceValues.EXPENSE.equals(kind)) {
            return;
        }
        if (categoryOrNull != null && DefaultCategories.ADJUSTMENT.equals(categoryOrNull.getSystemKey())) {
            return;
        }
        YearMonth month = YearMonth.from(calendar.today(userId));
        if (!YearMonth.from(occurredOn).equals(month)) {
            return;
        }
        UUID categoryId = categoryOrNull == null ? null : categoryOrNull.getId();
        List<Budget> affected = budgets.lockAffected(userId, currency, categoryId);
        LocalDate from = month.atDay(1);
        for (Budget budget : affected) {
            BigDecimal spent = jdbc.spent(userId, budget.getCurrency(), budget.getCategoryId(), from,
                    month.plusMonths(1).atDay(1));
            for (int threshold : THRESHOLDS) {
                BigDecimal line = budget.getAmount().multiply(BigDecimal.valueOf(threshold))
                        .divide(BigDecimal.valueOf(100));
                if (spent.compareTo(line) >= 0
                        && jdbc.insertAlert(userId, budget.getId(), from, threshold, clock.instant())) {
                    Map<String, Object> variables = new LinkedHashMap<>();
                    variables.put("budgetId", budget.getId().toString());
                    variables.put("month", month.toString());
                    variables.put("threshold", threshold);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("userId", userId.toString());
                    payload.put("category", "FINANCE");
                    payload.put("templateKey", "finance.budget.threshold." + threshold);
                    payload.put("variables", variables);
                    outbox.write(EVENT_TYPE, Budget.ENTITY_TYPE, budget.getId(), payload);
                }
            }
        }
    }
}
