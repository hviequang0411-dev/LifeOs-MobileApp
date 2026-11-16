package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.ai.provider.FakeLLMProvider;
import com.lifeos.support.ApiTestBase;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

@TestPropertySource(properties = {"lifeos.ai.enabled=true", "lifeos.ai.features.chat=true"})
class AiConversationIT extends ApiTestBase {
    @Autowired
    FakeLLMProvider provider;

    @BeforeEach
    void resetProvider() {
        provider.reset();
    }

    @Test
    void AI_04_28_conversationLifecycleUsesOwnedCursorPagesAndCascadeDelete() throws Exception {
        TestUser user = newUser();
        UUID first = conversation(user, "Một");
        UUID second = conversation(user, "Hai");

        MvcResult listed = get(user, "/api/v1/ai/conversations?limit=1");
        assertThat(listed.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> page = readJson(listed);
        assertThat((List<?>) page.get("items")).hasSize(1);
        assertThat(page).containsEntry("hasMore", true);
        String cursor = (String) page.get("nextCursor");
        assertThat(items(get(user, "/api/v1/ai/conversations?limit=1&cursor=" + cursor))).hasSize(1);

        assertThat(get(user, "/api/v1/ai/conversations/" + first).getResponse().getStatus()).isEqualTo(200);
        assertThat(delete(user, "/api/v1/ai/conversations/" + first).getResponse().getStatus()).isEqualTo(204);
        assertThat(get(user, "/api/v1/ai/conversations/" + first).getResponse().getStatus()).isEqualTo(404);
        assertThat(jdbc.sql("SELECT count(*) FROM ai_conversations WHERE id IN (:a,:b)")
                .param("a", first).param("b", second).query(Integer.class).single()).isOne();
    }

    @Test
    void AI_07_08_chatSendsExactMinimalContextAndCallsProviderOutsideTransaction() throws Exception {
        TestUser user = newUser();
        consent(user, true);
        UUID task = UUID.randomUUID();
        LocalDate due = LocalDate.now(clock.withZone(ZoneOffset.UTC)).plusDays(1);
        assertThat(post(user, "/api/v1/tasks", body("id", task, "title", "Ưu tiên báo cáo",
                "description", "không có secret", "priority", "HIGH", "dueDate", due.toString()))
                .getResponse().getStatus()).isEqualTo(201);
        UUID conversation = conversation(user, "Ưu tiên");

        MvcResult sent = post(user, "/api/v1/ai/conversations/" + conversation + "/messages",
                body("message", "Hôm nay làm gì?", "mode", "CHAT",
                        "contextDomains", List.of("TASK_CALENDAR")));

        assertThat(sent.getResponse().getStatus()).as(sent.getResponse().getContentAsString()).isEqualTo(201);
        Map<String, Object> response = readJson(sent);
        Map<String, Object> assistant = cast(response.get("assistantMessage"));
        assertThat(assistant).containsEntry("content", "fake-response").containsEntry("contextAvailable", true);
        assertThat(provider.calls()).isOne();
        assertThat(provider.lastCallInTransaction()).isFalse();
        assertThat(provider.lastRequest().context().toString()).contains(task.toString(), "Ưu tiên báo cáo")
                .doesNotContain("email", "password", "token", "device", "push", "audit", "outbox");

        MvcResult disclosure = get(user, "/api/v1/ai/messages/" + assistant.get("id") + "/context");
        assertThat(disclosure.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> context = readJson(disclosure);
        assertThat(context.get("domains")).isEqualTo(List.of("TASK_CALENDAR"));
        assertThat(context.toString()).contains(task.toString(), "Ưu tiên báo cáo");
        assertThat(jdbc.sql("SELECT count(*) FROM ai_usage_logs WHERE user_id=:u AND result_code='SUCCESS'")
                .param("u", user.id()).query(Integer.class).single()).isOne();
    }

    @Test
    void AI_03_04_consentBlocksBeforeProviderAndRevocationTakesEffectImmediately() throws Exception {
        TestUser user = newUser();
        UUID conversation = conversation(user, "Consent");
        MvcResult blocked = chat(user, conversation);
        assertThat(blocked.getResponse().getStatus()).isEqualTo(403);
        assertThat(readJson(blocked)).containsEntry("errorCode", "AI_CONSENT_REQUIRED");
        assertThat(provider.calls()).isZero();

        consent(user, true);
        assertThat(chat(user, conversation).getResponse().getStatus()).isEqualTo(201);
        consent(user, false);
        MvcResult revoked = chat(user, conversation);
        assertThat(revoked.getResponse().getStatus()).isEqualTo(403);
        assertThat(provider.calls()).isOne();
    }

    @Test
    void AI_05_conversationAndContextIdorReturnSame404AsUnknownIds() throws Exception {
        TestUser owner = newUser();
        TestUser stranger = newUser();
        consent(owner, true);
        UUID conversation = conversation(owner, "Riêng tư");
        Map<String, Object> sent = readJson(chat(owner, conversation));
        UUID message = UUID.fromString(String.valueOf(cast(sent.get("assistantMessage")).get("id")));

        Map<String, Object> foreignConversation = problem(get(stranger, "/api/v1/ai/conversations/" + conversation));
        Map<String, Object> unknownConversation = problem(get(stranger, "/api/v1/ai/conversations/" + UUID.randomUUID()));
        assertThat(foreignConversation).isEqualTo(unknownConversation);
        Map<String, Object> foreignContext = problem(get(stranger, "/api/v1/ai/messages/" + message + "/context"));
        Map<String, Object> unknownContext = problem(get(stranger, "/api/v1/ai/messages/" + UUID.randomUUID() + "/context"));
        assertThat(foreignContext).isEqualTo(unknownContext);
    }

    @Test
    void AI_06_massAssignmentIsRejectedForConversationAndChat() throws Exception {
        TestUser user = newUser();
        UUID other = newUser().id();
        MvcResult create = post(user, "/api/v1/ai/conversations",
                body("title", "x", "userId", other));
        assertThat(create.getResponse().getStatus()).isEqualTo(400);
        UUID conversation = conversation(user, "x");
        MvcResult chat = post(user, "/api/v1/ai/conversations/" + conversation + "/messages",
                body("message", "x", "mode", "CHAT", "contextDomains", List.of(),
                        "model", "attacker-model", "cost", 0, "userId", other));
        assertThat(chat.getResponse().getStatus()).isEqualTo(400);
        assertThat(provider.calls()).isZero();
    }

    private UUID conversation(TestUser user, String title) throws Exception {
        MvcResult result = post(user, "/api/v1/ai/conversations", body("title", title));
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return UUID.fromString(String.valueOf(readJson(result).get("id")));
    }

    private MvcResult chat(TestUser user, UUID conversation) throws Exception {
        return post(user, "/api/v1/ai/conversations/" + conversation + "/messages",
                body("message", "Ưu tiên gì?", "mode", "CHAT",
                        "contextDomains", List.of("TASK_CALENDAR")));
    }

    private void consent(TestUser user, boolean allowed) throws Exception {
        MvcResult result = call(user, HttpMethod.PUT, "/api/v1/ai/preferences", body(
                "taskCalendar", allowed, "habit", false, "goal", false, "notes", false, "finance", false,
                "providerTermsAccepted", allowed));
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }
}
