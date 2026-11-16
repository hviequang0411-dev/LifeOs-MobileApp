package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.privacy.UserDataCatalog;
import com.lifeos.common.security.PasswordHasher;
import com.lifeos.identity.service.AccountDeletionService;
import com.lifeos.support.ApiTestBase;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

/**
 * INT-14 (PHASE_7 D205; §4 kế hoạch): xóa tài khoản cần mật khẩu + mã email, cooldown 7 ngày hủy được, API thường bị
 * chặn 403 trong cooldown, refresh token bị thu hồi; job xóa cứng mọi bảng của danh mục (kiểm tự động theo
 * UserDataCatalog), không đụng dữ liệu người khác, nhật ký kiểm toán chỉ có băm userId.
 */
class AccountDeletionIT extends ApiTestBase {

    private static final String PASSWORD = "Mật-khẩu-xóa-123";

    @Autowired
    PasswordHasher hasher;

    @Autowired
    AccountDeletionService deletion;

    private TestUser userWithPassword() {
        TestUser user = newUser();
        jdbc.sql("UPDATE users SET password_hash = :h WHERE id = :u").param("h", hasher.hash(PASSWORD))
                .param("u", user.id()).update();
        return user;
    }

    private String requestCode(TestUser user) throws Exception {
        assertThat(post(user, "/api/v1/account/deletion/code", null).getResponse().getStatus()).isEqualTo(202);
        // Mã mới nhất = mã còn hiệu lực duy nhất (mã cũ bị vô hiệu); created_at theo đồng hồ test có thể trùng nhau.
        List<String> tokens = jdbc.sql("SELECT payload->>'token' FROM outbox_events WHERE event_type ="
                + " 'ACCOUNT_DELETION_CODE' AND aggregate_id = :u").param("u", user.id()).query(String.class).list();
        String hash = jdbc.sql("SELECT code_hash FROM account_deletion_codes WHERE user_id = :u AND used_at IS NULL")
                .param("u", user.id()).query(String.class).single();
        return tokens.stream().filter(t -> sha256(t).equals(hash)).findFirst().orElseThrow();
    }

    private MvcResult schedule(TestUser user, String password, String code) throws Exception {
        return post(user, "/api/v1/account/deletion", body("password", password, "code", code));
    }

    private void created(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isIn(200, 201, 202, 204);
    }

    @Test
    void Int_14_deletionNeedsPasswordAndEmailCodeAndCanBeCancelledDuringCooldown() throws Exception {
        TestUser user = userWithPassword();
        UUID device = UUID.randomUUID();
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel', 'ANDROID')")
                .param("id", device).param("u", user.id()).update();
        jdbc.sql("INSERT INTO refresh_tokens (user_id, device_id, family_id, token_hash, family_started_at, expires_at)"
                + " VALUES (:u, :d, gen_random_uuid(), repeat('b', 64), now(), now() + interval '1 day')")
                .param("u", user.id()).param("d", device).update();
        assertThat(get(user, "/api/v1/account/deletion").getResponse().getStatus()).isEqualTo(404);

        String code = requestCode(user);
        assertThat(code).matches("\\d{6}");
        assertThat(jdbc.sql("SELECT code_hash FROM account_deletion_codes WHERE user_id = :u").param("u", user.id())
                .query(String.class).single()).as("chỉ lưu băm").isNotEqualTo(code).hasSize(64);
        MvcResult wrongPassword = schedule(user, "sai-mat-khau", code);
        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(wrongPassword)).containsEntry("errorCode", "AUTH_CURRENT_PASSWORD_WRONG");
        MvcResult wrongCode = schedule(user, PASSWORD, code.equals("000000") ? "111111" : "000000");
        assertThat(wrongCode.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(wrongCode)).containsEntry("errorCode", "VALIDATION_FAILED");
        String stale = code;
        code = requestCode(user);
        if (!stale.equals(code)) {
            assertThat(schedule(user, PASSWORD, stale).getResponse().getStatus()).as("mã cũ bị vô hiệu").isEqualTo(400);
        }

