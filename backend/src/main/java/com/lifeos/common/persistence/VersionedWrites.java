package com.lifeos.common.persistence;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ghi entity đồng bộ có kiểm tra version (D58, D80). Hai request đồng thời cùng version có thể cùng vượt qua bước
 * kiểm tường minh; @Version của Hibernate chặn request thứ hai khi flush. Transaction đó đã hỏng, nên version hiện
 * tại được đọc lại trong một transaction MỚI (REQUIRES_NEW, chỉ đọc) và trả CÙNG lỗi 409 ENTITY_VERSION_CONFLICT kèm
 * currentVersion như kiểm tra tường minh.
 * <p>
 * Đây là transaction boundary của Application Service gọi nó (lập trình bằng TransactionTemplate thay cho
 * @Transactional, vì lỗi phải được bắt NGOÀI transaction ghi). Gọi lồng trong một transaction có sẵn thì phần ghi
 * tham gia transaction đó (transaction ngoài bị đánh dấu rollback khi xung đột).
 */
@Component
public class VersionedWrites {

    private final TransactionTemplate writeTx;
    private final TransactionTemplate readNewTx;

    public VersionedWrites(PlatformTransactionManager transactionManager) {
        this.writeTx = new TransactionTemplate(transactionManager);
        this.readNewTx = new TransactionTemplate(transactionManager);
        this.readNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readNewTx.setReadOnly(true);
    }

    /**
     * @param write          use case ghi (chạy trong transaction)
     * @param currentVersion đọc version hiện tại của entity (kể cả đã xóa mềm) khi Hibernate phát hiện xung đột
     */
    public <T> T write(Supplier<T> write, LongSupplier currentVersion) {
        try {
            return writeTx.execute(status -> write.get());
        } catch (ObjectOptimisticLockingFailureException e) {
            Long current = readNewTx.execute(status -> currentVersion.getAsLong());
            throw versionConflict(current);
        }
    }

    /** Kiểm version tường minh (SYSTEM_ARCHITECTURE §3.4 bước 3). */
    public static void requireVersion(SyncedEntity entity, long expectedVersion) {
        if (entity.getVersion() != expectedVersion) {
            throw versionConflict(entity.getVersion());
        }
    }

    public static ConflictException versionConflict(long currentVersion) {
        ConflictException conflict = new ConflictException(ErrorCode.ENTITY_VERSION_CONFLICT);
        conflict.with("currentVersion", currentVersion);
        return conflict;
    }
}
