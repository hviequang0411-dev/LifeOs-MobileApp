package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

class AiFoundationIT extends ApiTestBase {

    @Test
    void AI_01_newUserHasAllConsentOffWithoutCreatingRow() throws Exception {
        TestUser user = newUser();
        var result = get(user, "/api/v1/ai/preferences");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> response = readJson(result);
        assertThat(response).containsEntry("taskCalendar", false).containsEntry("habit", false)
                .containsEntry("goal", false).containsEntry("notes", false).containsEntry("finance", false);
        assertThat(jdbc.sql("SELECT count(*) FROM ai_user_preferences WHERE user_id = :u")
                .param("u", user.id()).query(Integer.class).single()).isZero();
    }

    @Test
    void AI_02_replaceConsentPersistsWhitelistAndPolicyTimestamps() throws Exception {
        TestUser user = newUser();
        var result = call(user, HttpMethod.PUT, "/api/v1/ai/preferences", body(
                "taskCalendar", true, "habit", true, "goal", false, "notes", false, "finance", false,
                "providerTermsAccepted", true));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> response = readJson(result);
        assertThat(response).containsEntry("taskCalendar", true).containsEntry("habit", true)
                .containsEntry("policyVersion", "2026-11-10");
        assertThat(response.get("providerTermsAcceptedAt")).isNotNull();
        assertThat(get(user, "/api/v1/ai/preferences").getResponse().getContentAsString())
                .contains("\"taskCalendar\":true", "\"habit\":true");
    }

    @Test
    void AI_03_consentCannotBeEnabledWithoutProviderTerms() throws Exception {
        TestUser user = newUser();
        var result = call(user, HttpMethod.PUT, "/api/v1/ai/preferences", body(
                "taskCalendar", true, "habit", false, "goal", false, "notes", false, "finance", false,
                "providerTermsAccepted", false));
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(readJson(result)).containsEntry("errorCode", "AI_CONSENT_REQUIRED");
    }

    @Test
    void AI_05_preferencesAreOwnedAndMassAssignmentIsRejected() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        var massAssignment = call(a, HttpMethod.PUT, "/api/v1/ai/preferences", body(
                "taskCalendar", false, "habit", false, "goal", false, "notes", false, "finance", false,
                "providerTermsAccepted", false, "userId", b.id()));
        assertThat(massAssignment.getResponse().getStatus()).isEqualTo(400);
        assertThat(get(b, "/api/v1/ai/preferences").getResponse().getContentAsString())
                .contains("\"taskCalendar\":false");
    }

    @Test
    void AI_16_usageAndLimitsAreScopedAndResetOnUtcBoundaries() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        insertUsage(user.id(), 31, 7, 1234, Instant.parse("2026-11-12T01:00:00Z"));
        insertUsage(other.id(), 999, 999, 999999, Instant.parse("2026-11-12T01:00:00Z"));
        var usage = get(user, "/api/v1/ai/usage?from=2026-11-12&to=2026-11-12");
        assertThat(usage.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> totals = (Map<String, Object>) readJson(usage).get("totals");
        assertThat(totals).containsEntry("requests", 1).containsEntry("inputTokens", 31)
                .containsEntry("outputTokens", 7).containsEntry("estimatedCostMicrousd", 1234);
        var limits = get(user, "/api/v1/ai/limits");
        assertThat(limits.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(limits)).containsEntry("rateLimitPerMinute", 5).containsEntry("maxConcurrentPerUser", 2);
    }

    @Test
    void AI_17_costUsesIntegerMicrousdWithoutDouble() throws Exception {
        TestUser user = newUser();
        insertUsage(user.id(), 10, 2, 42, clock.instant());
        Long stored = jdbc.sql("SELECT estimated_cost_microusd FROM ai_usage_logs WHERE user_id = :u")
                .param("u", user.id()).query(Long.class).single();
        assertThat(stored).isEqualTo(42L);
    }

    @Test
    void AI_29_deleteAiDataRemovesChildrenAndResetsConsentButKeepsUser() throws Exception {
        TestUser user = newUser();
        var consent = call(user, HttpMethod.PUT, "/api/v1/ai/preferences", body(
                "taskCalendar", true, "habit", false, "goal", false, "notes", false, "finance", false,
                "providerTermsAccepted", true));
        assertThat(consent.getResponse().getStatus()).isEqualTo(200);
        UUID conversation = UUID.randomUUID();
        UUID message = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO ai_conversations(id,user_id,title,status,last_message_at,expires_at,created_at,updated_at)
                VALUES (:id,:u,'test','ACTIVE',:now,:expiry,:now,:now)
                """).param("id", conversation).param("u", user.id())
                .param("now", clock.instant().atOffset(ZoneOffset.UTC))
                .param("expiry", clock.instant().plusSeconds(3600).atOffset(ZoneOffset.UTC)).update();
        jdbc.sql("""
                INSERT INTO ai_messages(id,user_id,conversation_id,role,kind,content,created_at)
                VALUES (:id,:u,:c,'USER','CHAT','hello',:now)
                """).param("id", message).param("u", user.id()).param("c", conversation)
                .param("now", clock.instant().atOffset(ZoneOffset.UTC)).update();
        insertUsage(user.id(), 1, 1, 1, clock.instant());
        var deleted = delete(user, "/api/v1/ai/data");
        assertThat(deleted.getResponse().getStatus()).isEqualTo(204);
        for (String table : new String[] {"ai_action_proposals", "ai_usage_logs", "ai_messages",
                "ai_conversations", "ai_user_preferences"}) {
            assertThat(jdbc.sql("SELECT count(*) FROM " + table + " WHERE user_id = :u").param("u", user.id())
                    .query(Integer.class).single()).as(table).isZero();
        }
        assertThat(jdbc.sql("SELECT count(*) FROM users WHERE id = :u").param("u", user.id())
                .query(Integer.class).single()).isOne();
    }

    private void insertUsage(UUID userId, long input, long output, long cost, Instant at) {
        jdbc.sql("""
                INSERT INTO ai_usage_logs(id,user_id,provider,model,input_tokens,output_tokens,
                    estimated_cost_microusd,latency_ms,result_code,created_at)
                VALUES (:id,:u,'fake','fake-model',:i,:o,:cost,3,'SUCCESS',:at)
                """).param("id", UUID.randomUUID()).param("u", userId).param("i", input).param("o", output)
                .param("cost", cost).param("at", at.atOffset(ZoneOffset.UTC)).update();
    }
}
