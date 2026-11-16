package com.lifeos.analytics.dto;

import java.time.Instant;
import java.util.List;

/** GET /dashboard (D196): mỗi thẻ độc lập, status ∈ {OK, ERROR, UNAVAILABLE}; data null khi không OK. */
public record DashboardResponse(Instant computedAt, List<Card> cards) {

    public record Card(String id, String status, Object data) {
    }
}
