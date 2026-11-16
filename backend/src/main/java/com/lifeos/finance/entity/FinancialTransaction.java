package com.lifeos.finance.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Giao dịch thu/chi (FR-FIN-04, D103 currency theo tài khoản do server đặt, D108 ngày nghiệp vụ). recurringTransactionId/recurrenceDate chỉ do job đặt (D110). */
@Entity
@Table(name = "financial_transactions")
public class FinancialTransaction extends SyncedEntity {

    public static final String ENTITY_TYPE = "financial_transaction";

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "kind", nullable = false, length = 7)
    private String kind;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "recurring_transaction_id", updatable = false)
    private UUID recurringTransactionId;

    @Column(name = "recurrence_date", updatable = false)
    private LocalDate recurrenceDate;

    protected FinancialTransaction() {
    }

    public FinancialTransaction(UUID id, UUID userId) {
        super(id, userId);
    }

    public UUID getAccountId() {
        return accountId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setAccountId(UUID value) {
        if (Objects.equals(accountId, value)) {
            return false;
        }
        accountId = value;
        return true;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setCategoryId(UUID value) {
        if (Objects.equals(categoryId, value)) {
            return false;
        }
        categoryId = value;
        return true;
    }

    public String getKind() {
        return kind;
    }

    /** @return true nếu giá trị đổi */
    public boolean setKind(String value) {
        if (Objects.equals(kind, value)) {
            return false;
        }
        kind = value;
        return true;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    /** @return true nếu giá trị đổi */
    public boolean setAmount(BigDecimal value) {
        if (sameAmount(amount, value)) {
            return false;
        }
        amount = value;
        return true;
    }

    public String getCurrency() {
        return currency;
    }

    /** @return true nếu giá trị đổi */
    public boolean setCurrency(String value) {
        if (Objects.equals(currency, value)) {
            return false;
        }
        currency = value;
        return true;
    }

    public LocalDate getOccurredOn() {
        return occurredOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setOccurredOn(LocalDate value) {
        if (Objects.equals(occurredOn, value)) {
            return false;
        }
        occurredOn = value;
        return true;
    }

    public String getNote() {
        return note;
    }

    /** @return true nếu giá trị đổi */
    public boolean setNote(String value) {
        if (Objects.equals(note, value)) {
            return false;
        }
        note = value;
        return true;
    }

    public UUID getRecurringTransactionId() {
        return recurringTransactionId;
    }

    public LocalDate getRecurrenceDate() {
        return recurrenceDate;
    }

    /** So sánh theo giá trị (125000 và 125000.0000 là một), không theo scale. */
    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
