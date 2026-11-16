package com.lifeos.identity.service;

import com.lifeos.common.provisioning.UserProvisioner;
import com.lifeos.common.provisioning.UserProvisioner.PendingChange;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.identity.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dữ liệu mặc định do các module nghiệp vụ sở hữu ({@link UserProvisioner}, ví dụ danh mục thu/chi hệ thống D113):
 * tạo lúc đăng ký (cùng transaction với RegistrationService) và bù cho người dùng có sẵn ({@link #backfill}, chạy khi
 * khởi động). identity chỉ biết interface ở common.
 * <p>
 * Thứ tự D86: provision (ghi nghiệp vụ, flush) TRƯỚC; record (change_logs CREATE, mutation_id NULL) SAU CÙNG.
 */
@Service
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);
    private static final int PAGE = 200;

    private final List<UserProvisioner> provisioners;
    private final ChangeRecorder changeRecorder;
    private final UserRepository users;
    private final TransactionTemplate tx;

    public UserProvisioningService(List<UserProvisioner> provisioners, ChangeRecorder changeRecorder,
            UserRepository users, TransactionTemplate tx) {
        this.provisioners = provisioners;
        this.changeRecorder = changeRecorder;
        this.users = users;
        this.tx = tx;
    }

    /** Ghi dữ liệu mặc định còn thiếu; phải gọi trong transaction đang mở. */
    public List<PendingChange> provision(UUID userId) {
        List<PendingChange> pending = new ArrayList<>();
        for (UserProvisioner provisioner : provisioners) {
            pending.addAll(provisioner.provision(userId));
        }
        return pending;
    }

    /** change_logs cho dữ liệu vừa tạo; gọi SAU mọi ghi nghiệp vụ của transaction (D86). */
    public void record(UUID userId, List<PendingChange> pending) {
        for (PendingChange change : pending) {
            changeRecorder.record(userId, change.entityType(), change.entityId(), ChangeOperation.CREATE,
                    change.version(), null);
        }
    }

    /** Bù cho một người dùng trong transaction riêng. @return số bản ghi đã tạo */
    public int provisionMissing(UUID userId) {
        Integer created = tx.execute(status -> {
            List<PendingChange> pending = provision(userId);
            record(userId, pending);
            return pending.size();
        });
        return created == null ? 0 : created;
    }

    /**
     * Job bù idempotent (D113): duyệt mọi người dùng theo id, mỗi người một transaction. Lỗi trùng (đăng ký đồng thời
     * vừa tạo cùng dữ liệu) bỏ qua người đó. Log chỉ có số lượng.
     *
     * @return tổng số bản ghi đã tạo
     */
    public int backfill() {
        int total = 0;
        UUID after = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        List<UUID> page;
        do {
            page = users.idsAfter(after, Limit.of(PAGE));
            for (UUID userId : page) {
                try {
                    total += provisionMissing(userId);
                } catch (DataIntegrityViolationException e) {
                    log.debug("Default data already provisioned concurrently");
                }
            }
            if (!page.isEmpty()) {
                after = page.get(page.size() - 1);
            }
        } while (page.size() == PAGE);
        if (total > 0) {
            log.atInfo().addKeyValue("event", "user_provisioning_backfill").addKeyValue("created", total)
                    .log("user_provisioning_backfill");
        }
        return total;
    }
}
