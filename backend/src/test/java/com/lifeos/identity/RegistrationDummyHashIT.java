package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.lifeos.common.security.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Băm giả (D47, D76, ENUM-02 phần đăng ký): khi email đã tồn tại, đăng ký vẫn chạy một phép băm Argon2 có cùng chi phí
 * (simulateMatch) để thời gian phản hồi không lộ sự tồn tại. PasswordHasher thật được bọc spy, nên phép băm thật vẫn
 * chạy; spy chỉ đếm lời gọi. Đường "email không tồn tại" của đăng nhập được kiểm ở bước 1c (LoginIT).
 */
class RegistrationDummyHashIT extends IdentityIntegrationTest {

    @MockitoSpyBean
    PasswordHasher passwordHasher;

    @Test
    void newEmailHashesThePasswordExactlyOnce() throws Exception {
        clearInvocations(passwordHasher);

        assertThat(register(newEmail()).getResponse().getStatus()).isEqualTo(202);

        verify(passwordHasher, times(1)).hash(PASSWORD);
        verify(passwordHasher, never()).simulateMatch(anyString());
    }

    @Test
    void existingEmailRunsExactlyOneDummyHashInsteadOfARealOne() throws Exception {
        String email = newEmail();
        register(email);
        clearInvocations(passwordHasher);

        assertThat(register(email, "mat-khau-khac-999", null).getResponse().getStatus()).isEqualTo(202);

        verify(passwordHasher, times(1)).simulateMatch("mat-khau-khac-999");
        verify(passwordHasher, never()).hash(anyString());
        verify(passwordHasher, never()).matches(anyString(), anyString());
    }

    @Test
    void existingVerifiedEmailAlsoRunsTheDummyHash() throws Exception {
        String email = newEmail();
        registerVerified(email);
        clearInvocations(passwordHasher);

        register(email);

        verify(passwordHasher, times(1)).simulateMatch(PASSWORD);
        verify(passwordHasher, never()).hash(anyString());
    }
}
