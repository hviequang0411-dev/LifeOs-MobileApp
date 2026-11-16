package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Sync cho note, tag, note_tag, note_link, goal, milestone và khóa goalId/milestoneId của task (PHASE_6 §7; KNW-02, 07,
 * 08, 10 qua sync). note_tag/note_link chỉ CREATE/DELETE; cascade server có mutation_id NULL.
 */
class KnowledgeGoalSyncIT extends SyncTestBase {

    private UUID create(TestUser user, String type, Map<String, Object> payload) throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> r = pushOne(user, mutation(type, id, "CREATE", 0, payload));
        assertThat(r).as(type + " " + r).containsEntry("status", "APPLIED");
        return id;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> result) {
        return (Map<String, Object>) ((Map<String, Object>) result.get("serverSnapshot")).get("data");
    }

    @Test
    void Knw_02_notePayloadMayReach192KbButTheBodyStaysWithin1Mb() throws Exception {
        TestUser user = newUser();
        String vietnamese = "ệ".repeat(50_000);
        assertThat(vietnamese.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(150_000);
        UUID note = create(user, "note", payload("title", "Dài", "content", vietnamese));
        assertThat(jdbc.sql("SELECT char_length(content) FROM notes WHERE id = :id").param("id", note)
                .query(Integer.class).single()).isEqualTo(50_000);

        Map<String, Object> tooBig = pushOne(user, mutation("note", UUID.randomUUID(), "CREATE", 0,
                payload("content", "😀".repeat(50_000))));
        assertThat(tooBig).containsEntry("status", "REJECTED").containsEntry("errorCode", "PAYLOAD_TOO_LARGE");
        Map<String, Object> taskTooBig = pushOne(user, mutation("task", UUID.randomUUID(), "CREATE", 0,
                payload("title", "x", "description", "ệ".repeat(30_000))));
        assertThat(taskTooBig).as("giới hạn 64 KB chung vẫn giữ cho loại khác").containsEntry("errorCode",
                "PAYLOAD_TOO_LARGE");

        List<Map<String, Object>> batch = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            batch.add(mutation("note", UUID.randomUUID(), "CREATE", 0, payload("content", vietnamese)));
        }
        MvcResult over = pushResult(user, batch.toArray(new Map[0]));
        assertThat(over.getResponse().getStatus()).as("tổng body > 1 MB").isEqualTo(413);
    }

    @Test
    void Knw_07_twoDevicesAddingDifferentTagsKeepBoth() throws Exception {
        TestUser user = newUser();
        UUID note = create(user, "note", payload("content", "ghi chú"));
        UUID work = create(user, "tag", payload("name", "Việc"));
        UUID home = create(user, "tag", payload("name", "Nhà"));

        UUID fromA = UUID.randomUUID();
        UUID fromB = UUID.randomUUID();
        assertThat(pushOne(user, mutation("note_tag", fromA, "CREATE", 0, payload("noteId", note.toString(),
                "tagId", work.toString())))).containsEntry("status", "APPLIED");
        assertThat(pushOne(user, mutation("note_tag", fromB, "CREATE", 0, payload("noteId", note.toString(),
                "tagId", home.toString())))).containsEntry("status", "APPLIED");
        assertThat(jdbc.sql("SELECT count(*) FROM note_tags WHERE note_id = :n AND deleted_at IS NULL")
                .param("n", note).query(Integer.class).single()).isEqualTo(2);

        Map<String, Object> dupPair = pushOne(user, mutation("note_tag", UUID.randomUUID(), "CREATE", 0,
                payload("noteId", note.toString(), "tagId", work.toString())));
        assertThat(dupPair).as("cặp trùng: APPLIED, không lỗi").containsEntry("status", "APPLIED")
                .containsEntry("entityVersion", 1);
        assertThat(pushOne(user, mutation("note_tag", fromA, "UPDATE", 1, payload("tagId", home.toString()))))
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "SYNC_OPERATION_NOT_ALLOWED");
        assertThat(pushOne(user, mutation("note_tag", UUID.randomUUID(), "CREATE", 0, payload("noteId",
                note.toString(), "tagId", UUID.randomUUID().toString())))).containsEntry("errorCode",
                "TAG_NOT_FOUND");
        assertThat(pushOne(user, mutation("note_tag", UUID.randomUUID(), "CREATE", 0, payload("noteId",
                UUID.randomUUID().toString(), "tagId", work.toString())))).containsEntry("errorCode",
                "NOTE_NOT_FOUND");
        assertThat(pushOne(user, mutation("note_tag", fromA, "DELETE", 1, null))).containsEntry("status", "APPLIED");
        assertThat(pushOne(user, mutation("note_tag", fromA, "DELETE", 1, null))).as("xóa lại tombstone")
                .containsEntry("status", "APPLIED");

        Map<String, Object> sameName = pushOne(user, mutation("tag", UUID.randomUUID(), "CREATE", 0,
                payload("name", "VIỆC")));
        assertThat(sameName).containsEntry("status", "CONFLICT").containsEntry("conflictReason", "ALREADY_EXISTS");
        assertThat(((Map<?, ?>) sameName.get("serverSnapshot")).get("entityId")).isEqualTo(work.toString());

        assertThat(pushOne(user, mutation("note", note, "DELETE", 1, null))).containsEntry("status", "APPLIED");
        assertThat(changeLogRows(user.id(), fromB)).last().satisfies(l -> assertThat(l)
                .containsEntry("operation", "DELETE").containsEntry("mutation_id", null));
    }

    @Test
    void Knw_08_concurrentNoteEditsConflictWithTheServerText() throws Exception {
        TestUser user = newUser();
        UUID note = create(user, "note", payload("title", "Gốc", "content", "dòng 1"));
        assertThat(pushOne(user, mutation("note", note, "UPDATE", 1, payload("content", "bản máy A"))))
                .containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        Map<String, Object> fromB = pushOne(user, mutation("note", note, "UPDATE", 1, payload("content",
                "bản máy B")));
        assertThat(fromB).containsEntry("status", "CONFLICT");
        assertThat(data(fromB)).containsEntry("content", "bản máy A").containsEntry("title", "Gốc");
        assertThat(pushOne(user, mutation("note", note, "UPDATE", 2, payload("pinned", true, "archived", true))))
                .containsEntry("status", "APPLIED");
        assertThat(jdbc.sql("SELECT pinned AND archived_at IS NOT NULL FROM notes WHERE id = :id").param("id", note)
                .query(Boolean.class).single()).isTrue();
    }

    @Test
    void Knw_10_goalMilestoneAndTaskLinksSyncAndCascade() throws Exception {
        TestUser user = newUser();
        UUID goal = create(user, "goal", payload("title", "Mục tiêu", "startOn", "2027-01-01", "targetOn",
                "2027-12-31"));
        UUID other = create(user, "goal", payload("title", "Khác"));
        UUID milestone = create(user, "milestone", payload("goalId", goal.toString(), "title", "Mốc",
                "position", 1000, "completed", true));
        assertThat(row("milestones", milestone).get("completed_at")).isNotNull();
        assertThat(pushOne(user, mutation("milestone", UUID.randomUUID(), "CREATE", 0, payload("goalId",
                UUID.randomUUID().toString(), "title", "Mồ côi")))).containsEntry("errorCode", "GOAL_NOT_FOUND");
        assertThat(pushOne(user, mutation("milestone", milestone, "UPDATE", 1, payload("goalId",
                other.toString())))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("goal", goal, "UPDATE", 1, payload("status", "ACHIEVED"))))
                .containsEntry("status", "APPLIED");
        assertThat(pushOne(user, mutation("goal", goal, "UPDATE", 2, payload("status", "PAUSED"))))
                .containsEntry("errorCode", "GOAL_INVALID_STATUS_TRANSITION");

        UUID task = create(user, "task", payload("title", "Việc", "milestoneId", milestone.toString()));
        assertThat(row("tasks", task)).containsEntry("goal_id", goal).containsEntry("milestone_id", milestone);
        assertThat(pushOne(user, mutation("task", UUID.randomUUID(), "CREATE", 0, payload("title", "Sai",
                "goalId", other.toString(), "milestoneId", milestone.toString())))).containsEntry("errorCode",
                "GOAL_LINK_MISMATCH");

        UUID note = create(user, "note", payload("content", "liên kết"));
        UUID link = create(user, "note_link", payload("noteId", note.toString(), "targetType", "GOAL", "targetId",
                goal.toString()));
        assertThat(pushOne(user, mutation("note_link", link, "UPDATE", 1, payload("targetType", "TASK"))))
                .containsEntry("errorCode", "SYNC_OPERATION_NOT_ALLOWED");

        UUID deletion = UUID.randomUUID();
        assertThat(pushOne(user, mutation(deletion, "goal", goal, "DELETE", 2, null))).containsEntry("status",
                "APPLIED");
        assertThat(row("tasks", task)).containsEntry("goal_id", null).containsEntry("milestone_id", null)
                .containsEntry("deleted_at", null).containsEntry("version", 2L);
        for (UUID child : List.of(task, milestone, link)) {
            assertThat(changeLogRows(user.id(), child)).last().satisfies(l -> assertThat(l)
                    .containsEntry("mutation_id", null));
        }
        assertThat(changeLogRows(user.id(), goal)).last().satisfies(l -> assertThat(l)
                .containsEntry("mutation_id", deletion).containsEntry("operation", "DELETE"));
        assertThat(longOf(changeLogRows(user.id(), goal).getLast().get("seq"))).isGreaterThan(
                longOf(changeLogRows(user.id(), link).getLast().get("seq")));
    }
}
