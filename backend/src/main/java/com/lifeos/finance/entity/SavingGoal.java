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

/** Mục tiêu tiết kiệm (FR-FIN-08, D111). ACHIEVED chỉ do server đặt. */
@Entity
@Table(name = "saving_goals")
public class SavingGoal extends SyncedEntity {

    public static final String ENTITY_TYPE = "saving_goal";

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @Column(name = "target_on")
    private LocalDate targetOn;

    @Column(name = "status", nullable = false, length = 8)
    private String status;

    protected SavingGoal() {
    }

    public SavingGoal(UUID id, UUID userId) {
        super(id, userId);
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

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTargetAmount(BigDecimal value) {
        if (sameAmount(targetAmount, value)) {
            return false;
        }
        targetAmount = value;
        return true;
    }

    public LocalDate getTargetOn() {
        return targetOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTargetOn(LocalDate value) {
        if (Objects.equals(targetOn, value)) {
            return false;
        }
        targetOn = value;
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
