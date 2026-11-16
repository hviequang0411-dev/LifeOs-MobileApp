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
 * ENUM-02 / D47 — đường "email không tồn tại" của đăng nhập thật sự chạy MỘT phép băm Argon2 (simulateMatch, cùng
 * tham số với băm thật), còn email có tồn tại thì chạy so khớp thật. Spy bọc PasswordHasher thật nên phép băm vẫn chạy
 * thật; spy chỉ đếm lời gọi. Cùng cấu hình bean override với RegistrationDummyHashIT nên dùng chung context.
 */
class LoginDummyHashIT extends IdentityIntegrationTest {

    @MockitoSpyBean
    PasswordHasher passwordHasher;

    @Test
    void unknownEmailRunsExactlyOneDummyHash() throws Exception {
        clearInvocations(passwordHasher);

        assertThat(login(newEmail(), "mat-khau-bat-ky-1", null, ip).getResponse().getStatus()).isEqualTo(401);

        verify(passwordHasher, times(1)).simulateMatch("mat-khau-bat-ky-1");
        verify(passwordHasher, never()).matches(anyString(), anyString());
        verify(passwordHasher, never()).hash(anyString());
    }

    @Test
    void existingEmailWithWrongPasswordRunsExactlyOneRealMatchAndNoDummy() throws Exception {
        String email = newEmail();
        registerVerified(email);
        clearInvocations(passwordHasher);

        assertThat(login(email, "mat-khau-sai-000", null, ip).getResponse().getStatus()).isEqualTo(401);

        verify(passwordHasher, times(1)).matches(anyString(), anyString());
        verify(passwordHasher, never()).simulateMatch(anyString());
    }
}
