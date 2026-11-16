package com.lifeos.finance.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Tài khoản tài chính (FR-FIN-01/02, D102 số dư không lưu, D107 lưu trữ). Giá trị đã được service kiểm. */
@Entity
@Table(name = "financial_accounts")
public class FinancialAccount extends SyncedEntity {

    public static final String ENTITY_TYPE = "financial_account";

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "account_type", nullable = false, length = 12)
    private String accountType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected FinancialAccount() {
    }

    public FinancialAccount(UUID id, UUID userId) {
        super(id, userId);
    }

    /** @return true nếu lần này mới lưu trữ */
    public boolean archive(Instant now) {
        if (archivedAt != null) {
            return false;
        }
        archivedAt = now;
        return true;
    }

    /** @return true nếu lần này mới bỏ lưu trữ */
    public boolean unarchive() {
        if (archivedAt == null) {
            return false;
        }
        archivedAt = null;
        return true;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public String getName() {
        return name;
    }

    /** @return true nếu giá trị đổi */
    public boolean setName(String value) {
        if (Objects.equals(name, value)) {
            return false;
        }
        name = value;
        return true;
    }

    public String getAccountType() {
        return accountType;
    }

    /** @return true nếu giá trị đổi */
    public boolean setAccountType(String value) {
        if (Objects.equals(accountType, value)) {
            return false;
        }
        accountType = value;
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

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    /** @return true nếu giá trị đổi */
    public boolean setOpeningBalance(BigDecimal value) {
        if (sameAmount(openingBalance, value)) {
            return false;
        }
        openingBalance = value;
        return true;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    /** So sánh theo giá trị (125000 và 125000.0000 là một), không theo scale. */
    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
