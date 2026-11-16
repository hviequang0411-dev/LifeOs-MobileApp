package com.lifeos.finance.mapper;

import com.lifeos.finance.domain.Money;
import com.lifeos.finance.dto.AccountResponse;
import com.lifeos.finance.dto.BudgetResponse;
import com.lifeos.finance.dto.FinanceCategoryResponse;
import com.lifeos.finance.dto.ContributionResponse;
import com.lifeos.finance.dto.GoalResponse;
import com.lifeos.finance.dto.RecurringResponse;
import com.lifeos.finance.dto.TransactionResponse;
import com.lifeos.finance.dto.TransferResponse;
import com.lifeos.finance.entity.AccountTransfer;
import com.lifeos.finance.entity.Budget;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.entity.FinancialTransaction;
import com.lifeos.finance.entity.RecurringTransaction;
import com.lifeos.finance.entity.SavingContribution;
import com.lifeos.finance.entity.SavingGoal;
import java.math.BigDecimal;
import org.mapstruct.Mapper;

/**
 * Mapper entity → DTO của module finance (D5: MapStruct, unmappedTargetPolicy = ERROR). Mọi BigDecimal tiền thành chuỗi
 * thập phân qua {@link Money#format} (D100) — không bao giờ qua số thực.
 */
@Mapper
public interface FinanceMapper {

    AccountResponse toResponse(FinancialAccount account);

    FinanceCategoryResponse toResponse(FinancialCategory category);

    TransactionResponse toResponse(FinancialTransaction transaction);

    TransferResponse toResponse(AccountTransfer transfer);

    BudgetResponse toResponse(Budget budget);

    RecurringResponse toResponse(RecurringTransaction recurring);

    GoalResponse toResponse(SavingGoal goal);

    ContributionResponse toResponse(SavingContribution contribution);

    default String money(BigDecimal value) {
        return Money.format(value);
    }
}
