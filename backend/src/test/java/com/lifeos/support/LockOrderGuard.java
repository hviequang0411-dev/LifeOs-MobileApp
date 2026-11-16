package com.lifeos.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Kiểm tra ĐỘNG bất biến thứ tự khóa D86 / SYN-48, CHỈ trong cấu hình test (LockOrderGuardConfig). Bọc DataSource của
 * ứng dụng và theo dõi từng kết nối trong một giao dịch: sau câu upsert {@code user_sync_state} đầu tiên (lần gọi
 * ChangeRecorder.record đầu tiên, chiếm khóa dòng theo user đến khi commit), giao dịch chỉ còn được ghi vào
 * {@code change_logs}, {@code sync_mutations} và {@code user_sync_state}. Mọi INSERT/UPDATE/DELETE vào bảng khác hoặc
 * SELECT … FOR UPDATE/SHARE sau thời điểm đó là vi phạm: ném {@link Violation} ngay lập tức, nên MỌI test tích hợp
 * dùng context chung đều đỏ nếu đi qua một đường ghi sai thứ tự. Câu SQL đều có tham số, nên thông báo không chứa dữ
 * liệu người dùng.
 */
public final class LockOrderGuard extends DelegatingDataSource {

    /** Vi phạm D86. Không phải SQLException để không bị dịch thành lỗi dữ liệu thông thường. */
    public static final class Violation extends IllegalStateException {
        Violation(String message) {
            super(message);
        }
    }

    private static final Set<String> ALLOWED_AFTER_SEQ = Set.of("change_logs", "sync_mutations", "user_sync_state");
    private static final Pattern WRITE = Pattern.compile("^(?:insert\\s+into|update|delete\\s+from)\\s+\"?([a-z_]+)");
    private static final Pattern LOCKING_SELECT = Pattern.compile("\\bfor\\s+(?:update|share|no\\s+key\\s+update"
            + "|key\\s+share)\\b");

    private static final List<String> VIOLATIONS = new CopyOnWriteArrayList<>();

    public LockOrderGuard(DataSource target) {
        super(target);
    }

    /** Các vi phạm đã phát hiện từ khi JVM test khởi động (để test riêng kiểm). */
    public static List<String> violations() {
        return List.copyOf(VIOLATIONS);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return guard(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return guard(super.getConnection(username, password));
    }

    private static Connection guard(Connection target) {
        return (Connection) Proxy.newProxyInstance(LockOrderGuard.class.getClassLoader(),
                new Class<?>[] {Connection.class}, new ConnectionHandler(target));
    }

    /** Trạng thái theo một lần mượn kết nối (một giao dịch Spring giữ đúng một kết nối đến khi commit). */
    private static final class ConnectionHandler implements InvocationHandler {

        private final Connection target;
        private boolean seqTaken;

        ConnectionHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            switch (name) {
                case "commit", "rollback", "close" -> seqTaken = false;
                case "setAutoCommit" -> {
                    if (Boolean.TRUE.equals(args[0])) {
                        seqTaken = false;
                    }
                }
                case "prepareStatement", "prepareCall" -> inspect((String) args[0]);
                default -> {
                }
            }
            Object result = call(target, method, args);
            if ("createStatement".equals(name)) {
                Statement statement = (Statement) result;
                return Proxy.newProxyInstance(LockOrderGuard.class.getClassLoader(), new Class<?>[] {Statement.class},
                        (p, m, a) -> {
                            if (m.getName().startsWith("execute") && a != null && a.length > 0
                                    && a[0] instanceof String sql) {
                                inspect(sql);
                            }
                            return call(statement, m, a);
                        });
            }
            return result;
        }

        private void inspect(String rawSql) throws SQLException {
            if (target.getAutoCommit()) {
                return;
            }
            String sql = rawSql.strip().toLowerCase(Locale.ROOT);
            Matcher write = WRITE.matcher(sql);
            String table = write.find() ? write.group(1) : null;
            if (seqTaken) {
                boolean forbiddenWrite = table != null && !ALLOWED_AFTER_SEQ.contains(table);
                boolean lockingSelect = sql.startsWith("select") && LOCKING_SELECT.matcher(sql).find();
                if (forbiddenWrite || lockingSelect) {
                    String message = "D86: khóa/ghi nghiệp vụ SAU ChangeRecorder.record: "
                            + sql.substring(0, Math.min(sql.length(), 120));
                    VIOLATIONS.add(message);
                    throw new Violation(message);
                }
            }
            if ("user_sync_state".equals(table) || sql.startsWith("insert into user_sync_state")) {
                seqTaken = true;
            }
        }

        private static Object call(Object target, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
