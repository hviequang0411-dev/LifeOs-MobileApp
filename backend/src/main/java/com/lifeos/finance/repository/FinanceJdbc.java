package com.lifeos.finance.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Truy vấn tổng hợp và lệnh ghi đặc thù của Finance bằng SQL tham số hóa (không nối chuỗi giá trị người dùng). Số dư,
 * mức dùng ngân sách và báo cáo LUÔN tính từ lịch sử (D102), chỉ bản ghi chưa xóa. Mọi câu lệnh lọc theo user_id và
 * khớp chỉ mục một phần của V6 (FIN-23 kiểm EXPLAIN bằng chính các hằng SQL ở đây).
 */
@Repository
public class FinanceJdbc {

    /** Giao dịch có danh mục ADJUSTMENT: tính vào số dư, KHÔNG tính vào ngân sách và báo cáo thu/chi (D119). */
    static final String NOT_ADJUSTMENT = " AND NOT EXISTS (SELECT 1 FROM financial_categories c"
            + " WHERE c.id = t.category_id AND c.system_key = 'ADJUSTMENT')";

    public static final String BALANCES = """
            SELECT a.id,
                   a.opening_balance
                 + COALESCE((SELECT SUM(CASE WHEN t.kind = 'INCOME' THEN t.amount ELSE -t.amount END)
                             FROM financial_transactions t
                             WHERE t.user_id = a.user_id AND t.account_id = a.id AND t.deleted_at IS NULL
                               AND t.occurred_on <= :asOf), 0)
                 + COALESCE((SELECT SUM(x.amount) FROM account_transfers x
                             WHERE x.user_id = a.user_id AND x.to_account_id = a.id AND x.deleted_at IS NULL
                               AND x.occurred_on <= :asOf), 0)
                 - COALESCE((SELECT SUM(x.amount) FROM account_transfers x
                             WHERE x.user_id = a.user_id AND x.from_account_id = a.id AND x.deleted_at IS NULL
                               AND x.occurred_on <= :asOf), 0) AS balance
            FROM financial_accounts a
            WHERE a.user_id = :userId AND a.id IN (:ids)
            """;

    public static final String SPENT = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM financial_transactions t
            WHERE t.user_id = :userId AND t.deleted_at IS NULL AND t.kind = 'EXPENSE' AND t.currency = :currency
              AND t.occurred_on >= :from AND t.occurred_on < :until
              AND (CAST(:categoryId AS uuid) IS NULL OR t.category_id = :categoryId)""" + NOT_ADJUSTMENT;

    /** Nhóm theo ngày, loại, danh mục, tài khoản cho báo cáo tháng/dòng tiền (FR-FIN-10). */
    public static final String REPORT_ROWS = """
            SELECT t.occurred_on, t.kind, t.category_id, t.account_id, SUM(t.amount) AS total
            FROM financial_transactions t
            WHERE t.user_id = :userId AND t.deleted_at IS NULL AND t.currency = :currency
              AND t.occurred_on >= :from AND t.occurred_on <= :to""" + NOT_ADJUSTMENT + """

