package com.lifeos.finance.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/**
 * Lệnh của use case Finance (JAVA_SPRING_RULES §2.2): REST và sync gọi cùng service. Trường {@link Patch}: vắng = không
 * đổi (update) / mặc định (create); payload sync sai kiểu nổ ở bước kiểm giá trị, sau tồn tại/version (D96). Tiền là
 * chuỗi (D100). toString không in dữ liệu người dùng (số tiền, ghi chú, tên).
 */
public final class FinanceCommands {

    private FinanceCommands() {
    }

    public record CreateAccount(UUID id, Patch<String> name, Patch<String> accountType, Patch<String> currency,
            Patch<String> openingBalance) {
        @Override
        public String toString() {
            return "CreateAccount[id=" + id + "]";
        }
    }

    public record UpdateAccount(long expectedVersion, Patch<String> name, Patch<String> accountType,
            Patch<String> currency, Patch<String> openingBalance, Patch<Boolean> archived) {
        @Override
        public String toString() {
            return "UpdateAccount[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateCategory(UUID id, Patch<String> name, Patch<String> kind) {
        @Override
        public String toString() {
            return "CreateCategory[id=" + id + "]";
        }
    }

    public record UpdateCategory(long expectedVersion, Patch<String> name, Patch<String> kind,
            Patch<Boolean> archived) {
        @Override
        public String toString() {
            return "UpdateCategory[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateTransaction(UUID id, Patch<UUID> accountId, Patch<UUID> categoryId, Patch<String> kind,
            Patch<String> amount, Patch<String> occurredOn, Patch<String> note) {
        @Override
        public String toString() {
            return "CreateTransaction[id=" + id + "]";
        }
    }

    public record UpdateTransaction(long expectedVersion, Patch<UUID> accountId, Patch<UUID> categoryId,
            Patch<String> kind, Patch<String> amount, Patch<String> occurredOn, Patch<String> note) {
        @Override
        public String toString() {
            return "UpdateTransaction[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateTransfer(UUID id, Patch<UUID> fromAccountId, Patch<UUID> toAccountId, Patch<String> amount,
            Patch<String> occurredOn, Patch<String> note) {
        @Override
        public String toString() {
            return "CreateTransfer[id=" + id + "]";
        }
    }

    public record UpdateTransfer(long expectedVersion, Patch<UUID> fromAccountId, Patch<UUID> toAccountId,
            Patch<String> amount, Patch<String> occurredOn, Patch<String> note) {
        @Override
        public String toString() {
            return "UpdateTransfer[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateBudget(UUID id, Patch<UUID> categoryId, Patch<String> currency, Patch<String> amount) {
        @Override
        public String toString() {
            return "CreateBudget[id=" + id + "]";
        }
    }

    public record UpdateBudget(long expectedVersion, Patch<UUID> categoryId, Patch<String> currency,
            Patch<String> amount, Patch<Boolean> archived) {
        @Override
        public String toString() {
            return "UpdateBudget[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateRecurring(UUID id, Patch<UUID> accountId, Patch<UUID> categoryId, Patch<String> kind,
            Patch<String> amount, Patch<String> note, Patch<String> frequency, Patch<Integer> intervalCount,
            Patch<String> startOn, Patch<String> endOn, Patch<String> status) {
        @Override
        public String toString() {
            return "CreateRecurring[id=" + id + "]";
        }
    }

    public record UpdateRecurring(long expectedVersion, Patch<UUID> accountId, Patch<UUID> categoryId,
            Patch<String> kind, Patch<String> amount, Patch<String> note, Patch<String> frequency,
            Patch<Integer> intervalCount, Patch<String> startOn, Patch<String> endOn, Patch<String> status) {
        @Override
        public String toString() {
            return "UpdateRecurring[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateGoal(UUID id, Patch<String> name, Patch<String> currency, Patch<String> targetAmount,
            Patch<String> targetOn, Patch<String> status) {
        @Override
        public String toString() {
            return "CreateGoal[id=" + id + "]";
        }
    }

    public record UpdateGoal(long expectedVersion, Patch<String> name, Patch<String> currency,
            Patch<String> targetAmount, Patch<String> targetOn, Patch<String> status) {
        @Override
        public String toString() {
            return "UpdateGoal[expectedVersion=" + expectedVersion + "]";
        }
    }

    public record CreateContribution(UUID id, Patch<UUID> goalId, Patch<String> amount, Patch<String> occurredOn,
            Patch<String> note) {
        @Override
        public String toString() {
            return "CreateContribution[id=" + id + "]";
        }
    }

    public record UpdateContribution(long expectedVersion, Patch<UUID> goalId, Patch<String> amount,
            Patch<String> occurredOn, Patch<String> note) {
        @Override
        public String toString() {
            return "UpdateContribution[expectedVersion=" + expectedVersion + "]";
        }
    }
}
