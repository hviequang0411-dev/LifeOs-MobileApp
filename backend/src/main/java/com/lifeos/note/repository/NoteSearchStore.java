package com.lifeos.note.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Danh sách và tìm kiếm ghi chú bằng SQL tham số hóa (D182): truy vấn người dùng chỉ đi qua
 * {@code websearch_to_tsquery('simple', lifeos_unaccent(:q))} — không bao giờ nối chuỗi vào tsquery. Danh sách không
 * trả content đầy đủ: excerpt 200 ký tự và contentLength (code point).
 */
@Repository
public class NoteSearchStore {

    /** Hàng danh sách ghi chú (không có content đầy đủ). */
    public record Row(UUID id, String title, String excerpt, int contentLength, boolean pinned, Instant archivedAt,
            long version, Instant createdAt, Instant updatedAt) {
        @Override
        public String toString() {
            return "NoteSearchStore.Row[id=" + id + "]";
        }
    }

    /** Bộ lọc chung: archived ∈ {false, true, all}; tagIds rỗng = không lọc; anyTag = OR thay vì AND. */
    public record Filter(String archived, Boolean pinned, List<UUID> tagIds, boolean anyTag) {
    }

    private static final String COLUMNS = """
            SELECT n.id, n.title, left(n.content, 200) AS excerpt, char_length(n.content) AS content_length,
                   n.pinned, n.archived_at, n.version, n.created_at, n.updated_at""";

    /**
     * Tìm toàn văn: khớp vector (GIN ix_notes_search) HOẶC tên thẻ (join) — UNION ALL để vẫn dùng chỉ mục. Biểu thức
     * tsquery viết thẳng (không qua CTE) để planner gấp hằng và chọn chỉ mục GIN (KNW-17).
     */
    public static final String SEARCH_HITS = """
            WITH hits AS (
                SELECT n.id, ts_rank_cd(n.search_vector, websearch_to_tsquery('simple', lifeos_unaccent(:q))) AS rank
                FROM notes n
                WHERE n.user_id = :userId AND n.deleted_at IS NULL
                  AND n.search_vector @@ websearch_to_tsquery('simple', lifeos_unaccent(:q))
                UNION ALL
                SELECT nt.note_id, 0.05 AS rank
                FROM note_tags nt JOIN tags t ON t.id = nt.tag_id
                WHERE nt.user_id = :userId AND nt.deleted_at IS NULL AND t.deleted_at IS NULL
                  AND to_tsvector('simple', lifeos_unaccent(t.name)) @@ websearch_to_tsquery('simple', lifeos_unaccent(:q))
            ),
            ranked AS (SELECT id, sum(rank) AS rank FROM hits GROUP BY id)
            """;

    private final JdbcClient jdbc;

    public NoteSearchStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Danh sách theo (updated_at desc, id desc), keyset. */
    public List<Row> page(UUID userId, Filter filter, Instant afterUpdatedAt, UUID afterId, int limit) {
        StringBuilder sql = new StringBuilder(COLUMNS).append(" FROM notes n WHERE n.user_id = :userId"
                + " AND n.deleted_at IS NULL");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        appendFilter(sql, params, filter);
        if (afterId != null) {
            sql.append(" AND (n.updated_at < :afterUpdatedAt OR (n.updated_at = :afterUpdatedAt AND n.id < :afterId))");
            params.put("afterUpdatedAt", utc(afterUpdatedAt));
            params.put("afterId", afterId);
        }
        sql.append(" ORDER BY n.updated_at DESC, n.id DESC LIMIT :limit");
        params.put("limit", limit);
        return jdbc.sql(sql.toString()).params(params).query((rs, i) -> row(rs)).list();
    }