            GROUP BY t.occurred_on, t.kind, t.category_id, t.account_id
            """;

    public static final String CONTRIBUTION_SUMS = """
            SELECT c.goal_id, SUM(c.amount) AS saved
            FROM saving_contributions c
            WHERE c.user_id = :userId AND c.goal_id IN (:goalIds) AND c.deleted_at IS NULL
            GROUP BY c.goal_id
            """;

    /** D110: idempotent khi job chạy lặp/song song — khóa UNIQUE (recurring_transaction_id, recurrence_date). */
    static final String INSERT_OCCURRENCE = """
            INSERT INTO financial_transactions
                (id, user_id, account_id, category_id, kind, amount, currency, occurred_on, note,
                 recurring_transaction_id, recurrence_date, version, created_at, updated_at)
            VALUES (:id, :userId, :accountId, :categoryId, :kind, :amount, :currency, :date, :note,
                    :recurringId, :date, 1, :now, :now)
            ON CONFLICT (recurring_transaction_id, recurrence_date) WHERE recurring_transaction_id IS NOT NULL
            DO NOTHING
            """;

    static final String INSERT_ALERT = """
            INSERT INTO budget_alerts (id, user_id, budget_id, month, threshold, created_at)
            VALUES (:id, :userId, :budgetId, :month, :threshold, :now)
            ON CONFLICT (budget_id, month, threshold) DO NOTHING
            """;

    private final JdbcClient jdbc;

    public FinanceJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Số dư của các tài khoản (của user) tại cuối ngày {@code asOf}. */
    public Map<UUID, BigDecimal> balances(UUID userId, Collection<UUID> accountIds, LocalDate asOf) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (accountIds.isEmpty()) {
            return result;
        }
        jdbc.sql(BALANCES).param("userId", userId).param("ids", accountIds).param("asOf", asOf)
                .query((rs, n) -> {
                    result.put(rs.getObject("id", UUID.class), rs.getBigDecimal("balance"));
                    return null;
                }).list();
        return result;
    }

    /** Σ chi (EXPENSE, chưa xóa, không ADJUSTMENT) cùng tiền tệ trong [from, until), lọc danh mục nếu có (D109). */
    public BigDecimal spent(UUID userId, String currency, UUID categoryIdOrNull, LocalDate from, LocalDate until) {
        return jdbc.sql(SPENT).param("userId", userId).param("currency", currency)
                .param("categoryId", categoryIdOrNull, Types.OTHER).param("from", from).param("until", until)
                .query(BigDecimal.class).single();
    }

    public record ReportRow(LocalDate date, String kind, UUID categoryId, UUID accountId, BigDecimal total) {
    }

    /** PHASE_7 D199: Σ thu/chi theo tiền tệ trong [from, to] (loại trừ chuyển khoản, tombstone, ADJUSTMENT). */
    public static final String CURRENCY_TOTALS = """
            SELECT t.currency, t.kind, SUM(t.amount) AS total
            FROM financial_transactions t
            WHERE t.user_id = :userId AND t.deleted_at IS NULL AND t.occurred_on >= :from AND t.occurred_on <= :to"""
            + NOT_ADJUSTMENT + " GROUP BY t.currency, t.kind ORDER BY t.currency";

    public record CurrencyKindTotal(String currency, String kind, BigDecimal total) {
    }

    public List<CurrencyKindTotal> currencyTotals(UUID userId, LocalDate from, LocalDate to) {
        return jdbc.sql(CURRENCY_TOTALS).param("userId", userId).param("from", from).param("to", to)
                .query((rs, n) -> new CurrencyKindTotal(rs.getString("currency"), rs.getString("kind"),
                        rs.getBigDecimal("total")))
                .list();
    }

    public List<ReportRow> reportRows(UUID userId, String currency, LocalDate from, LocalDate to) {
        return jdbc.sql(REPORT_ROWS).param("userId", userId).param("currency", currency).param("from", from)
                .param("to", to)
                .query((rs, n) -> new ReportRow(rs.getObject("occurred_on", LocalDate.class), rs.getString("kind"),
                        rs.getObject("category_id", UUID.class), rs.getObject("account_id", UUID.class),
                        rs.getBigDecimal("total")))
                .list();
    }

    public Map<UUID, BigDecimal> contributionSums(UUID userId, Collection<UUID> goalIds) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        if (goalIds.isEmpty()) {
            return result;
        }
        jdbc.sql(CONTRIBUTION_SUMS).param("userId", userId).param("goalIds", goalIds)
                .query((rs, n) -> {
                    result.put(rs.getObject("goal_id", UUID.class), rs.getBigDecimal("saved"));
                    return null;
                }).list();
        return result;
    }

    /** Bộ lọc tìm kiếm giao dịch (FR-FIN-11); trường null = không lọc. q đã được escape cho LIKE. */
    public record SearchFilter(String kind, UUID accountId, UUID categoryId, LocalDate from, LocalDate to,
            BigDecimal minAmount, BigDecimal maxAmount, String likePattern) {
        @Override
        public String toString() {
            return "SearchFilter[]";
        }
    }

    /**
     * id giao dịch khớp bộ lọc, sắp occurredOn giảm dần rồi id giảm dần, keyset sau (afterDate, afterId). SQL dựng từ
     * các mảnh CỐ ĐỊNH theo bộ lọc có mặt; mọi giá trị là tham số. {@code q} tìm trong note bằng ILIKE … ESCAPE.
     */
    public List<UUID> searchTransactionIds(UUID userId, SearchFilter f, LocalDate afterDate, UUID afterId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT t.id FROM financial_transactions t
                WHERE t.user_id = :userId AND t.deleted_at IS NULL""");
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        if (f.kind() != null) {
            sql.append(" AND t.kind = :kind");
            params.put("kind", f.kind());
        }
        if (f.accountId() != null) {
            sql.append(" AND t.account_id = :accountId");
            params.put("accountId", f.accountId());
        }
        if (f.categoryId() != null) {
            sql.append(" AND t.category_id = :categoryId");
            params.put("categoryId", f.categoryId());
        }
        if (f.from() != null) {
            sql.append(" AND t.occurred_on >= :from");
            params.put("from", f.from());
        }
        if (f.to() != null) {
            sql.append(" AND t.occurred_on <= :to");
            params.put("to", f.to());
        }
        if (f.minAmount() != null) {
            sql.append(" AND t.amount >= :minAmount");
            params.put("minAmount", f.minAmount());
        }
        if (f.maxAmount() != null) {
            sql.append(" AND t.amount <= :maxAmount");
            params.put("maxAmount", f.maxAmount());
        }
        if (f.likePattern() != null) {
            sql.append(" AND t.note ILIKE :pattern ESCAPE '\\'");
            params.put("pattern", f.likePattern());
        }
        if (afterDate != null) {
            sql.append(" AND (t.occurred_on < :afterDate OR (t.occurred_on = :afterDate AND t.id < :afterId))");
            params.put("afterDate", afterDate);
            params.put("afterId", afterId);
        }
        sql.append(" ORDER BY t.occurred_on DESC, t.id DESC LIMIT :limit");
        params.put("limit", limit);
        return jdbc.sql(sql.toString()).params(params).query(UUID.class).list();
    }

    public record Occurrence(UUID id, UUID userId, UUID accountId, UUID categoryId, String kind, BigDecimal amount,
            String currency, LocalDate date, String note, UUID recurringId) {
        @Override
        public String toString() {
            return "Occurrence[id=" + id + ", recurringId=" + recurringId + "]";
        }
    }

    /** @return true nếu đã chèn (false: lần xuất hiện này đã có giao dịch, kể cả tombstone) */
    public boolean insertOccurrence(Occurrence o, Instant now) {
        return jdbc.sql(INSERT_OCCURRENCE)
                .param("id", o.id()).param("userId", o.userId()).param("accountId", o.accountId())
                .param("categoryId", o.categoryId(), Types.OTHER).param("kind", o.kind()).param("amount", o.amount())
                .param("currency", o.currency()).param("date", o.date()).param("note", o.note())
                .param("recurringId", o.recurringId()).param("now", utc(now))
                .update() == 1;
    }

    /** @return true nếu đây là lần đầu ngưỡng này của (ngân sách, tháng) được ghi nhận */
    public boolean insertAlert(UUID userId, UUID budgetId, LocalDate month, int threshold, Instant now) {
        return jdbc.sql(INSERT_ALERT).param("id", UUID.randomUUID()).param("userId", userId)
                .param("budgetId", budgetId).param("month", month).param("threshold", threshold)
                .param("now", utc(now)).update() == 1;
    }

    /** Ngưỡng đã ghi nhận của các ngân sách trong một tháng (để hiển thị/kiểm thử). */
    public List<Integer> alerts(UUID userId, UUID budgetId, LocalDate month) {
        return jdbc.sql("""
                SELECT threshold FROM budget_alerts
                WHERE user_id = :userId AND budget_id = :budgetId AND month = :month ORDER BY threshold
                """).param("userId", userId).param("budgetId", budgetId).param("month", month)
                .query(Integer.class).list();
    }

    public Optional<Long> transactionVersion(UUID id) {
        return jdbc.sql("SELECT version FROM financial_transactions WHERE id = :id").param("id", id)
                .query(Long.class).optional();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    /** Escape ký tự đặc biệt của LIKE (\, %, _) để q được tìm nguyên văn (FIN-17). */
    public static String likeContains(String q) {
        StringBuilder out = new StringBuilder("%");
        for (char ch : q.toCharArray()) {
            if (ch == '\\' || ch == '%' || ch == '_') {
                out.append('\\');
            }
            out.append(ch);
        }
        return out.append('%').toString();
    }
}
