package com.lifeos.analytics.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Phản tư của người dùng cho một kỳ (D202). Không lưu số liệu (D190). periodType, periodKey bất biến. */
@Entity
@Table(name = "reviews")
public class Review extends SyncedEntity {

    public static final String ENTITY_TYPE = "review";

    @Column(name = "period_type", nullable = false, length = 5, updatable = false)
    private String periodType;

    @Column(name = "period_key", nullable = false, updatable = false)
    private LocalDate periodKey;

    @Column(name = "reflection", nullable = false)
    private String reflection;

    @Column(name = "mood")
    private Short mood;

    protected Review() {
    }

    public static Review create(UUID id, UUID userId, String periodType, LocalDate periodKey, String reflection,
            Short mood) {
        Review r = new Review(id, userId);
        r.periodType = periodType;
        r.periodKey = periodKey;
        r.reflection = reflection;
        r.mood = mood;
        return r;
    }

    private Review(UUID id, UUID userId) {
        super(id, userId);
    }

    public boolean apply(String newReflection, Short newMood) {
        if (Objects.equals(reflection, newReflection) && Objects.equals(mood, newMood)) {
            return false;
        }
        reflection = newReflection;
        mood = newMood;
        return true;
    }

    public String getPeriodType() {
        return periodType;
    }

    public LocalDate getPeriodKey() {
        return periodKey;
    }

    public String getReflection() {
        return reflection;
    }

    public Short getMood() {
        return mood;
    }
}
