package com.lifeos.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import com.lifeos.task.api.TaskStatsApi;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Đợt sửa backend 1 (BACKEND_FIX_REPORT) F7: thống kê Task không được cắt im lặng ở {@link TaskStatsApi#MAX_ROWS} dòng
 * — vượt trần thì trả 422 ANALYTICS_RANGE_TOO_LARGE thay vì con số sai (D198, D208).
 */
class AnalyticsTruncationIT extends ApiTestBase {

    private void insertCompleted(UUID userId, int count) {
        OffsetDateTime at = clock.instant().atOffset(ZoneOffset.UTC);
        jdbc.sql("INSERT INTO tasks (id, user_id, title, status, completed_at, created_at)"
                        + " SELECT gen_random_uuid(), :u, 'Việc', 'DONE', :at, :at FROM generate_series(1, :n)")
                .param("u", userId).param("at", at).param("n", count).update();
    }

    @Test
    void F7_taskAnalyticsReportsAnErrorInsteadOfSilentlyTruncatingAtTheRowCap() throws Exception {
        TestUser user = newUser();
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
        String url = "/api/v1/analytics/tasks?from=" + today + "&to=" + today;
        try {
            insertCompleted(user.id(), TaskStatsApi.MAX_ROWS);
            MvcResult atCap = get(user, url);
            assertThat(atCap.getResponse().getStatus()).as("đúng bằng trần vẫn tính đủ").isEqualTo(200);
            assertThat(((Number) readJson(atCap).get("completed")).intValue()).isEqualTo(TaskStatsApi.MAX_ROWS);

            insertCompleted(user.id(), 1);
            MvcResult overCap = get(user, url);
            assertThat(overCap.getResponse().getStatus()).as(overCap.getResponse().getContentAsString())
                    .isEqualTo(422);
            assertThat(readJson(overCap)).containsEntry("errorCode", "ANALYTICS_RANGE_TOO_LARGE");
        } finally {
            // Dọn 200.001 dòng để không làm lệch thống kê bảng của các test kế hoạch truy vấn dùng chung DB.
            jdbc.sql("DELETE FROM tasks WHERE user_id = :u").param("u", user.id()).update();
        }
    }
}
