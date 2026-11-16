package com.lifeos.ai.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class AiStore {
    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() { };
    public record Preference(boolean taskCalendar, boolean habit, boolean goal, boolean notes, boolean finance,
            String policyVersion, Instant acceptedAt, Instant updatedAt) {
    }

    public record Usage(LocalDate date, long requests, long inputTokens, long outputTokens, long costMicrousd) {
    }

    public record Totals(long requests, long inputTokens, long outputTokens, long costMicrousd) {
    }

    public record Conversation(UUID id, String title, Instant lastMessageAt, Instant expiresAt, Instant createdAt,
            Instant updatedAt) { }

    public record Message(UUID id, UUID conversationId, String role, String kind, String content,
            Map<String, Object> structured, Map<String, Object> context, Instant createdAt) { }

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public AiStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public Optional<Preference> preference(UUID userId) {
        return jdbc.sql("""
                SELECT allow_task_calendar, allow_habit, allow_goal, allow_notes, allow_finance,
                       policy_version, accepted_provider_terms_at, updated_at
                FROM ai_user_preferences WHERE user_id = :u
                """).param("u", userId).query(AiStore::preference).optional();
    }

    public void replacePreference(UUID userId, boolean taskCalendar, boolean habit, boolean goal, boolean notes,
            boolean finance, String policyVersion, Instant acceptedAt, Instant now) {
        jdbc.sql("""
                INSERT INTO ai_user_preferences(user_id, allow_task_calendar, allow_habit, allow_goal, allow_notes,
                    allow_finance, policy_version, accepted_provider_terms_at, created_at, updated_at)
                VALUES (:u, :tc, :h, :g, :n, :f, :p, :a, :now, :now)
                ON CONFLICT (user_id) DO UPDATE SET allow_task_calendar = EXCLUDED.allow_task_calendar,
                    allow_habit = EXCLUDED.allow_habit, allow_goal = EXCLUDED.allow_goal,
                    allow_notes = EXCLUDED.allow_notes, allow_finance = EXCLUDED.allow_finance,
                    policy_version = EXCLUDED.policy_version,
                    accepted_provider_terms_at = EXCLUDED.accepted_provider_terms_at, updated_at = EXCLUDED.updated_at
                """).param("u", userId).param("tc", taskCalendar).param("h", habit).param("g", goal)
                .param("n", notes).param("f", finance).param("p", policyVersion).param("a", utc(acceptedAt))
                .param("now", utc(now)).update();
    }

    public List<Usage> usage(UUID userId, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return jdbc.sql("""
                SELECT (created_at AT TIME ZONE 'UTC')::date AS day, count(*) AS requests,
                       coalesce(sum(input_tokens), 0) AS input_tokens,
                       coalesce(sum(output_tokens), 0) AS output_tokens,
                       coalesce(sum(estimated_cost_microusd), 0) AS cost
                FROM ai_usage_logs WHERE user_id = :u AND created_at >= :start AND created_at < :end
                GROUP BY day ORDER BY day
                """).param("u", userId).param("start", utc(start)).param("end", utc(end))
                .query(AiStore::usage).list();
    }

    public Totals totals(UUID userId, Instant start, Instant end) {
        return jdbc.sql("""
                SELECT count(*) AS requests, coalesce(sum(input_tokens), 0) AS input_tokens,
                       coalesce(sum(output_tokens), 0) AS output_tokens,
                       coalesce(sum(estimated_cost_microusd), 0) AS cost
                FROM ai_usage_logs WHERE user_id = :u AND created_at >= :start AND created_at < :end
                """).param("u", userId).param("start", utc(start)).param("end", utc(end))
                .query(AiStore::totals).single();
    }

    public Conversation createConversation(UUID userId, UUID id, String title, Instant now, Instant expiresAt) {
        return jdbc.sql("""
                INSERT INTO ai_conversations(id,user_id,title,status,last_message_at,expires_at,created_at,updated_at)
                VALUES (:id,:u,:title,'ACTIVE',:now,:expires,:now,:now)
                RETURNING id,title,last_message_at,expires_at,created_at,updated_at
                """).param("id", id).param("u", userId).param("title", title).param("now", utc(now))
                .param("expires", utc(expiresAt)).query(AiStore::conversation).single();
    }

    public Optional<Conversation> conversation(UUID userId, UUID id) {
        return jdbc.sql("""
                SELECT id,title,last_message_at,expires_at,created_at,updated_at FROM ai_conversations
                WHERE user_id=:u AND id=:id
                """).param("u", userId).param("id", id).query(AiStore::conversation).optional();
    }

    public List<Conversation> conversations(UUID userId, Instant afterAt, UUID afterId, int limit) {
        if (afterAt == null) {
            return jdbc.sql("""
                    SELECT id,title,last_message_at,expires_at,created_at,updated_at FROM ai_conversations
                    WHERE user_id=:u ORDER BY last_message_at DESC,id DESC LIMIT :limit
                    """).param("u", userId).param("limit", limit).query(AiStore::conversation).list();
        }
        return jdbc.sql("""
                SELECT id,title,last_message_at,expires_at,created_at,updated_at FROM ai_conversations
                WHERE user_id=:u AND (last_message_at,id) < (:afterAt,:afterId)
                ORDER BY last_message_at DESC,id DESC LIMIT :limit
                """).param("u", userId).param("afterAt", utc(afterAt)).param("afterId", afterId)
                .param("limit", limit).query(AiStore::conversation).list();
    }

    public Message createMessage(UUID userId, UUID id, UUID conversationId, String role, String kind, String content,
            Map<String, Object> structured, Map<String, Object> context, String promptVersion, String provider,
            String model, Instant now) {
        return jdbc.sql("""
                INSERT INTO ai_messages(id,user_id,conversation_id,role,kind,content,structured_payload,
                    context_snapshot,prompt_version,provider,model,created_at)
                VALUES (:id,:u,:conversation,:role,:kind,:content,CAST(:structured AS jsonb),CAST(:context AS jsonb),
                    :promptVersion,:provider,:model,:now)
                RETURNING id,conversation_id,role,kind,content,structured_payload::text AS structured,
                    context_snapshot::text AS context,created_at
                """).param("id", id).param("u", userId).param("conversation", conversationId)
                .param("role", role).param("kind", kind).param("content", content)
                .param("structured", structured == null ? null : json.writeValueAsString(structured))
                .param("context", context == null ? null : json.writeValueAsString(context))
                .param("promptVersion", promptVersion).param("provider", provider).param("model", model)
                .param("now", utc(now)).query(this::message).single();
    }

    public Optional<Message> message(UUID userId, UUID id) {
        return jdbc.sql("""
                SELECT id,conversation_id,role,kind,content,structured_payload::text AS structured,
                    context_snapshot::text AS context,created_at FROM ai_messages WHERE user_id=:u AND id=:id
                """).param("u", userId).param("id", id).query(this::message).optional();
    }

    public List<Message> messages(UUID userId, UUID conversationId, Instant afterAt, UUID afterId, int limit) {
        if (afterAt == null) {
            return jdbc.sql("""
                    SELECT id,conversation_id,role,kind,content,structured_payload::text AS structured,
                        context_snapshot::text AS context,created_at FROM ai_messages
                    WHERE user_id=:u AND conversation_id=:conversation
                    ORDER BY created_at,id LIMIT :limit
                    """).param("u", userId).param("conversation", conversationId).param("limit", limit)
                    .query(this::message).list();
        }
        return jdbc.sql("""
                SELECT id,conversation_id,role,kind,content,structured_payload::text AS structured,
                    context_snapshot::text AS context,created_at FROM ai_messages
                WHERE user_id=:u AND conversation_id=:conversation
                  AND (created_at,id) > (:afterAt,:afterId)
                ORDER BY created_at,id LIMIT :limit
                """).param("u", userId).param("conversation", conversationId).param("afterAt", utc(afterAt))
                .param("afterId", afterId).param("limit", limit).query(this::message).list();
    }

    public void touchConversation(UUID userId, UUID id, Instant now, Instant expiresAt) {
        jdbc.sql("""
                UPDATE ai_conversations SET last_message_at=:now,updated_at=:now,expires_at=:expires
                WHERE user_id=:u AND id=:id
                """).param("now", utc(now)).param("expires", utc(expiresAt)).param("u", userId).param("id", id)
                .update();
    }

    public boolean deleteConversation(UUID userId, UUID id) {
        return jdbc.sql("DELETE FROM ai_conversations WHERE user_id=:u AND id=:id")
                .param("u", userId).param("id", id).update() == 1;
    }

    public void recordUsage(UUID userId, UUID conversationId, UUID messageId, String provider, String model,
            long inputTokens, long cachedInputTokens, long outputTokens, long reasoningTokens, long costMicrousd,
            long latencyMs, String resultCode, UUID correlationId, Instant now) {
        jdbc.sql("""
                INSERT INTO ai_usage_logs(id,user_id,conversation_id,message_id,provider,model,input_tokens,
                    cached_input_tokens,output_tokens,reasoning_tokens,estimated_cost_microusd,latency_ms,
                    result_code,correlation_id,created_at)
                VALUES (:id,:u,:conversation,:message,:provider,:model,:input,:cached,:output,:reasoning,:cost,
                    :latency,:result,:correlation,:now)
                """).param("id", UUID.randomUUID()).param("u", userId).param("conversation", conversationId)
                .param("message", messageId).param("provider", provider).param("model", model)
                .param("input", inputTokens).param("cached", cachedInputTokens).param("output", outputTokens)
                .param("reasoning", reasoningTokens).param("cost", costMicrousd).param("latency", latencyMs)
                .param("result", resultCode).param("correlation", correlationId).param("now", utc(now)).update();
    }

    public int deleteExpiredConversations(Instant now, int batch) {
        return jdbc.sql("""
                DELETE FROM ai_conversations WHERE id IN (
                    SELECT id FROM ai_conversations WHERE expires_at <= :now
                    ORDER BY expires_at,id LIMIT :batch)
                """).param("now", utc(now)).param("batch", batch).update();
    }

    public int deleteUsageBefore(Instant cutoff, int batch) {
        return jdbc.sql("""
                DELETE FROM ai_usage_logs WHERE id IN (
                    SELECT id FROM ai_usage_logs WHERE created_at < :cutoff
                    ORDER BY created_at,id LIMIT :batch)
                """).param("cutoff", utc(cutoff)).param("batch", batch).update();
    }

    public long deleteAll(UUID userId) {
        long count = 0;
        for (String table : List.of("ai_action_proposals", "ai_usage_logs", "ai_messages", "ai_conversations",
                "ai_user_preferences")) {
            count += jdbc.sql("DELETE FROM " + table + " WHERE user_id = :u").param("u", userId).update();
        }
        return count;
    }

    private static Preference preference(ResultSet rs, int row) throws SQLException {
        var accepted = rs.getTimestamp("accepted_provider_terms_at");
        return new Preference(rs.getBoolean("allow_task_calendar"), rs.getBoolean("allow_habit"),
                rs.getBoolean("allow_goal"), rs.getBoolean("allow_notes"), rs.getBoolean("allow_finance"),
                rs.getString("policy_version"), accepted == null ? null : accepted.toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }

    private static Usage usage(ResultSet rs, int row) throws SQLException {
        return new Usage(rs.getObject("day", LocalDate.class), rs.getLong("requests"),
                rs.getLong("input_tokens"), rs.getLong("output_tokens"), rs.getLong("cost"));
    }

    private static Totals totals(ResultSet rs, int row) throws SQLException {
        return new Totals(rs.getLong("requests"), rs.getLong("input_tokens"), rs.getLong("output_tokens"),
                rs.getLong("cost"));
    }

    private static Conversation conversation(ResultSet rs, int row) throws SQLException {
        return new Conversation(rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getTimestamp("last_message_at").toInstant(), rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    private Message message(ResultSet rs, int row) throws SQLException {
        return new Message(rs.getObject("id", UUID.class), rs.getObject("conversation_id", UUID.class),
                rs.getString("role"), rs.getString("kind"), rs.getString("content"), map(rs.getString("structured")),
                map(rs.getString("context")), rs.getTimestamp("created_at").toInstant());
    }

    private Map<String, Object> map(String value) {
        return value == null ? null : json.readValue(value, MAP);
    }

    private static OffsetDateTime utc(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
