package com.lifeos.common.privacy;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Xuất toàn bộ dữ liệu của một người dùng ra tệp zip (PHASE_7 D204): {@code data.json} (mọi bảng xuất được của
 * {@link UserDataCatalog} + hồ sơ, không băm mật khẩu) và một CSV/bảng (UTF-8 có BOM, chống CSV injection). Mỗi bảng đọc
 * trong một transaction {@code REPEATABLE READ READ ONLY} riêng, con trỏ JDBC theo lô (fetchSize) — không giữ
 * transaction dài, bộ nhớ chặn. Vượt {@code maxBytes} ⇒ {@link TooLargeException}.
 */
@Component
public class UserDataExporter {

    /** Vượt kích thước tối đa (EXPORT_TOO_LARGE). */
    public static class TooLargeException extends RuntimeException {
        public TooLargeException() {
            super("EXPORT_TOO_LARGE");
        }
    }

    private static final String PROFILE = "SELECT id, email, display_name, email_verified_at, status, created_at,"
            + " updated_at FROM users WHERE id = ?";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate snapshotTx;
    private final JsonMapper json;

    public UserDataExporter(DataSource dataSource, PlatformTransactionManager transactionManager, JsonMapper json) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setFetchSize(500);
        this.snapshotTx = new TransactionTemplate(transactionManager);
        this.snapshotTx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.snapshotTx.setReadOnly(true);
        this.json = json;
    }

    /** @return kích thước tệp zip (byte) */
    public long export(UUID userId, Path target, long maxBytes, Instant exportedAt) throws IOException {
        Files.createDirectories(target.getParent());
        try (OutputStream file = Files.newOutputStream(target);
                Counting counting = new Counting(new BufferedOutputStream(file), maxBytes);
                ZipOutputStream zip = new ZipOutputStream(counting, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("data.json"));
            Writer out = new OutputStreamWriter(new NonClosing(zip), StandardCharsets.UTF_8);
            out.write("{\"exportedAt\":\"" + exportedAt + "\",\"user\":");
            List<Map<String, Object>> profile = snapshotTx.execute(s -> jdbc.query(PROFILE,
                    (rs, n) -> row(rs, List.of()), userId));
            out.write(json.writeValueAsString(profile == null || profile.isEmpty() ? null : profile.get(0)));
            out.write(",\"tables\":{");
            boolean firstTable = true;
            for (String table : UserDataCatalog.exportedTables()) {
                out.write((firstTable ? "" : ",") + "\"" + table + "\":[");
                firstTable = false;
                List<String> skip = UserDataCatalog.EXPORT_EXCLUDED_COLUMNS.getOrDefault(table, List.of());
                boolean[] first = {true};
                stream(table, userId, rs -> {
                    write(out, (first[0] ? "" : ",") + json.writeValueAsString(row(rs, skip)));
                    first[0] = false;
                });
                out.write("]");
            }
            out.write("}}");
            out.flush();
            zip.closeEntry();
            for (String table : UserDataCatalog.exportedTables()) {
                zip.putNextEntry(new ZipEntry("csv/" + table + ".csv"));
                Writer csv = new OutputStreamWriter(new NonClosing(zip), StandardCharsets.UTF_8);
                csv.write('\uFEFF');
                List<String> skip = UserDataCatalog.EXPORT_EXCLUDED_COLUMNS.getOrDefault(table, List.of());
                boolean[] header = {true};
                stream(table, userId, rs -> {
                    Map<String, Object> r = row(rs, skip);
                    if (header[0]) {
                        write(csv, csvLine(List.copyOf(r.keySet())));
                        header[0] = false;
                    }
                    write(csv, csvLine(r.values().stream().map(v -> v == null ? "" : String.valueOf(v)).toList()));
                });
                csv.flush();
                zip.closeEntry();
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return Files.size(target);
    }

    private interface RowSink {
        void accept(ResultSet rs) throws SQLException;
    }

    private void stream(String table, UUID userId, RowSink sink) {
        if (!table.matches("[a-z_]+")) {
            throw new IllegalArgumentException("Tên bảng không hợp lệ");
        }
        snapshotTx.executeWithoutResult(s -> jdbc.query("SELECT * FROM " + table + " WHERE user_id = ?",
                rs -> {
                    sink.accept(rs);
                }, userId));
    }

    private static void write(Writer w, String text) {
        try {
            w.write(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Map<String, Object> row(ResultSet rs, List<String> skip) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String name = meta.getColumnLabel(i);
            if (skip.contains(name) || name.endsWith("_hash") || "tsvector".equals(meta.getColumnTypeName(i))) {
                continue;
            }
            row.put(name, value(rs.getObject(i)));
        }
        return row;
    }

    static Object value(Object v) {
        if (v == null || v instanceof String || v instanceof Boolean || v instanceof Integer || v instanceof Long
                || v instanceof Short) {
            return v;
        }
        if (v instanceof java.math.BigDecimal d) {
            return d.toPlainString();
        }
        if (v instanceof java.sql.Timestamp ts) {
            return ts.toInstant().toString();
        }
        if (v instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant().toString();
        }
        if (v instanceof byte[]) {
            return null;
        }
        return v.toString();
    }

    /** Một dòng CSV RFC 4180; ô bắt đầu bằng = + - @ TAB CR được thêm ' (chống formula injection, D204). */
    static String csvLine(List<String> cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);
            if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) {
                cell = "'" + cell;
            }
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r")) {
                cell = "\"" + cell.replace("\"", "\"\"") + "\"";
            }
            line.append(i == 0 ? "" : ",").append(cell);
        }
        return line.append("\r\n").toString();
    }

    /** Đếm byte đã ghi ra tệp; vượt giới hạn ⇒ TooLargeException. */
    private static final class Counting extends FilterOutputStream {
        private final long max;
        private long count;

        Counting(OutputStream out, long max) {
            super(out);
            this.max = max;
        }

        @Override
        public void write(int b) throws IOException {
            add(1);
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            add(len);
            out.write(b, off, len);
        }

        private void add(long n) {
            count += n;
            if (count > max) {
                throw new TooLargeException();
            }
        }
    }

    /** Writer bọc entry zip không được đóng zip. */
    private static final class NonClosing extends FilterOutputStream {
        NonClosing(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            flush();
        }
    }
}
