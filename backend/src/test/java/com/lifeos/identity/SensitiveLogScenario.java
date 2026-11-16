package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import static org.awaitility.Awaitility.await;

import com.lifeos.common.security.JwtService;
import com.lifeos.identity.service.RegisterCommand;
import com.lifeos.support.TestUsers;
import com.lifeos.identity.service.RegistrationService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * LOG-01 / SEC-11 (SECURITY_RULES §11.1): chạy toàn bộ luồng auth nhạy cảm rồi quét log đã ghi — không được chứa mật
 * khẩu, access/refresh token, token xác minh email hay token đặt lại mật khẩu. Mỗi bí mật được tìm ở nhiều dạng
 * (nguyên văn, đã escape JSON, đã escape \\uXXXX) để quét đúng cả log dạng chữ lẫn log JSON. Dùng bởi SensitiveLogIT.
 */
abstract class SensitiveLogScenario extends IdentityIntegrationTest {

    /** Có ký tự mà JSON phải escape (" và \) và ký tự ngoài ASCII. */
    static final String PASSWORD_1 = "Mk\"đặc\\biệt-111";
    static final String PASSWORD_2 = "Mk\"đặc\\biệt-222";
    static final String PASSWORD_3 = "Mk\"đặc\\biệt-333";
    static final String WRONG_PASSWORD = "Sai\"mật\\khẩu-999";

    @Autowired
    RegistrationService registrationService;

    @Autowired
    DataSource dataSource;

    /** Mọi địa chỉ email mà luồng đã dùng: là PII, không được xuất hiện trong log (CODE_GUIDELINES §14). */
    final List<String> emailsUsed = new ArrayList<>();

