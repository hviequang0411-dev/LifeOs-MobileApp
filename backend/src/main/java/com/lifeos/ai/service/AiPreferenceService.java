package com.lifeos.ai.service;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.dto.AiFeatureFlags;
import com.lifeos.ai.dto.AiPreferencesRequest;
import com.lifeos.ai.dto.AiPreferencesResponse;
import com.lifeos.ai.repository.AiStore;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiPreferenceService {
    private final AiStore store;
    private final AiProperties properties;
    private final Clock clock;

    public AiPreferenceService(AiStore store, AiProperties properties, Clock clock) {
        this.store = store;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiPreferencesResponse get(UUID userId) {
        return store.preference(userId).map(this::response).orElseGet(this::defaults);
    }

    @Transactional
    public AiPreferencesResponse replace(UUID userId, AiPreferencesRequest request) {
        boolean anyConsent = request.taskCalendar() || request.habit() || request.goal() || request.notes()
                || request.finance();
        if (anyConsent && !request.providerTermsAccepted()) {
            throw AiErrors.error("AI_CONSENT_REQUIRED", HttpStatus.FORBIDDEN,
                    "Phải chấp nhận điều khoản nhà cung cấp trước khi bật miền dữ liệu.");
        }
        Instant now = clock.instant();
        Instant accepted = request.providerTermsAccepted() ? now : null;
        store.replacePreference(userId, request.taskCalendar(), request.habit(), request.goal(), request.notes(),
                request.finance(), properties.getPolicyVersion(), accepted, now);
        return get(userId);
    }

    private AiPreferencesResponse defaults() {
        return new AiPreferencesResponse(false, false, false, false, false, properties.getPolicyVersion(),
                properties.getProviderNoticeVersion(), null, null, features());
    }

    private AiPreferencesResponse response(AiStore.Preference value) {
        return new AiPreferencesResponse(value.taskCalendar(), value.habit(), value.goal(), value.notes(),
                value.finance(), value.policyVersion(), properties.getProviderNoticeVersion(), value.acceptedAt(),
                value.updatedAt(), features());
    }

    private AiFeatureFlags features() {
        var f = properties.getFeatures();
        return new AiFeatureFlags(f.isChat(), f.isInsights(), f.isNotes(), f.isFinance(), f.isRag(), f.isActions());
    }
}
