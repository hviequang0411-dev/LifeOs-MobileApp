package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import com.lifeos.identity.service.IdentityOutboxEvents;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-01 bước 4 / PRD §6: users + user_settings + change_logs + user_sync_state + token xác minh + outbox nằm trong
 * MỘT transaction. Cho bước ghi outbox (bước cuối) thất bại: không được còn dòng nào của các bước trước.
 */
class RegistrationAtomicityIT extends IdentityIntegrationTest {

    @MockitoSpyBean
    IdentityOutboxEvents outboxEvents;

    @Test
    void failureWhileWritingOutboxRollsBackTheWholeRegistration() throws Exception {
        String email = newEmail();
        doThrow(new IllegalStateException("outbox giả lập lỗi"))
                .when(outboxEvents).emailVerification(any(), eq(email), anyString(), anyString(), any());

        MvcResult result = register(email);

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"INTERNAL_ERROR\"")
                .doesNotContain("outbox giả lập lỗi");
        assertThat(countUsers(email)).isZero();
        assertThat(jdbc.sql("""
                        SELECT (SELECT count(*) FROM user_settings s JOIN users u ON u.id = s.user_id
                                 WHERE lower(u.email) = :e)
                             + (SELECT count(*) FROM email_verification_tokens t JOIN users u ON u.id = t.user_id
                                 WHERE lower(u.email) = :e)
                        """).param("e", email).query(Long.class).single()).isZero();
        // change_logs/user_sync_state có FK tới users: user không tồn tại nghĩa là cũng không còn dòng nào của nó.
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE payload ->> 'email' = :e")
                .param("e", email).query(Long.class).single()).isZero();

        // Lần sau (không lỗi) đăng ký bình thường với cùng email: không có dữ liệu nửa vời chặn lại.
        org.mockito.Mockito.reset(outboxEvents);
        assertThat(register(email).getResponse().getStatus()).isEqualTo(202);
        assertThat(countUsers(email)).isEqualTo(1);
    }
}
