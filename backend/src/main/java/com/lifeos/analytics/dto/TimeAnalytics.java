package com.lifeos.analytics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** GET /analytics/time (FR-INT-07): phút theo bucket. */
public record TimeAnalytics(LocalDate from, LocalDate to, String granularity, Instant computedAt, long eventMinutes,
        long blockMinutes, int events, List<Bucket> buckets) {

    public record Bucket(LocalDate periodStart, long eventMinutes, long blockMinutes, int events) {
    }
}
