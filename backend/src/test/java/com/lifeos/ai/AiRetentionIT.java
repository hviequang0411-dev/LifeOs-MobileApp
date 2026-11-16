package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.ai.service.AiRetentionJob;
import com.lifeos.support.ApiTestBase;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiRetentionIT extends ApiTestBase {
    @Autowired
    AiRetentionJob retention;

    @Test
    void AI_32_retentionDeletesExpiredRowsInBatchesAndIsIdempotent() {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID expired = conversation(user.id(), clock.instant().minusSeconds(1));
        UUID live = conversation(user.id(), clock.instant().plus(Duration.ofDays(1)));
        UUID otherLive = conversation(other.id(), clock.instant().plus(Duration.ofDays(1)));
        message(user.id(), expired);
        usage(user.id(), clock.instant().minus(Duration.ofDays(366)));
        usage(user.id(), clock.instant());

        retention.runOnce();
        retention.runOnce();

        assertThat(exists("ai_conversations", expired)).isFalse();
        assertThat(exists("ai_conversations", live)).isTrue();
        assertThat(exists("ai_conversations", otherLive)).isTrue();
        assertThat(jdbc.sql("SELECT count(*) FROM ai_messages WHERE conversation_id=:c")
                .param("c", expired).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM ai_usage_logs WHERE user_id=:u")
                .param("u", user.id()).query(Integer.class).single()).isOne();
    }

    private UUID conversation(UUID userId, java.time.Instant expires) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO ai_conversations(id,user_id,title,status,last_message_at,expires_at,created_at,updated_at)
                VALUES (:id,:u,'retention','ACTIVE',:now,:expires,:now,:now)
                """).param("id", id).param("u", userId).param("now", clock.instant().atOffset(ZoneOffset.UTC))
                .param("expires", expires.atOffset(ZoneOffset.UTC)).update();
        return id;
    }

    private void message(UUID userId, UUID conversation) {
        jdbc.sql("""
                INSERT INTO ai_messages(id,user_id,conversation_id,role,kind,content,created_at)
                VALUES (:id,:u,:c,'USER','CHAT','fixture',:now)
                """).param("id", UUID.randomUUID()).param("u", userId).param("c", conversation)
                .param("now", clock.instant().atOffset(ZoneOffset.UTC)).update();
    }

    private void usage(UUID userId, java.time.Instant created) {
        jdbc.sql("""
                INSERT INTO ai_usage_logs(id,user_id,provider,model,input_tokens,output_tokens,
                    estimated_cost_microusd,latency_ms,result_code,created_at)
                VALUES (:id,:u,'fake','fake',1,1,12,3,'SUCCESS',:created)
                """).param("id", UUID.randomUUID()).param("u", userId)
                .param("created", created.atOffset(ZoneOffset.UTC)).update();
    }

    private boolean exists(String table, UUID id) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE id=:id")
                .param("id", id).query(Integer.class).single() == 1;
    }
}
