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

/** Giao dịch định kỳ (FR-FIN-07, D110, PHASE_3 §7). nextRunOn do server quản; job sinh giao dịch idempotent theo (id, recurrence_date). */
@Entity
@Table(name = "recurring_transactions")
public class RecurringTransaction extends SyncedEntity {

    public static final String ENTITY_TYPE = "recurring_transaction";

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

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "frequency", nullable = false, length = 7)
    private String frequency;

    @Column(name = "interval_count", nullable = false)
    private Short intervalCount;

    @Column(name = "start_on", nullable = false)
    private LocalDate startOn;

    @Column(name = "end_on")
    private LocalDate endOn;

    @Column(name = "next_run_on", nullable = false)
    private LocalDate nextRunOn;

    @Column(name = "status", nullable = false, length = 6)
    private String status;

    protected RecurringTransaction() {
    }

    public RecurringTransaction(UUID id, UUID userId) {
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

    public String getFrequency() {
        return frequency;
    }

    /** @return true nếu giá trị đổi */
    public boolean setFrequency(String value) {
        if (Objects.equals(frequency, value)) {
            return false;
        }
        frequency = value;
        return true;
    }

    public Short getIntervalCount() {
        return intervalCount;
    }

    /** @return true nếu giá trị đổi */
    public boolean setIntervalCount(Short value) {
        if (Objects.equals(intervalCount, value)) {
            return false;
        }
        intervalCount = value;
        return true;
    }

    public LocalDate getStartOn() {
        return startOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStartOn(LocalDate value) {
        if (Objects.equals(startOn, value)) {
            return false;
        }
        startOn = value;
        return true;
    }

    public LocalDate getEndOn() {
        return endOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setEndOn(LocalDate value) {
        if (Objects.equals(endOn, value)) {
            return false;
        }
        endOn = value;
        return true;
    }

    public LocalDate getNextRunOn() {
        return nextRunOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setNextRunOn(LocalDate value) {
        if (Objects.equals(nextRunOn, value)) {
            return false;
        }
        nextRunOn = value;
        return true;
    }

    public String getStatus() {
        return status;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStatus(String value) {
        if (Objects.equals(status, value)) {
            return false;
        }
        status = value;
        return true;
    }

    /** So sánh theo giá trị (125000 và 125000.0000 là một), không theo scale. */
    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
