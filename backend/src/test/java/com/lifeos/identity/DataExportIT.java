package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.common.privacy.UserDataExporter;
import com.lifeos.identity.service.DataExportService;
import com.lifeos.support.ApiTestBase;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * INT-12 (PHASE_7 D204): xuất dữ liệu bất đồng bộ — đủ bảng, không bí mật, CSV có BOM và chống injection, chỉ chủ sở
 * hữu tải, giới hạn 2/ngày, 5 lượt tải, hết hạn 24 giờ, vượt kích thước ⇒ EXPORT_TOO_LARGE.
 */
class DataExportIT extends ApiTestBase {

    @Autowired
    DataExportService exports;

    @Autowired
    UserDataExporter exporter;

    private UUID requestExport(TestUser user, int expectedStatus) throws Exception {
        MvcResult r = post(user, "/api/v1/account/export", null);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(expectedStatus);
        return UUID.fromString((String) readJson(r).get("id"));
    }

    private static Map<String, String> unzip(byte[] zip) throws Exception {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                entries.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    @Test
    @SuppressWarnings("unchecked")
    void Int_12_exportContainsTheUsersDataOnlyWithoutSecretsAndIsDownloadableByTheOwner() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID task = UUID.randomUUID();
        assertThat(post(user, "/api/v1/tasks", body("id", task.toString(), "title", "=HYPERLINK(\"x\")"))
                .getResponse().getStatus()).isEqualTo(201);
        assertThat(post(user, "/api/v1/notes", body("id", UUID.randomUUID().toString(), "content", "Ghi chú, riêng"))
                .getResponse().getStatus()).isEqualTo(201);
        assertThat(post(other, "/api/v1/tasks", body("id", UUID.randomUUID().toString(), "title", "Của người khác"))
                .getResponse().getStatus()).isEqualTo(201);
        UUID device = UUID.randomUUID();
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel', 'ANDROID')")
                .param("id", device).param("u", user.id()).update();
        jdbc.sql("INSERT INTO refresh_tokens (user_id, device_id, family_id, token_hash, family_started_at, expires_at)"
                + " VALUES (:u, :d, gen_random_uuid(), repeat('a', 64), now(), now() + interval '1 day')")
                .param("u", user.id()).param("d", device).update();

        UUID id = requestExport(user, 202);
        assertThat(requestExport(user, 200)).as("đang có yêu cầu chờ ⇒ trả yêu cầu đó").isEqualTo(id);
        assertThat(readJson(get(user, "/api/v1/account/export/" + id))).containsEntry("status", "PENDING");
        assertThat(get(user, "/api/v1/account/export/" + id + "/download").getResponse().getStatus())
                .as("chưa sẵn sàng").isEqualTo(409);
        assertThat(exports.runPending()).isGreaterThanOrEqualTo(1);
        Map<String, Object> ready = readJson(get(user, "/api/v1/account/export/" + id));
        assertThat(ready).containsEntry("status", "READY").containsEntry("downloadsRemaining", 5);
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'SYSTEM_NOTIFICATION'"
                + " AND aggregate_id = :u").param("u", user.id()).query(Integer.class).single()).isEqualTo(1);

        MvcResult download = get(user, "/api/v1/account/export/" + id + "/download");
        assertThat(download.getResponse().getStatus()).isEqualTo(200);
        assertThat(download.getResponse().getContentType()).isEqualTo("application/zip");
        Map<String, String> files = unzip(download.getResponse().getContentAsByteArray());
        String data = files.get("data.json");
        Map<String, Object> parsed = json.readValue(data, Map.class);
        Map<String, Object> tables = (Map<String, Object>) parsed.get("tables");
        assertThat(tables).containsKeys("tasks", "notes", "devices", "user_settings", "reviews")
                .doesNotContainKeys("refresh_tokens", "change_logs", "sync_mutations", "device_push_tokens");
        assertThat((Map<String, Object>) parsed.get("user")).containsEntry("id", user.id().toString())
                .doesNotContainKey("password_hash");
        assertThat(data).contains(task.toString()).doesNotContain("Của người khác").doesNotContain("search_vector")
                .doesNotContain("aaaaaaaaaaaaaaaa");
        String csv = files.get("csv/tasks.csv");
        assertThat(csv).startsWith("﻿").contains("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(files.get("csv/notes.csv")).contains("\"Ghi chú, riêng\"");
        assertThat(files).doesNotContainKey("csv/refresh_tokens.csv");

        assertThat(get(other, "/api/v1/account/export/" + id).getResponse().getStatus()).isEqualTo(404);
        assertThat(get(other, "/api/v1/account/export/" + id + "/download").getResponse().getStatus()).isEqualTo(404);
        for (int i = 0; i < 4; i++) {
            assertThat(get(user, "/api/v1/account/export/" + id + "/download").getResponse().getStatus())
                    .isEqualTo(200);
        }
        MvcResult sixth = get(user, "/api/v1/account/export/" + id + "/download");
        assertThat(sixth.getResponse().getStatus()).as("tối đa 5 lượt tải").isEqualTo(410);

        UUID second = requestExport(user, 202);
        exports.runPending();
        MvcResult third = post(user, "/api/v1/account/export", null);
        assertThat(third.getResponse().getStatus()).as("2 lần/24 giờ").isEqualTo(429);
        assertThat(readJson(third)).containsEntry("errorCode", "EXPORT_LIMIT_REACHED");

        jdbc.sql("UPDATE data_exports SET expires_at = :past WHERE id = :id")
                .param("past", OffsetDateTime.ofInstant(clock.instant().minusSeconds(60), ZoneOffset.UTC))
                .param("id", second).update();
        assertThat(readJson(get(user, "/api/v1/account/export/" + second + "/download"))).containsEntry("errorCode",
                "EXPORT_EXPIRED");
        String file = jdbc.sql("SELECT file_ref FROM data_exports WHERE id = :id").param("id", second)
                .query(String.class).single();
        assertThat(exports.expire()).isGreaterThanOrEqualTo(1);
        assertThat(Files.exists(Path.of(file))).as("tệp hết hạn bị xóa").isFalse();
        assertThat(readJson(get(user, "/api/v1/account/export/" + second))).containsEntry("status", "EXPIRED");
    }

    @Test
    void Int_12_exportOverTheSizeLimitFails() throws Exception {
        TestUser user = newUser();
        Path target = Files.createTempDirectory("lifeos-export-test").resolve("x.zip");
        assertThatThrownBy(() -> exporter.export(user.id(), target, 64, Instant.now()))
                .isInstanceOf(UserDataExporter.TooLargeException.class);
    }
}
