package com.lifeos.finance.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.repository.FinancialAccountRepository;
import com.lifeos.finance.repository.FinancialCategoryRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Nạp và khóa FOR SHARE bản ghi được tham chiếu bởi lệnh ghi Finance (D115). Tham chiếu không tồn tại, đã xóa hoặc của
 * user khác đều là 422 cùng một mã (REST) / REJECTED (sync). Thứ tự khóa cố định trong module: tài khoản (id tăng dần)
 * → danh mục → mục tiêu → ngân sách (id tăng dần) → ChangeRecorder (D86).
 */
@Component
class FinanceRefs {

    private final FinancialAccountRepository accounts;
    private final FinancialCategoryRepository categories;

    FinanceRefs(FinancialAccountRepository accounts, FinancialCategoryRepository categories) {
        this.accounts = accounts;
        this.categories = categories;
    }

    FinancialAccount account(UUID userId, UUID accountId) {
        return accounts.findActiveForShare(accountId, userId)
                .orElseThrow(() -> FinanceValues.badReference(ErrorCode.FINANCE_ACCOUNT_NOT_FOUND));
    }

    /** Tài khoản nhận giao dịch/chuyển khoản MỚI: không được lưu trữ (D107). */
    FinancialAccount writableAccount(UUID userId, UUID accountId) {
        FinancialAccount account = account(userId, accountId);
        if (account.isArchived()) {
            throw FinanceValues.business(ErrorCode.FINANCE_ACCOUNT_ARCHIVED);
        }
        return account;
    }

    /** Danh mục (null = chưa phân loại) phải đúng loại thu/chi. Danh mục lưu trữ vẫn dùng được [PROPOSED]. */
    FinancialCategory category(UUID userId, UUID categoryIdOrNull, String kind) {
        if (categoryIdOrNull == null) {
            return null;
        }
        FinancialCategory category = categories.findActiveForShare(categoryIdOrNull, userId)
                .orElseThrow(() -> FinanceValues.badReference(ErrorCode.FINANCE_CATEGORY_NOT_FOUND));
        if (!category.getKind().equals(kind)) {
            throw FinanceValues.business(ErrorCode.FINANCE_CATEGORY_KIND_MISMATCH);
        }
        return category;
    }
}
