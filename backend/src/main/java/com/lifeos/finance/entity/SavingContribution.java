package com.lifeos.finance.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Khoản góp tiết kiệm (D111): độc lập với số dư tài khoản; goalId bất biến. */
@Entity
@Table(name = "saving_contributions")
public class SavingContribution extends SyncedEntity {

    public static final String ENTITY_TYPE = "saving_contribution";

    @Column(name = "goal_id", nullable = false, updatable = false)
    private UUID goalId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "occurred_on", nullable = false)
    private LocalDate occurredOn;

    @Column(name = "note", length = 500)
    private String note;

    protected SavingContribution() {
    }

    public SavingContribution(UUID id, UUID userId) {
        super(id, userId);
    }

    public static SavingContribution create(UUID id, UUID userId, UUID goalId) {
        SavingContribution contribution = new SavingContribution(id, userId);
        contribution.goalId = goalId;
        return contribution;
    }

    public UUID getGoalId() {
        return goalId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setGoalId(UUID value) {
        if (Objects.equals(goalId, value)) {
            return false;
        }
        goalId = value;
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
