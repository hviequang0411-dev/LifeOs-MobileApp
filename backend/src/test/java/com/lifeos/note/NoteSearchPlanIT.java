package com.lifeos.note;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.note.repository.NoteSearchStore;
import com.lifeos.support.ApiTestBase;
import com.lifeos.support.TestUsers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * KNW-17 (PHASE_6 §9, D182): EXPLAIN truy vấn tìm kiếm với 20 000 ghi chú của user đích (giữa 20 000 ghi chú của user
 * khác). Dùng CHÍNH hằng SQL của NoteSearchStore; phần khớp vector phải đi qua chỉ mục GIN ix_notes_search, không Seq
 * Scan trên notes. Kế hoạch ghi ra target/explain-notes.txt.
 */
class NoteSearchPlanIT extends ApiTestBase {

    private void seed(UUID user, int count) {
        jdbc.sql("""
                INSERT INTO notes (id, user_id, title, content, updated_at)
                SELECT gen_random_uuid(), :u, CASE WHEN g % 10 = 0 THEN 'Ghi chú ' || g ELSE NULL END,
                       'Nội dung số ' || g || CASE WHEN g % 997 = 0 THEN ' hóa đơn điện' ELSE ' việc thường ngày' END
                       || ' từ khóa' || (g % 500), now() - (g || ' minutes')::interval
                FROM generate_series(1, :n) g
                """).param("u", user).param("n", count).update();
    }

    @Test
    void Knw_17_searchUsesTheGinIndexAt20kNotes() throws Exception {
        UUID user = TestUsers.insert(jdbc);
        seed(user, 20_000);
        seed(TestUsers.insert(jdbc), 20_000);
        jdbc.sql("ANALYZE notes").update();
        jdbc.sql("ANALYZE note_tags").update();
        jdbc.sql("ANALYZE tags").update();

        String sql = NoteSearchStore.SEARCH_HITS + "SELECT n.id FROM notes n JOIN ranked r ON r.id = n.id"
                + " WHERE n.user_id = :userId AND n.deleted_at IS NULL AND n.archived_at IS NULL"
                + " ORDER BY r.rank DESC, n.updated_at DESC, n.id DESC OFFSET 0 LIMIT 51";
        String bound = sql.replace(":userId", "'" + user + "'").replace(":q", "'hoa don dien'");
        String plan = String.join("\n", jdbc.sql("EXPLAIN " + bound).query(String.class).list());
        String analyzed = String.join("\n", jdbc.sql("EXPLAIN (ANALYZE, BUFFERS) " + bound).query(String.class)
                .list());
        Path out = Path.of("target", "explain-notes.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, "# EXPLAIN tìm kiếm ghi chú — 20 000 ghi chú/user, 2 user\n\n" + bound + "\n\n" + analyzed
                + "\n", StandardCharsets.UTF_8);

        assertThat(plan).as(plan).contains("ix_notes_search").doesNotContain("Seq Scan on notes");
        assertThat(jdbc.sql(sql.replace(":userId", "'" + user + "'")).param("q", "hoa don dien").query(UUID.class)
                .list()).hasSize(20);
    }
}