        MvcResult scheduled = schedule(user, PASSWORD, code);
        assertThat(scheduled.getResponse().getStatus()).as(scheduled.getResponse().getContentAsString())
                .isEqualTo(200);
        Instant at = Instant.parse((String) readJson(scheduled).get("deletionScheduledAt"));
        assertThat(Duration.between(clock.instant(), at)).isEqualTo(Duration.ofDays(7));
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_tokens WHERE user_id = :u AND revoked_at IS NULL")
                .param("u", user.id()).query(Integer.class).single()).as("thu hồi mọi phiên").isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'ACCOUNT_DELETION_SCHEDULED'"
                + " AND aggregate_id = :u").param("u", user.id()).query(Integer.class).single()).isEqualTo(1);

        MvcResult blocked = get(user, "/api/v1/tasks");
        assertThat(blocked.getResponse().getStatus()).isEqualTo(403);
        assertThat(readJson(blocked)).containsEntry("errorCode", "ACCOUNT_DELETION_PENDING");
        assertThat(post(user, "/api/v1/account/export", null).getResponse().getStatus()).isEqualTo(403);
        assertThat(readJson(get(user, "/api/v1/account/deletion"))).containsEntry("cancellable", true);
        assertThat(delete(user, "/api/v1/account/deletion").getResponse().getStatus()).isEqualTo(204);
        assertThat(get(user, "/api/v1/tasks").getResponse().getStatus()).isEqualTo(200);
        assertThat(get(user, "/api/v1/account/deletion").getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(user, "/api/v1/account/deletion").getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void Int_14_purgeRemovesEveryCatalogTableRowOfTheUserAndNothingElse() throws Exception {
        TestUser user = userWithPassword();
        TestUser bystander = newUser();
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID task = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        created(post(user, "/api/v1/projects", body("id", project.toString(), "name", "Dự án")));
        created(post(user, "/api/v1/categories", body("id", UUID.randomUUID().toString(), "name", "DM xóa")));
        UUID goal = UUID.randomUUID();
        created(post(user, "/api/v1/goals", body("id", goal.toString(), "title", "Mục tiêu")));
        UUID milestone = UUID.randomUUID();
        created(post(user, "/api/v1/goals/" + goal + "/milestones", body("id", milestone.toString(), "title", "M")));
        created(post(user, "/api/v1/tasks", body("id", task.toString(), "title", "Việc", "projectId",
                project.toString(), "milestoneId", milestone.toString())));
        UUID note = UUID.randomUUID();
        UUID tag = UUID.randomUUID();
        created(post(user, "/api/v1/notes", body("id", note.toString(), "content", "n")));
        created(post(user, "/api/v1/tags", body("id", tag.toString(), "name", "Thẻ")));
        created(call(user, HttpMethod.PUT, "/api/v1/notes/" + note + "/tags", body("tagIds", List.of(tag.toString()))));
        created(post(user, "/api/v1/notes/" + note + "/links", body("id", UUID.randomUUID().toString(), "targetType",
                "TASK", "targetId", task.toString())));
        UUID habit = UUID.randomUUID();
        created(post(user, "/api/v1/habits", body("id", habit.toString(), "name", "H", "scheduleType", "DAILY",
                "startOn", today.minusDays(2).toString())));
        created(call(user, HttpMethod.PUT, "/api/v1/habits/" + habit + "/checkins/" + today,
                body("id", UUID.randomUUID().toString(), "status", "DONE")));
        UUID account = UUID.randomUUID();
        created(post(user, "/api/v1/finance/accounts", body("id", account.toString(), "name", "Ví", "accountType",
                "CASH", "currency", "VND")));
        created(post(user, "/api/v1/finance/transactions", body("id", UUID.randomUUID().toString(), "accountId",
                account.toString(), "kind", "EXPENSE", "amount", "1000", "occurredOn", today.toString())));
        UUID event = UUID.randomUUID();
        created(post(user, "/api/v1/calendar/events", body("id", event.toString(), "title", "E", "timezone",
                "Asia/Ho_Chi_Minh", "startLocal", today + "T09:00:00", "endLocal", today + "T10:00:00")));
        created(post(user, "/api/v1/reminders", body("id", UUID.randomUUID().toString(), "targetType", "TASK",
                "targetId", task.toString(), "mode", "LOCAL_AT", "localTime", "07:00", "localDate",
                today.plusDays(1).toString())));
        created(post(user, "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType", "DAY",
                "periodKey", today.toString(), "reflection", "r")));
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, 'Asia/Ho_Chi_Minh')")
                .param("id", UUID.randomUUID()).param("u", user.id()).update();
        created(post(user, "/api/v1/account/export", null));
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel', 'ANDROID')")
                .param("id", UUID.randomUUID()).param("u", user.id()).update();
        String code = requestCode(user);
        created(schedule(user, PASSWORD, code));
        created(post(bystander, "/api/v1/tasks", body("id", UUID.randomUUID().toString(), "title", "Còn lại")));

        List<String> populated = new ArrayList<>();
        for (String table : UserDataCatalog.TABLES) {
            if (count(table, user.id()) > 0) {
                populated.add(table);
            }
        }
        assertThat(populated).as("dữ liệu trải rộng nhiều module").hasSizeGreaterThanOrEqualTo(20);

        jdbc.sql("UPDATE users SET deletion_scheduled_at = :at WHERE id = :u")
                .param("at", OffsetDateTime.ofInstant(clock.instant().minusSeconds(60), ZoneOffset.UTC))
                .param("u", user.id()).update();
        assertThat(deletion.purgeDue()).isGreaterThanOrEqualTo(1);

        for (String table : UserDataCatalog.TABLES) {
            assertThat(count(table, user.id())).as(table + " còn dòng của user đã xóa").isZero();
        }
        assertThat(jdbc.sql("SELECT count(*) FROM users WHERE id = :u").param("u", user.id()).query(Integer.class)
                .single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE aggregate_id = :u").param("u", user.id())
                .query(Integer.class).single()).isZero();
        assertThat(count("tasks", bystander.id())).as("không đụng người khác").isEqualTo(1);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(user.id().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Map<String, Object> audit = jdbc.sql("SELECT user_hash, table_rows FROM account_deletion_audit WHERE user_hash"
                + " = :h").param("h", hash).query().singleRow();
        assertThat(((Number) audit.get("table_rows")).intValue()).isGreaterThan(20);
        assertThat(jdbc.sql("SELECT count(*) FROM account_deletion_audit WHERE user_hash = :raw")
                .param("raw", user.id().toString()).query(Integer.class).single()).as("không lưu userId thô").isZero();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private int count(String table, UUID userId) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single();
    }
}
