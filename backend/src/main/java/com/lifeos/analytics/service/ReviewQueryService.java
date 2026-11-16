package com.lifeos.analytics.service;

import com.lifeos.analytics.domain.Period;
import com.lifeos.analytics.dto.ReviewResponse;
import com.lifeos.analytics.dto.ReviewSyncState;
import com.lifeos.analytics.entity.Review;
import com.lifeos.analytics.repository.ReviewRepository;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.web.Cursors;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc review (D202). */
@Service
public class ReviewQueryService {

    static final String SORT = "periodKey:desc";
    private static final Set<String> TYPES = Set.of(Period.DAY, Period.WEEK, Period.MONTH);

    private final ReviewRepository reviews;

    public ReviewQueryService(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(UUID userId, UUID id) {
        return reviews.findActive(id, userId).map(ReviewCommandService::toResponse)
                .orElseThrow(ReviewCommandService::notFound);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> list(UUID userId, String periodType, Integer limit, String cursor) {
        if (periodType != null && !TYPES.contains(periodType)) {
            throw AnalyticsValues.invalid("periodType", "INVALID_VALUE", "periodType phải là DAY, WEEK hoặc MONTH.");
        }
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        LocalDate afterKey = after == null ? null : AnalyticsValues.date(after.key(), "cursor");
        List<Review> rows = reviews.page(userId, periodType, after == null, afterKey,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<Review> page = hasMore ? rows.subList(0, size) : rows;
        String next = hasMore ? Cursors.encode(new Cursors.Position(SORT, page.get(page.size() - 1).getPeriodKey()
                .toString(), page.get(page.size() - 1).getId())) : null;
        return new PageResponse<>(page.stream().map(ReviewCommandService::toResponse).toList(), next, hasMore);
    }

    @Transactional(readOnly = true)
    public ReviewResponse forPeriod(UUID userId, Period period) {
        return reviews.findLivePeriod(userId, period.type(), period.start()).map(ReviewCommandService::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReviewSyncState> syncStates(UUID userId, Collection<UUID> ids) {
        return ids.isEmpty() ? List.of() : reviews.findAllOwned(userId, ids).stream()
                .map(r -> new ReviewSyncState(r.getId(), r.getVersion(), r.isDeleted(),
                        ReviewCommandService.toResponse(r))).toList();
    }
}
