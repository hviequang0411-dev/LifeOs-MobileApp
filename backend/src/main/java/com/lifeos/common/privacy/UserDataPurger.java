package com.lifeos.common.privacy;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Xóa cứng mọi dữ liệu của một người dùng (PHASE_7 D205): duyệt {@link UserDataCatalog#TABLES} theo thứ tự con trước
 * cha, mỗi lô ≤ 1000 dòng trong một transaction ngắn; sau đó sự kiện outbox còn chờ của user và cuối cùng dòng
 * {@code users}. Gọi lại an toàn (idempotent) nếu bị ngắt giữa chừng.
 */
@Component
public class UserDataPurger {

    static final int BATCH = 1000;

    private final JdbcClient jdbc;
    private final TransactionTemplate tx;

    public UserDataPurger(JdbcClient jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** @return tổng số dòng đã xóa (kể cả dòng users) */
    public long purge(UUID userId) {
        long total = 0;
        for (String table : UserDataCatalog.TABLES) {
            if (!table.matches("[a-z_]+")) {
                throw new IllegalArgumentException("Tên bảng không hợp lệ");
            }
            Integer deleted;
            do {
                deleted = tx.execute(s -> jdbc.sql("DELETE FROM " + table + " WHERE ctid IN (SELECT ctid FROM " + table
                        + " WHERE user_id = :u LIMIT " + BATCH + ")").param("u", userId).update());
                total += deleted == null ? 0 : deleted;
            } while (deleted != null && deleted == BATCH);
        }
        // outbox_events có user_id nên đã được xóa theo danh mục ở trên (đợt sửa F8); câu này giữ lại cho sự kiện cũ
        // có aggregate là user mà user_id chưa được điền.
        Integer outbox = tx.execute(s -> jdbc.sql("DELETE FROM outbox_events WHERE aggregate_type = 'user'"
                + " AND aggregate_id = :u").param("u", userId).update());
        Integer user = tx.execute(s -> jdbc.sql("DELETE FROM users WHERE id = :u").param("u", userId).update());
        return total + (outbox == null ? 0 : outbox) + (user == null ? 0 : user);
    }
}