    /** Chạy các luồng và trả về mọi bí mật đã xuất hiện trong request/response. */
    List<String> runSensitiveFlows() throws Exception {
        List<String> secrets = new ArrayList<>(List.of(PASSWORD_1, PASSWORD_2, PASSWORD_3, WRONG_PASSWORD));
        String email = newEmail();
        emailsUsed.add(email);

        assertThat(register(email, PASSWORD_1, null).getResponse().getStatus()).isEqualTo(202);
        UUID userId = userId(email).orElseThrow();
        String verification = latestVerificationToken(userId);
        secrets.add(verification);
        assertThat(verifyEmail(verification).getResponse().getStatus()).isEqualTo(204);

        assertThat(login(email, WRONG_PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(401);
        MvcResult loginResult = login(email, PASSWORD_1, null, ip);
        assertThat(loginResult.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> session = readJson(loginResult);
        secrets.add(str(session, "accessToken"));
        secrets.add(str(session, "refreshToken"));

        Map<String, Object> rotated = readJson(refresh(str(session, "refreshToken")));
        secrets.add(str(rotated, "accessToken"));
        secrets.add(str(rotated, "refreshToken"));
        // Dùng lại refresh token cũ: kích hoạt sự kiện REFRESH_TOKEN_REUSED (có log), không được in token.
        assertThat(refresh(str(session, "refreshToken")).getResponse().getStatus()).isEqualTo(401);

        Map<String, Object> second = readJson(login(email, PASSWORD_1, null, ip));
        secrets.add(str(second, "accessToken"));
        secrets.add(str(second, "refreshToken"));
        assertThat(getDevices(str(second, "accessToken")).getResponse().getStatus()).isEqualTo(200);
        assertThat(sendAuthed("POST", "/api/v1/me/change-password",
                body("currentPassword", PASSWORD_1, "newPassword", PASSWORD_2), str(second, "accessToken"))
                .getResponse().getStatus()).isEqualTo(204);

        assertThat(forgot(email).getResponse().getStatus()).isEqualTo(202);
        String reset = activeResetToken(userId);
        secrets.add(reset);
        assertThat(resetPassword(reset, PASSWORD_3).getResponse().getStatus()).isEqualTo(204);

        Map<String, Object> third = readJson(login(email, PASSWORD_3, null, ip));
        secrets.add(str(third, "accessToken"));
        secrets.add(str(third, "refreshToken"));
        assertThat(logout(str(third, "refreshToken")).getResponse().getStatus()).isEqualTo(204);
        return secrets;
    }

    /**
     * Hai đăng ký cùng email chạy chồng nhau, theo cách XÁC ĐỊNH được (không phụ thuộc may rủi của luồng): một kết nối
     * JDBC khác chèn user cùng email và giữ chưa commit; đăng ký thật không thấy bản ghi đó (READ COMMITTED), băm mật
     * khẩu, rồi bị chặn ở chỉ mục ux_users_email_lower; khi kết nối kia commit, lệnh chèn vi phạm unique — đúng đường
     * D76 từng khiến Hibernate ghi email vào log WARN. Đăng ký vẫn phải kết thúc bình thường.
     */
    void runConcurrentDuplicateRegistration() throws Exception {
        String email = newEmail();
        emailsUsed.add(email);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (PreparedStatement ps = competitor.prepareStatement(
                    "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'x', 'Đối thủ')")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setString(2, email);
                ps.executeUpdate();
            }
            Future<?> registration = pool.submit(() -> {
                registrationService.register(new RegisterCommand(email, PASSWORD_1, "Người Test", null, ip));
                return null;
            });
            await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.sql("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE wait_event_type = 'Lock' AND query ILIKE 'insert into users%'
                    """).query(Integer.class).single() > 0);
            competitor.commit();
            registration.get(15, TimeUnit.SECONDS); // ném lỗi nếu đăng ký không kết thúc bình thường
        } finally {
            pool.shutdownNow();
        }
        assertThat(countUsers(email)).isEqualTo(1);
    }

    @Autowired
    JwtService jwtService;

    /**
     * Q18 / SECURITY_RULES §11.1: name/description của dữ liệu người dùng không được log. Chạy đủ các thao tác Project
     * (tạo, sửa, đọc, liệt kê, lưu trữ, xóa) với nội dung đặc trưng; trả về các chuỗi nội dung đó để quét log.
     */
    List<String> runProjectFlows() throws Exception {
        UUID userId = TestUsers.insert(jdbc);
        String access = jwtService.issue(userId, UUID.randomUUID()).token();
        String name = "Dự-án-bí-mật-" + UUID.randomUUID();
        String renamed = "Tên-mới-bí-mật-" + UUID.randomUUID();
        String description = "Mô tả riêng tư " + UUID.randomUUID();
        UUID id = UUID.randomUUID();
        String path = "/api/v1/projects/" + id;

        assertThat(sendAuthed("POST", "/api/v1/projects",
                body("id", id.toString(), "name", name, "description", description), access)
                .getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("PATCH", path, body("version", 1, "name", renamed), access)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed(path, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/projects?archived=all", access).getResponse().getStatus()).isEqualTo(200);
        assertThat(sendAuthed("POST", path + "/archive", body("version", 2), access)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete(path + "?version=3"), access).getResponse().getStatus()).isEqualTo(204);

        // Category: tạo, trùng tên (409), đổi tên, đọc, liệt kê, xóa.
        String categoryName = "DM-bí-mật-" + UUID.randomUUID().toString().substring(0, 18);
        String categoryRenamed = "DM-mới-bí-mật-" + UUID.randomUUID().toString().substring(0, 18);
        UUID categoryId = UUID.randomUUID();
        String categoryPath = "/api/v1/categories/" + categoryId;
        assertThat(sendAuthed("POST", "/api/v1/categories", body("id", categoryId.toString(), "name", categoryName),
                access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/categories", body("id", UUID.randomUUID().toString(),
                "name", categoryName.toUpperCase()), access).getResponse().getStatus()).isEqualTo(409);
        assertThat(sendAuthed("PATCH", categoryPath, body("version", 1, "name", categoryRenamed), access)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed(categoryPath, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/categories", access).getResponse().getStatus()).isEqualTo(200);
        assertThat(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete(categoryPath + "?version=2"), access).getResponse().getStatus()).isEqualTo(204);
        // Task: tạo, sửa tiêu đề/mô tả, hoàn thành, đọc, xóa.
        String taskTitle = "Việc-bí-mật-" + UUID.randomUUID();
        String taskRetitled = "Việc-mới-bí-mật-" + UUID.randomUUID();
        String taskDescription = "Ghi chú riêng tư " + UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        String taskPath = "/api/v1/tasks/" + taskId;
        assertThat(sendAuthed("POST", "/api/v1/tasks", body("id", taskId.toString(), "title", taskTitle,
                "description", taskDescription), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("PATCH", taskPath, body("version", 1, "title", taskRetitled), access)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(sendAuthed("POST", taskPath + "/complete", body("version", 2), access)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed(taskPath, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete(taskPath + "?version=3"), access).getResponse().getStatus()).isEqualTo(204);
        return List.of(name, renamed, description, categoryName, categoryName.toUpperCase(), categoryRenamed,
                taskTitle, taskRetitled, taskDescription);
    }

    /**
     * SYN-47 (SYNC_PROTOCOL §7.2): luồng sync với nội dung đặc trưng — push đủ 4 entityType (CREATE/UPDATE/DELETE),
     * CONFLICT, REJECTED (trùng tên có nội dung), DUPLICATE, mutationId tái dùng (log sync_mutation_id_reuse), payload
     * sai kiểu, lỗi cấu trúc, rồi pull. Trả về nội dung + access token để quét log.
     */
    List<String> runSyncFlows() throws Exception {
        UUID userId = TestUsers.insert(jdbc);
        UUID settingsId = UUID.randomUUID();
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, 'Asia/Ho_Chi_Minh')")
                .param("id", settingsId).param("u", userId).update();
        String access = jwtService.issue(userId, UUID.randomUUID()).token();
        String projectName = "Sync-dự-án-bí-mật-" + UUID.randomUUID();
        String projectDescription = "Sync mô tả riêng tư " + UUID.randomUUID();
        String categoryName = "Sync-DM-" + UUID.randomUUID().toString().substring(0, 18);
        String taskTitle = "Sync-việc-bí-mật-" + UUID.randomUUID();
        String taskDescription = "Sync ghi chú riêng tư " + UUID.randomUUID();
        String conflictTitle = "Sync-xung-đột-" + UUID.randomUUID();
        UUID project = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        UUID task = UUID.randomUUID();
        UUID reused = UUID.randomUUID();

        String batch = body("mutations", List.of(
                syncMutation(reused, "project", project, "CREATE", 0,
                        Map.of("name", projectName, "description", projectDescription)),
                syncMutation(UUID.randomUUID(), "category", category, "CREATE", 0, Map.of("name", categoryName)),
                syncMutation(UUID.randomUUID(), "task", task, "CREATE", 0, Map.of("title", taskTitle,
                        "description", taskDescription, "projectId", project.toString())),
                syncMutation(UUID.randomUUID(), "task", task, "UPDATE", 9, Map.of("title", conflictTitle)),
                syncMutation(UUID.randomUUID(), "category", UUID.randomUUID(), "CREATE", 0,
                        Map.of("name", categoryName.toUpperCase())),
                syncMutation(UUID.randomUUID(), "task", UUID.randomUUID(), "CREATE", 0,
                        Map.of("title", Map.of("lồng", taskTitle))),
                syncMutation(UUID.randomUUID(), "user_settings", settingsId, "UPDATE", 1,
                        Map.of("timezone", "Asia/Tokyo")),
                syncMutation(UUID.randomUUID(), "habit", UUID.randomUUID(), "CREATE", 0, Map.of("name", projectName))));
        assertThat(sendAuthed("POST", "/api/v1/sync/push", batch, access).getResponse().getStatus()).isEqualTo(200);
        // Gửi lại cả lô (DUPLICATE) và tái dùng mutationId với metadata khác (log sync_mutation_id_reuse).
        assertThat(sendAuthed("POST", "/api/v1/sync/push", batch, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(sendAuthed("POST", "/api/v1/sync/push", body("mutations", List.of(syncMutation(reused, "category",
                UUID.randomUUID(), "CREATE", 0, Map.of("name", categoryName)))), access).getResponse().getStatus())
                .isEqualTo(200);
        // Lỗi cấu trúc toàn request có payload chứa nội dung.
        Map<String, Object> malformed = syncMutation(UUID.randomUUID(), "task", UUID.randomUUID(), "CREATE", 0,
                Map.of("title", taskTitle));
        malformed.put("operation", "UPSERT");
        assertThat(sendAuthed("POST", "/api/v1/sync/push", body("mutations", List.of(malformed)), access)
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(sendAuthed("POST", "/api/v1/sync/push", body("mutations", List.of(syncMutation(UUID.randomUUID(),
                "project", project, "DELETE", 1, null))), access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/sync/pull", access).getResponse().getStatus()).isEqualTo(200);
        return List.of(projectName, projectDescription, categoryName, categoryName.toUpperCase(), taskTitle,
                taskDescription, conflictTitle, access);
    }

    private static Map<String, Object> syncMutation(UUID mutationId, String entityType, UUID entityId,
            String operation, long baseVersion, Map<String, Object> payload) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("mutationId", mutationId.toString());
        m.put("entityType", entityType);
        m.put("entityId", entityId.toString());
        m.put("operation", operation);
        m.put("baseVersion", baseVersion);
        if (payload != null) {
            m.put("payload", payload);
        }
        return m;
    }

    /**
     * D253 / HAB-11: tên habit/routine, ghi chú check-in, tiêu đề bước không vào log — REST và sync, kể cả lỗi.
     */
    List<String> runHabitRoutineFlows() throws Exception {
        UUID userId = TestUsers.insert(jdbc);
        String access = jwtService.issue(userId, UUID.randomUUID()).token();
        String habitName = "Thói-quen-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String habitDescription = "Mô tả thói quen riêng " + UUID.randomUUID();
        String note = "Ghi-chú-riêng-" + UUID.randomUUID().toString().substring(0, 12);
        String routineName = "Routine-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String stepTitle = "Bước-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        UUID habit = UUID.randomUUID();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(clock.instant(),
                java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        assertThat(sendAuthed("POST", "/api/v1/habits", body("id", habit.toString(), "name", habitName,
                "description", habitDescription, "scheduleType", "DAILY",
                "startOn", today.minusDays(3).toString()), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("PUT", "/api/v1/habits/" + habit + "/checkins/" + today,
                body("status", "DONE", "note", note), access).getResponse().getStatus()).isIn(200, 201);
        assertThat(getAuthed("/api/v1/habits/" + habit, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(sendAuthed("PATCH", "/api/v1/habits/" + habit, body("version", 1, "name", habitName + "!"),
                access).getResponse().getStatus()).isEqualTo(200);
        UUID routine = UUID.randomUUID();
        assertThat(sendAuthed("POST", "/api/v1/routines", body("id", routine.toString(), "name", routineName,
                "kind", "DAILY"), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/routines/" + routine + "/steps", body("id",
                UUID.randomUUID().toString(), "title", stepTitle), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(getAuthed("/api/v1/routines/" + routine, access).getResponse().getStatus()).isEqualTo(200);
        // Sync: CREATE trùng ngày (CONFLICT kèm snapshot có ghi chú), REJECTED có tên quá dài.
        String batch = body("mutations", List.of(
                syncMutation(UUID.randomUUID(), "habit_checkin", UUID.randomUUID(), "CREATE", 0, Map.of(
                        "habitId", habit.toString(), "localDate", today.toString(), "status", "SKIPPED",
                        "note", note)),
                syncMutation(UUID.randomUUID(), "habit", UUID.randomUUID(), "CREATE", 0, Map.of(
                        "name", habitName.repeat(10), "scheduleType", "DAILY")),
                syncMutation(UUID.randomUUID(), "routine_step", UUID.randomUUID(), "CREATE", 0, Map.of(
                        "routineId", routine.toString(), "title", stepTitle + "-2"))));
        assertThat(sendAuthed("POST", "/api/v1/sync/push", batch, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/sync/pull", access).getResponse().getStatus()).isEqualTo(200);
        return List.of(habitName, habitDescription, note, routineName, stepTitle);
    }

    /**
     * FIN-21 / PHASE_3 §9: số tiền, ghi chú, tên tài khoản/danh mục/mục tiêu không vào log, kể cả lỗi (SCALE, tham chiếu
     * sai, CONFLICT sync có snapshot chứa số tiền) và sự kiện ngưỡng ngân sách.
     */
    List<String> runFinanceFlows() throws Exception {
        UUID userId = TestUsers.insert(jdbc);
        String access = jwtService.issue(userId, UUID.randomUUID()).token();
        String accountName = "Tài-khoản-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String categoryName = "Danh-mục-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String goalName = "Mục-tiêu-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String note = "Ghi-chú-tiền-" + UUID.randomUUID().toString().substring(0, 12);
        String amount = "73198246";
        String opening = "51846392";
        java.time.LocalDate today = java.time.LocalDate.ofInstant(clock.instant(),
                java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID account = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        assertThat(sendAuthed("POST", "/api/v1/finance/accounts", body("id", account.toString(), "name", accountName,
                "accountType", "BANK", "currency", "VND", "openingBalance", opening), access).getResponse()
                .getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/finance/categories", body("id", category.toString(),
                "name", categoryName, "kind", "EXPENSE"), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/finance/budgets", body("id", UUID.randomUUID().toString(),
                "categoryId", category.toString(), "currency", "VND", "amount", "80000000"), access).getResponse()
                .getStatus()).isEqualTo(201);
        UUID tx = UUID.randomUUID();
        assertThat(sendAuthed("POST", "/api/v1/finance/transactions", body("id", tx.toString(),
                "accountId", account.toString(), "categoryId", category.toString(), "kind", "EXPENSE",
                "amount", amount, "occurredOn", today.toString(), "note", note), access).getResponse().getStatus())
                .isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/finance/transactions", body("id", UUID.randomUUID().toString(),
                "accountId", account.toString(), "kind", "EXPENSE", "amount", amount + ".5",
                "occurredOn", today.toString(), "note", note), access).getResponse().getStatus()).isEqualTo(400);
        assertThat(sendAuthed("POST", "/api/v1/finance/transactions", body("id", UUID.randomUUID().toString(),
                "accountId", UUID.randomUUID().toString(), "kind", "EXPENSE", "amount", amount,
                "occurredOn", today.toString(), "note", note), access).getResponse().getStatus()).isEqualTo(422);
        assertThat(getAuthed("/api/v1/finance/accounts/" + account, access).getResponse().getStatus())
                .isEqualTo(200);
        assertThat(getAuthed("/api/v1/finance/transactions?q=" + note, access).getResponse().getStatus())
                .isEqualTo(200);
        assertThat(sendAuthed("POST", "/api/v1/finance/saving-goals", body("id", UUID.randomUUID().toString(),
                "name", goalName, "currency", "VND", "targetAmount", amount), access).getResponse().getStatus())
                .isEqualTo(201);
        String batch = body("mutations", List.of(
                syncMutation(UUID.randomUUID(), "financial_transaction", tx, "UPDATE", 0, Map.of("amount", amount,
                        "note", note)),
                syncMutation(UUID.randomUUID(), "financial_account", UUID.randomUUID(), "CREATE", 0, Map.of(
                        "name", accountName, "accountType", "BANK", "currency", "VND"))));
        assertThat(sendAuthed("POST", "/api/v1/sync/push", batch, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/sync/pull", access).getResponse().getStatus()).isEqualTo(200);
        return List.of(accountName, categoryName, goalName, note, amount, opening);
    }

    /**
     * NTF-13 / PHASE_4 §8: tiêu đề/địa điểm sự kiện, token push (D147) không vào log — REST, sync, lỗi.
     */
    List<String> runCalendarNotificationFlows() throws Exception {
        UUID userId = TestUsers.insert(jdbc);
        String access = jwtService.issue(userId, UUID.randomUUID()).token();
        String title = "Sự-kiện-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String location = "Địa-điểm-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String pushToken = "fcm-bi-mat-" + UUID.randomUUID();
        UUID event = UUID.randomUUID();
        assertThat(sendAuthed("POST", "/api/v1/calendar/events", body("id", event.toString(), "title", title,
                "location", location, "timezone", "Asia/Ho_Chi_Minh", "startLocal", "2027-04-01T09:00:00",
                "endLocal", "2027-04-01T10:00:00", "recurrence", Map.of("freq", "WEEKLY", "count", 3)), access)
                .getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/calendar/events/" + event + "/exceptions", body("id",
                UUID.randomUUID().toString(), "originalStartLocal", "2027-04-08T09:00:00", "kind", "MODIFIED",
                "title", title + "-sửa", "location", location), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/reminders", body("id", UUID.randomUUID().toString(), "targetType",
                "EVENT", "targetId", event.toString(), "mode", "OFFSET", "offsetMinutes", 10), access).getResponse()
                .getStatus()).isEqualTo(201);
        UUID device = UUID.randomUUID();
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel', 'ANDROID')")
                .param("id", device).param("u", userId).update();
        assertThat(sendAuthed("PUT", "/api/v1/devices/" + device + "/push-token", body("token", pushToken,
                "platform", "ANDROID"), access).getResponse().getStatus()).isEqualTo(204);
        assertThat(sendAuthed("PUT", "/api/v1/devices/" + UUID.randomUUID() + "/push-token", body("token", pushToken,
                "platform", "ANDROID"), access).getResponse().getStatus()).isEqualTo(404);
        assertThat(getAuthed("/api/v1/devices", access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/calendar?from=2027-04-01&to=2027-04-30", access).getResponse().getStatus())
                .isEqualTo(200);
        String batch = body("mutations", List.of(
                syncMutation(UUID.randomUUID(), "calendar_event", event, "UPDATE", 0, Map.of("title", title,
                        "location", location)),
                syncMutation(UUID.randomUUID(), "calendar_event", UUID.randomUUID(), "CREATE", 0, Map.of(
                        "title", title, "timezone", "Không/Có", "startLocal", "2027-04-01T09:00:00",
                        "endLocal", "2027-04-01T10:00:00"))));
        assertThat(sendAuthed("POST", "/api/v1/sync/push", batch, access).getResponse().getStatus()).isEqualTo(200);
        return List.of(title, location, pushToken);
    }

    /** PHASE_6 KNW-16: tiêu đề/nội dung ghi chú, tên thẻ, tiêu đề Goal và truy vấn tìm kiếm không vào log. */
    List<String> runKnowledgeGoalFlows() throws Exception {
        UUID userId = TestUsers.insert(jdbc);
        String access = jwtService.issue(userId, UUID.randomUUID()).token();
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        String noteTitle = "Ghi-chú-bí-mật-" + suffix;
        String noteContent = "Nội-dung-bí-mật-" + suffix;
        String tagName = "Thẻ-mật-" + suffix.substring(0, 6);
        String goalTitle = "Mục-tiêu-bí-mật-" + suffix;
        String query = "truyvanmat" + suffix.substring(0, 8);
        UUID note = UUID.randomUUID();
        UUID tag = UUID.randomUUID();
        UUID goal = UUID.randomUUID();
        assertThat(sendAuthed("POST", "/api/v1/notes", body("id", note.toString(), "title", noteTitle, "content",
                noteContent), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/notes", body("id", UUID.randomUUID().toString(), "content",
                noteContent + "x".repeat(50_001)), access).getResponse().getStatus()).isEqualTo(400);
        assertThat(sendAuthed("POST", "/api/v1/tags", body("id", tag.toString(), "name", tagName), access)
                .getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/tags", body("id", UUID.randomUUID().toString(), "name", tagName),
                access).getResponse().getStatus()).isEqualTo(409);
        assertThat(sendAuthed("PUT", "/api/v1/notes/" + note + "/tags", body("tagIds", List.of(tag.toString())),
                access).getResponse().getStatus()).isEqualTo(200);
        assertThat(sendAuthed("POST", "/api/v1/goals", body("id", goal.toString(), "title", goalTitle), access)
                .getResponse().getStatus()).isEqualTo(201);
        assertThat(getAuthed("/api/v1/notes?q=" + query, access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/notes?q=" + query + "x".repeat(100), access).getResponse().getStatus())
                .isEqualTo(400);
        String batch = body("mutations", List.of(
                syncMutation(UUID.randomUUID(), "note", note, "UPDATE", 0, Map.of("title", noteTitle,
                        "content", noteContent)),
                syncMutation(UUID.randomUUID(), "tag", UUID.randomUUID(), "CREATE", 0, Map.of("name", tagName)),
                syncMutation(UUID.randomUUID(), "goal", goal, "UPDATE", 1, Map.of("title", goalTitle,
                        "status", "NHẦM"))));
        assertThat(sendAuthed("POST", "/api/v1/sync/push", batch, access).getResponse().getStatus()).isEqualTo(200);
        return List.of(noteTitle, noteContent, tagName, goalTitle, query);
    }

    /** PHASE_7 INT-18: phản tư, số tiền và mã xác nhận xóa tài khoản không vào log. */
    List<String> runAnalyticsAccountFlows() throws Exception {
        UUID userId = TestUsers.insert(jdbc);
        String access = jwtService.issue(userId, UUID.randomUUID()).token();
        String reflection = "Phản-tư-bí-mật-" + UUID.randomUUID().toString().substring(0, 12);
        String amount = "987654321";
        String today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toString();
        assertThat(sendAuthed("POST", "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType", "DAY",
                "periodKey", today, "reflection", reflection), access).getResponse().getStatus()).isIn(201, 409);
        assertThat(sendAuthed("POST", "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType", "DAY",
                "periodKey", today, "reflection", reflection + "x".repeat(5001)), access).getResponse().getStatus())
                .isEqualTo(400);
        UUID account = UUID.randomUUID();
        assertThat(sendAuthed("POST", "/api/v1/finance/accounts", body("id", account.toString(), "name", "Ví",
                "accountType", "CASH", "currency", "VND"), access).getResponse().getStatus()).isEqualTo(201);
        assertThat(sendAuthed("POST", "/api/v1/finance/transactions", body("id", UUID.randomUUID().toString(),
                "accountId", account.toString(), "kind", "INCOME", "amount", amount, "occurredOn", today), access)
                .getResponse().getStatus()).isEqualTo(201);
        assertThat(getAuthed("/api/v1/dashboard", access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/reviews/daily", access).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/analytics/finance?currency=VND&from=" + today + "&to=" + today, access)
                .getResponse().getStatus()).isEqualTo(200);
        MvcResult codeRequest = sendAuthed("POST", "/api/v1/account/deletion/code", "{}", access);
        assertThat(codeRequest.getResponse().getStatus()).as(codeRequest.getResponse().getContentAsString())
                .isEqualTo(202);
        String code = jdbc.sql("SELECT payload->>'token' FROM outbox_events WHERE event_type = 'ACCOUNT_DELETION_CODE'"
                + " AND aggregate_id = :u").param("u", userId).query(String.class).single();
        assertThat(sendAuthed("POST", "/api/v1/account/deletion", body("password", PASSWORD_1, "code", code), access)
                .getResponse().getStatus()).isEqualTo(400);
        String batch = body("mutations", List.of(syncMutation(UUID.randomUUID(), "review", UUID.randomUUID(), "CREATE",
                0, Map.of("periodType", "DAY", "periodKey", today, "reflection", reflection))));
        assertThat(sendAuthed("POST", "/api/v1/sync/push", batch, access).getResponse().getStatus()).isEqualTo(200);
        return List.of(reflection, amount, code);
    }

    /** Không địa chỉ email nào của luồng xuất hiện trong log (dạng nguyên văn hoặc chữ thường). */
    static void assertNoEmailIn(String log, List<String> emails, String label) {
        for (String email : emails) {
            assertThat(log).as("%s chứa địa chỉ email của người dùng", label)
                    .doesNotContainIgnoringCase(email);
        }
    }

    static void assertNoSecretIn(String log, List<String> secrets, String label) {
        assertThat(log).as(label + ": phải có nội dung để phép quét có ý nghĩa").contains("security_event");
        for (String secret : secrets) {
            for (String form : forms(secret)) {
                assertThat(log).as("%s chứa bí mật (bắt đầu bằng %s…)", label,
                        form.substring(0, Math.min(6, form.length()))).doesNotContain(form);
            }
        }
        assertThat(log).as(label).doesNotContain("Bearer ey");
    }

    /** Các dạng một bí mật có thể xuất hiện trong log chữ hoặc log JSON. */
    static Set<String> forms(String secret) {
        Set<String> forms = new LinkedHashSet<>();
        forms.add(secret);
        String jsonEscaped = secret.replace("\\", "\\\\").replace("\"", "\\\"");
        forms.add(jsonEscaped);
        StringBuilder unicodeEscaped = new StringBuilder();
        for (char c : jsonEscaped.toCharArray()) {
            if (c > 0x7E) {
                unicodeEscaped.append(String.format("\\u%04x", (int) c));
            } else {
                unicodeEscaped.append(c);
            }
        }
        forms.add(unicodeEscaped.toString());
        forms.add(unicodeEscaped.toString().toUpperCase().replace("\\U", "\\u"));
        return forms;
    }
}
