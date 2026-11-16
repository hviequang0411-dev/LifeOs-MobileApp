package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.identity.service.OpaqueTokens;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-01 Đăng ký (FR-ID-01, D76): tạo users + user_settings (change_logs CREATE v1, user_sync_state lười) +
 * token xác minh + outbox trong MỘT transaction; chính sách mật khẩu; timezone IANA; chuẩn hóa email; trường lạ;
 * JPA auditing theo Clock; @Version bắt đầu từ 1.
 */
class RegistrationIT extends IdentityIntegrationTest {

    @Test
    void registerCreatesAccountSettingsChangeLogAndVerificationEmailInOneTransaction() throws Exception {
        clock.advance(Duration.ofMinutes(1));
        Instant now = clock.instant();
        String email = newEmail();

        MvcResult result = register(email, PASSWORD, "Europe/Paris");

        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        UUID userId = userId(email).orElseThrow();

        // users: chưa xác minh, mật khẩu chỉ ở dạng Argon2id; created_at/updated_at do JPA auditing + Clock đặt.
        Map<String, Object> user = userRow(userId);
        assertThat(user.get("email")).isEqualTo(email);
        assertThat(user.get("display_name")).isEqualTo("Người Test");
        assertThat(user.get("email_verified_at")).isNull();
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat((String) user.get("password_hash")).startsWith("$argon2id$").doesNotContain(PASSWORD);
        assertThat(instant(user.get("created_at"))).isEqualTo(now);
        assertThat(instant(user.get("updated_at"))).isEqualTo(now);

        // user_settings: mặc định + version bắt đầu từ 1.
        Map<String, Object> settings = jdbc.sql("SELECT * FROM user_settings WHERE user_id = :u")
                .param("u", userId).query().singleRow();
        assertThat(settings.get("timezone")).isEqualTo("Europe/Paris");
        assertThat(settings.get("locale")).isEqualTo("vi-VN");
        assertThat(settings.get("default_currency")).isEqualTo("VND");
        assertThat(((Number) settings.get("week_start_day")).intValue()).isEqualTo(1);
        assertThat(settings.get("version")).isEqualTo(1L);
        assertThat(settings.get("deleted_at")).isNull();
        assertThat(instant(settings.get("created_at"))).isEqualTo(now);

        // change_logs: đúng một dòng user_settings CREATE v1 (seq 1), mutationId null; user_sync_state tạo lười (D21).
        // Từ Phase 3 (D113) đăng ký còn tạo 15 danh mục thu/chi mặc định, ghi change_logs SAU user_settings (D86).
        List<Map<String, Object>> changes = jdbc.sql("SELECT * FROM change_logs WHERE user_id = :u"
                        + " AND entity_type = 'user_settings'")
                .param("u", userId).query().listOfRows();
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).get("seq")).isEqualTo(1L);
        assertThat(changes.get(0).get("entity_type")).isEqualTo("user_settings");
        assertThat(changes.get(0).get("entity_id")).isEqualTo(settings.get("id"));
        assertThat(changes.get(0).get("operation")).isEqualTo("CREATE");
        assertThat(changes.get(0).get("entity_version")).isEqualTo(1L);
        assertThat(changes.get(0).get("mutation_id")).isNull();
        assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type ="
                + " 'financial_category' AND operation = 'CREATE' AND mutation_id IS NULL AND seq > 1")
                .param("u", userId).query(Integer.class).single()).isEqualTo(15);
        assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single()).isEqualTo(16);
        assertThat(jdbc.sql("SELECT last_seq FROM user_sync_state WHERE user_id = :u").param("u", userId)
                .query(Long.class).single()).isEqualTo(16L);

        // Token xác minh: chỉ hash trong DB, hạn 24 giờ; token thô chỉ nằm trong payload outbox.
        Map<String, Object> token = jdbc.sql("SELECT * FROM email_verification_tokens WHERE user_id = :u")
                .param("u", userId).query().singleRow();
        assertThat(token.get("used_at")).isNull();
        assertThat(instant(token.get("expires_at"))).isEqualTo(now.plus(Duration.ofHours(24)));
        Map<String, Object> event = jdbc.sql("SELECT event_type, status, payload::text AS payload"
                        + " FROM outbox_events WHERE aggregate_id = :u")
                .param("u", userId).query().singleRow();
        assertThat(event.get("event_type")).isEqualTo("EMAIL_VERIFICATION");
        assertThat(event.get("status")).isEqualTo("PENDING");
        String raw = latestVerificationToken(userId);
        assertThat(((String) token.get("token_hash"))).isEqualTo(OpaqueTokens.sha256Hex(raw)).isNotEqualTo(raw);
        assertThat((String) event.get("payload")).contains(email).contains(userId.toString())
                .doesNotContain(PASSWORD);
    }

    @Test
    void timezoneDefaultsToHoChiMinhWhenMissing() throws Exception {
        String email = newEmail();
        assertThat(register(email).getResponse().getStatus()).isEqualTo(202);
        UUID userId = userId(email).orElseThrow();
        assertThat(jdbc.sql("SELECT timezone FROM user_settings WHERE user_id = :u").param("u", userId)
                .query(String.class).single()).isEqualTo("Asia/Ho_Chi_Minh");
    }

    @Test
    void nonIanaTimezoneIsRejectedAndNothingIsCreated() throws Exception {
        for (String timezone : List.of("Mars/Phobos", "+07:00", "UTC+7", "GMT+07:00")) {
            String email = newEmail();
            MvcResult result = register(email, PASSWORD, timezone);

            assertThat(result.getResponse().getStatus()).as(timezone).isEqualTo(400);
            String bodyText = result.getResponse().getContentAsString();
            assertThat(bodyText).contains("\"errorCode\":\"VALIDATION_FAILED\"")
                    .contains("\"field\":\"timezone\"").contains("\"code\":\"INVALID_VALUE\"");
            assertThat(countUsers(email)).isZero();
        }
    }

    @Test
    void passwordOutside8To128CharactersIsTooWeak() throws Exception {
        for (String password : List.of("1234567", "x".repeat(129), "")) {
            String email = newEmail();
            MvcResult result = register(email, password, null);

            assertThat(result.getResponse().getStatus()).isEqualTo(400);
            assertThat(result.getResponse().getContentAsString())
                    .contains("\"errorCode\":\"AUTH_PASSWORD_TOO_WEAK\"")
                    .contains("\"field\":\"password\"").contains("\"code\":\"SIZE\"");
            assertThat(countUsers(email)).isZero();
        }
        assertThat(register(newEmail(), "12345678", null).getResponse().getStatus()).isEqualTo(202);
        assertThat(register(newEmail(), "y".repeat(128), null).getResponse().getStatus()).isEqualTo(202);
    }

    @Test
    void passwordLengthIsMeasuredAfterNfkc() throws Exception {
        // "abcdeﬀg": 7 code point; NFKC tách "ﬀ" thành "ff" → 8 ký tự → hợp lệ.
        String email = newEmail();
        assertThat(register(email, "abcdeﬀg", null).getResponse().getStatus()).isEqualTo(202);
        // "ﬀﬀﬀ": 3 code point → 6 sau NFKC → vẫn quá ngắn.
        MvcResult tooShort = register(newEmail(), "ﬀﬀﬀ", null);
        assertThat(tooShort.getResponse().getContentAsString()).contains("AUTH_PASSWORD_TOO_WEAK");
    }

    @Test
    void malformedEmailOrBlankDisplayNameIsValidationFailed() throws Exception {
        MvcResult badEmail = postJson("/api/v1/auth/register",
                body("email", "not-an-email", "password", PASSWORD, "displayName", "A"), ip);
        assertThat(badEmail.getResponse().getStatus()).isEqualTo(400);
        assertThat(badEmail.getResponse().getContentAsString()).contains("\"errorCode\":\"VALIDATION_FAILED\"")
                .contains("\"field\":\"email\"");

        MvcResult blankName = postJson("/api/v1/auth/register",
                body("email", newEmail(), "password", PASSWORD, "displayName", "   "), ip);
        assertThat(blankName.getResponse().getStatus()).isEqualTo(400);
        assertThat(blankName.getResponse().getContentAsString()).contains("\"field\":\"displayName\"")
                .contains("\"code\":\"REQUIRED\"");

        MvcResult longName = postJson("/api/v1/auth/register",
                body("email", newEmail(), "password", PASSWORD, "displayName", "n".repeat(101)), ip);
        assertThat(longName.getResponse().getContentAsString()).contains("\"field\":\"displayName\"")
                .contains("\"code\":\"SIZE\"");
    }

    @Test
    void unknownFieldsOnAuthEndpointsAreRejectedAsMalformed() throws Exception {
        String email = newEmail();
        MvcResult register = postJson("/api/v1/auth/register", body("email", email, "password", PASSWORD,
                "displayName", "A", "userId", UUID.randomUUID().toString()), ip);
        assertThat(register.getResponse().getStatus()).isEqualTo(400);
        assertThat(register.getResponse().getContentAsString()).contains("\"errorCode\":\"MALFORMED_REQUEST\"");
        assertThat(countUsers(email)).isZero();

        assertThat(postJson("/api/v1/auth/verify-email", body("token", "x", "admin", true), ip)
                .getResponse().getContentAsString()).contains("\"errorCode\":\"MALFORMED_REQUEST\"");
        assertThat(postJson("/api/v1/auth/resend-verification", body("email", email, "extra", 1), ip)
                .getResponse().getContentAsString()).contains("\"errorCode\":\"MALFORMED_REQUEST\"");
    }

    @Test
    void emailIsStoredNormalizedAndUniqueIgnoringCase() throws Exception {
        String local = "Mixed-" + UUID.randomUUID();
        String messy = "  " + local + "@Example.TEST ";

        assertThat(register(messy).getResponse().getStatus()).isEqualTo(202);
        UUID userId = userId(messy).orElseThrow();
        assertThat(userRow(userId).get("email")).isEqualTo(local.toLowerCase() + "@example.test");

        assertThat(register(local.toUpperCase() + "@EXAMPLE.test").getResponse().getStatus()).isEqualTo(202);
        assertThat(countUsers(messy)).isEqualTo(1);
    }

    @Test
    void auditingUpdatesUpdatedAtFromClockButKeepsCreatedAt() throws Exception {
        String email = newEmail();
        clock.advance(Duration.ofSeconds(5));
        Instant createdAt = clock.instant();
        register(email);
        UUID userId = userId(email).orElseThrow();

        clock.advance(Duration.ofMinutes(30));
        Instant verifiedAt = clock.instant();
        assertThat(verifyEmail(latestVerificationToken(userId)).getResponse().getStatus()).isEqualTo(204);

        Map<String, Object> user = userRow(userId);
        assertThat(instant(user.get("created_at"))).isEqualTo(createdAt);
        assertThat(instant(user.get("updated_at"))).isEqualTo(verifiedAt);
        assertThat(instant(user.get("email_verified_at"))).isEqualTo(verifiedAt);
    }

    static Instant instant(Object timestamptz) {
        if (timestamptz instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (timestamptz instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        throw new IllegalArgumentException("Không đọc được thời điểm: " + timestamptz);
    }
}
