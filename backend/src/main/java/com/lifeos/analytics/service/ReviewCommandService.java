package com.lifeos.analytics.service;

import com.lifeos.analytics.domain.Period;
import com.lifeos.analytics.dto.ReviewResponse;
import com.lifeos.analytics.entity.Review;
import com.lifeos.analytics.repository.ReviewRepository;
import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NaturalKeyConflictException;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi review (D202): một bản/kỳ; tạo trùng kỳ → 409 REVIEW_EXISTS (sync: CONFLICT ALREADY_EXISTS kèm bản đang có);
 * periodKey phải là ngày bắt đầu kỳ theo D192; reflection ≤ 5000 code point (NFC); mood 1–5 hoặc null.
 */
@Service
public class ReviewCommandService {

    static final int REFLECTION_MAX = 5000;
    private static final Set<String> TYPES = Set.of(Period.DAY, Period.WEEK, Period.MONTH);
    private static final String PK = "pk_reviews";
    private static final String PERIOD_UNIQUE = "ux_reviews_period";

    private final ReviewRepository reviews;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final UserCalendar calendar;
    private final Clock clock;

    public ReviewCommandService(ReviewRepository reviews, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, UserCalendar calendar, Clock clock) {
        this.reviews = reviews;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.calendar = calendar;
        this.clock = clock;
    }

    public ReviewResponse create(UUID userId, ReviewCommands.Create cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (reviews.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String type = cmd.periodType().value();
                if (type == null || !TYPES.contains(type)) {
                    throw AnalyticsValues.invalid("periodType", type == null ? "REQUIRED" : "INVALID_VALUE",
                            "periodType phải là DAY, WEEK hoặc MONTH.");
                }
                LocalDate key = AnalyticsValues.required(cmd.periodKey().value(), "periodKey");
                if (!Period.isPeriodStart(type, key, calendar.weekStart(userId))) {
                    throw AnalyticsValues.invalid("periodKey", "INVALID_VALUE", "periodKey phải là ngày bắt đầu kỳ.");
                }
                String reflection = reflection(cmd.reflection().value());
                Short mood = mood(cmd.mood().value());
                Optional<Review> existing = reviews.findLivePeriod(userId, type, key);
                if (existing.isPresent()) {
                    throw new NaturalKeyConflictException(ErrorCode.REVIEW_EXISTS, existing.get().getId());
                }
                Review saved = reviews.saveAndFlush(Review.create(cmd.id(), userId, type, key, reflection, mood));
                changeRecorder.record(userId, Review.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                String m = t.getMessage();
                if (m != null && m.contains(PK)) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                if (m != null && m.contains(PERIOD_UNIQUE)) {
                    throw new ConflictException(ErrorCode.REVIEW_EXISTS);
                }
            }
            throw e;
        }
    }

    public ReviewResponse update(UUID userId, UUID reviewId, ReviewCommands.Update cmd, UUID mutationId) {
        return versionedWrites.write(() -> {
            Review review = reviews.findActive(reviewId, userId).orElseThrow(ReviewCommandService::notFound);
            com.lifeos.common.persistence.VersionedWrites.requireVersion(review, cmd.expectedVersion());
            if (cmd.periodType().present() && !review.getPeriodType().equals(cmd.periodType().value())) {
                throw AnalyticsValues.invalid("periodType", "INVALID_VALUE", "periodType không đổi được.");
            }
            if (cmd.periodKey().present()
                    && !review.getPeriodKey().equals(AnalyticsValues.date(cmd.periodKey().value(), "periodKey"))) {
                throw AnalyticsValues.invalid("periodKey", "INVALID_VALUE", "periodKey không đổi được.");
            }
            String reflection = cmd.reflection().present() ? reflection(cmd.reflection().value())
                    : review.getReflection();
            Short mood = cmd.mood().present() ? mood(cmd.mood().value()) : review.getMood();
            if (!review.apply(reflection, mood)) {
                return toResponse(review);
            }
            Review saved = reviews.saveAndFlush(review);
            changeRecorder.record(userId, Review.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationId);
            return toResponse(saved);
        }, () -> reviews.findVersion(reviewId, userId).orElseThrow(ReviewCommandService::notFound));
    }

    public long delete(UUID userId, UUID reviewId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            Review review = reviews.findActive(reviewId, userId).orElseThrow(ReviewCommandService::notFound);
            com.lifeos.common.persistence.VersionedWrites.requireVersion(review, expectedVersion);
            review.softDelete(clock.instant());
            Review saved = reviews.saveAndFlush(review);
            changeRecorder.record(userId, Review.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> reviews.findVersion(reviewId, userId).orElseThrow(ReviewCommandService::notFound));
    }

    static String reflection(String raw) {
        if (raw == null) {
            return "";
        }
        String value = Normalizer.normalize(raw.replace("\r\n", "\n"), Normalizer.Form.NFC);
        if (value.codePointCount(0, value.length()) > REFLECTION_MAX) {
            throw AnalyticsValues.invalid("reflection", "MAX_LENGTH", "Tối đa " + REFLECTION_MAX + " ký tự.");
        }
        return value;
    }

    static Short mood(Integer raw) {
        if (raw == null) {
            return null;
        }
        if (raw < 1 || raw > 5) {
            throw AnalyticsValues.invalid("mood", "SIZE", "mood từ 1 đến 5.");
        }
        return raw.shortValue();
    }

    static ReviewResponse toResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getPeriodType(), r.getPeriodKey(), r.getReflection(),
                r.getMood() == null ? null : r.getMood().intValue(), r.getVersion(), r.getCreatedAt(),
                r.getUpdatedAt());
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.REVIEW_NOT_FOUND);
    }
}
