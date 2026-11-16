package com.lifeos.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Mức dùng ngân sách một tháng (D109): spent, limit, remaining là chuỗi; ratio = spent / limit (4 chữ số). */
public record BudgetUsageResponse(UUID budgetId, UUID categoryId, String currency, String month, String spent,
        String limit, BigDecimal ratio, String remaining) {
}