    /** Tìm kiếm: xếp theo rank desc, updated_at desc, id desc; offset (≤ 1000, kiểm ở service). */
    public List<Row> search(UUID userId, String q, Filter filter, int offset, int limit) {
        StringBuilder sql = new StringBuilder(SEARCH_HITS).append(COLUMNS)
                .append(" FROM notes n JOIN ranked r ON r.id = n.id WHERE n.user_id = :userId AND n.deleted_at IS NULL");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("q", q);
        appendFilter(sql, params, filter);
        sql.append(" ORDER BY r.rank DESC, n.updated_at DESC, n.id DESC OFFSET :offset LIMIT :limit");
        params.put("offset", offset);
        params.put("limit", limit);
        return jdbc.sql(sql.toString()).params(params).query((rs, i) -> row(rs)).list();
    }

    /** Ghi chú còn sống liên kết tới mục tiêu (GET /goals/{id}/notes), keyset (updated_at desc, id desc). */
    public List<Row> linkedTo(UUID userId, String targetType, UUID targetId, Instant afterUpdatedAt, UUID afterId,
            int limit) {
        StringBuilder sql = new StringBuilder(COLUMNS).append(" FROM notes n WHERE n.user_id = :userId"
                + " AND n.deleted_at IS NULL AND EXISTS (SELECT 1 FROM note_links l WHERE l.note_id = n.id"
                + " AND l.user_id = :userId AND l.deleted_at IS NULL AND l.target_type = :targetType"
                + " AND l.target_id = :targetId)");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("targetType", targetType);
        params.put("targetId", targetId);
        if (afterId != null) {
            sql.append(" AND (n.updated_at < :afterUpdatedAt OR (n.updated_at = :afterUpdatedAt AND n.id < :afterId))");
            params.put("afterUpdatedAt", utc(afterUpdatedAt));
            params.put("afterId", afterId);
        }
        sql.append(" ORDER BY n.updated_at DESC, n.id DESC LIMIT :limit");
        params.put("limit", limit);
        return jdbc.sql(sql.toString()).params(params).query((rs, i) -> row(rs)).list();
    }

    /** Số ghi chú còn sống đang gắn thẻ (xác nhận xóa thẻ, UC-NOTE-04). */
    public long countNotesWithTag(UUID userId, UUID tagId) {
        return jdbc.sql("SELECT count(*) FROM note_tags WHERE user_id = :userId AND tag_id = :tagId"
                + " AND deleted_at IS NULL").param("userId", userId).param("tagId", tagId).query(Long.class).single();
    }

    private static void appendFilter(StringBuilder sql, Map<String, Object> params, Filter f) {
        switch (f.archived()) {
            case "true" -> sql.append(" AND n.archived_at IS NOT NULL");
            case "all" -> { }
            default -> sql.append(" AND n.archived_at IS NULL");
        }
        if (f.pinned() != null) {
            sql.append(" AND n.pinned = :pinned");
            params.put("pinned", f.pinned());
        }
        if (f.tagIds().isEmpty()) {
            return;
        }
        if (f.anyTag()) {
            sql.append(" AND EXISTS (SELECT 1 FROM note_tags x WHERE x.note_id = n.id AND x.deleted_at IS NULL"
                    + " AND x.tag_id IN (:anyTags))");
            params.put("anyTags", f.tagIds());
            return;
        }
        List<UUID> tags = new ArrayList<>(f.tagIds());
        for (int i = 0; i < tags.size(); i++) {
            sql.append(" AND EXISTS (SELECT 1 FROM note_tags x").append(i).append(" WHERE x").append(i)
                    .append(".note_id = n.id AND x").append(i).append(".deleted_at IS NULL AND x").append(i)
                    .append(".tag_id = :tag").append(i).append(')');
            params.put("tag" + i, tags.get(i));
        }
    }

    private static Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Row(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("excerpt"),
                rs.getInt("content_length"), rs.getBoolean("pinned"), instant(rs.getTimestamp("archived_at")),
                rs.getLong("version"), instant(rs.getTimestamp("created_at")),
                instant(rs.getTimestamp("updated_at")));
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    /** Cho các truy vấn tập id. */
    static <T> Collection<T> nonEmpty(Collection<T> c) {
        return c;
    }
}
