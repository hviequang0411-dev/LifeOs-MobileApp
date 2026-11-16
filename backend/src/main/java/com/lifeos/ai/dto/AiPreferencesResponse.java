package com.lifeos.ai.dto;

import java.time.Instant;

public record AiPreferencesResponse(boolean taskCalendar, boolean habit, boolean goal, boolean notes,
        boolean finance, String policyVersion, String providerNoticeVersion, Instant providerTermsAcceptedAt,
        Instant updatedAt, AiFeatureFlags features) {
}
