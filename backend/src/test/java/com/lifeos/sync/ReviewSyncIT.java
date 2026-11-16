package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** INT-11 qua sync (PHASE_7 §7, D202): review CREATE trùng kỳ → CONFLICT ALREADY_EXISTS; sửa đồng thời → CONFLICT. */
class ReviewSyncIT extends SyncTestBase {

    @Test
    @SuppressWarnings("unchecked")
    void Int_11_reviewSyncUsesAlreadyExistsAndTextConflicts() throws Exception {
        TestUser user = newUser();
        String today = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh")).toString();
        UUID id = UUID.randomUUID();
        assertThat(pushOne(user, mutation("review", id, "CREATE", 0, payload("periodType", "DAY", "periodKey", today,
                "reflection", "Máy A", "mood", 3)))).containsEntry("status", "APPLIED");
        Map<String, Object> dup = pushOne(user, mutation("review", UUID.randomUUID(), "CREATE", 0,
                payload("periodType", "DAY", "periodKey", today, "reflection", "Máy B")));
        assertThat(dup).containsEntry("status", "CONFLICT").containsEntry("conflictReason", "ALREADY_EXISTS");
        assertThat(((Map<?, ?>) dup.get("serverSnapshot")).get("entityId")).isEqualTo(id.toString());

        assertThat(pushOne(user, mutation("review", id, "UPDATE", 1, payload("reflection", "Máy A sửa"))))
                .containsEntry("status", "APPLIED");
        Map<String, Object> stale = pushOne(user, mutation("review", id, "UPDATE", 1, payload("reflection", "B sửa")));
        assertThat(stale).containsEntry("status", "CONFLICT");
        assertThat((Map<String, Object>) ((Map<String, Object>) stale.get("serverSnapshot")).get("data"))
                .containsEntry("reflection", "Máy A sửa").containsEntry("mood", 3);
        assertThat(pushOne(user, mutation("review", id, "UPDATE", 2, payload("periodKey", "2000-01-01"))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("review", id, "DELETE", 2, null))).containsEntry("status", "APPLIED");
    }
}
