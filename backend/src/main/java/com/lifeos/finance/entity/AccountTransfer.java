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

/** Chuyển khoản giữa hai tài khoản cùng tiền tệ (FR-FIN-05, D104): không là thu/chi. Hai tài khoản bất biến sau khi tạo. */
@Entity
@Table(name = "account_transfers")
public class AccountTransfer extends SyncedEntity {

    public static final String ENTITY_TYPE = "account_transfer";

    @Column(name = "from_account_id", nullable = false, updatable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false, updatable = false)
    private UUID toAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "note", length = 500)
    private String note;

    protected AccountTransfer() {
    }

    public AccountTransfer(UUID id, UUID userId) {
        super(id, userId);
    }

    public static AccountTransfer create(UUID id, UUID userId, UUID fromAccountId, UUID toAccountId) {
        AccountTransfer transfer = new AccountTransfer(id, userId);
        transfer.fromAccountId = fromAccountId;
        transfer.toAccountId = toAccountId;
        return transfer;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setFromAccountId(UUID value) {
        if (Objects.equals(fromAccountId, value)) {
            return false;
        }
        fromAccountId = value;
        return true;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setToAccountId(UUID value) {
        if (Objects.equals(toAccountId, value)) {
            return false;
        }
        toAccountId = value;
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

    /** So sánh theo giá trị (125000 và 125000.0000 là một), không theo scale. */
    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
