package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.support.LockOrderGuard;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SYN-48 (D86) phần động: {@link LockOrderGuard} bọc DataSource của context test chung, nên mọi IT dùng context đó đều
 * bị kiểm. Test này chứng minh (1) guard thật sự đỏ khi có khóa/ghi nghiệp vụ sau ChangeRecorder.record, (2) ghi
 * change_logs/sync_mutations sau đó vẫn hợp lệ, (3) ba đường từng vi phạm ở Giai đoạn 2 — xóa Project, xóa Category
 * (cả qua sync), đăng ký — nay đi qua guard mà không vi phạm.
 */
class LockOrderGuardIT extends SyncTestBase {

    @Autowired
    ChangeRecorder changeRecorder;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void Sync_48_guardTripsOnBusinessWriteOrLockingSelectAfterRecord() {
        TestUser user = newUser();
        UUID project = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            changeRecorder.record(user.id(), "project", project, ChangeOperation.CREATE, 1, null);
            jdbc.sql("UPDATE projects SET name = 'x' WHERE id = :id").param("id", project).update();
        })).isInstanceOf(LockOrderGuard.Violation.class).hasMessageContaining("D86");

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            changeRecorder.record(user.id(), "project", project, ChangeOperation.CREATE, 1, null);
            jdbc.sql("SELECT 1 FROM projects WHERE id = :id FOR SHARE").param("id", project).query().listOfRows();
        })).isInstanceOf(LockOrderGuard.Violation.class);

        tx.executeWithoutResult(status -> {
            changeRecorder.record(user.id(), "project", project, ChangeOperation.CREATE, 1, null);
            changeRecorder.record(user.id(), "project", project, ChangeOperation.UPDATE, 2, null);
            jdbc.sql("SELECT count(*) FROM projects WHERE id = :id").param("id", project).query(Integer.class).single();
        });
    }

    @Test
    void Sync_48_cascadeDeletesAndRegistrationFollowTheLockOrder() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        UUID category = createCategory(user);
        for (int i = 0; i < 3; i++) {
            UUID task = UUID.randomUUID();
            assertThat(post(user, "/api/v1/tasks", body("id", task.toString(), "title", "T" + i,
                    "projectId", project.toString(), "categoryId", category.toString()))
                    .getResponse().getStatus()).isEqualTo(201);
        }
        int before = LockOrderGuard.violations().size();

        assertThat(pushOne(user, mutation("project", project, "DELETE", 1, null))).containsEntry("status", "APPLIED");
        assertThat(delete(user, "/api/v1/categories/" + category + "?version=1").getResponse().getStatus())
                .isEqualTo(204);
        MvcResult registered = mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register").with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("email", UUID.randomUUID() + "@example.test", "password", "mat-khau-dai-123",
                        "displayName", "Guard"))).andReturn();
        assertThat(registered.getResponse().getStatus()).isEqualTo(202);

        assertThat(LockOrderGuard.violations()).hasSize(before);
        // Thứ tự seq giữ nguyên: change_logs Task (mutation_id NULL) trước DELETE của Project.
        assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type = 'task'"
                        + " AND operation = 'UPDATE' AND mutation_id IS NULL"
                        + " AND seq < (SELECT seq FROM change_logs WHERE user_id = :u AND entity_id = :p"
                        + " AND operation = 'DELETE')")
                .param("u", user.id()).param("p", project).query(Integer.class).single()).isEqualTo(3);
    }
}
