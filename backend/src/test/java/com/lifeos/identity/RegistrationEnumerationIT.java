package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * ENUM-01 / SEC-06 (phần register và resend-verification): phản hồi giống hệt nhau (mã, header nội dung, thân) dù
 * email chưa tồn tại, đã tồn tại chưa xác minh hay đã xác minh. forgot-password bổ sung ở bước 1d.
 */
class RegistrationEnumerationIT extends IdentityIntegrationTest {

    private record Observed(int status, String contentType, String body) {
        static Observed of(MvcResult result) throws Exception {
            return new Observed(result.getResponse().getStatus(), result.getResponse().getContentType(),
                    result.getResponse().getContentAsString());
        }
    }

    @Test
    void registerRespondsIdenticallyWhetherEmailExistsOrNot() throws Exception {
        String unverified = newEmail();
        register(unverified);
        String verified = newEmail();
        registerVerified(verified);

        List<Observed> observed = List.of(
                Observed.of(register(newEmail())),
                Observed.of(register(unverified)),
                Observed.of(register(verified)));

        assertThat(observed).allSatisfy(o -> assertThat(o).isEqualTo(observed.get(0)));
        assertThat(observed.get(0).status()).isEqualTo(202);
        assertThat(observed.get(0).body()).isEmpty();
    }

    @Test
    void resendRespondsIdenticallyWhetherEmailExistsOrNot() throws Exception {
        String unverified = newEmail();
        register(unverified);
        String verified = newEmail();
        registerVerified(verified);

        List<Observed> observed = List.of(
                Observed.of(resend(newEmail())),
                Observed.of(resend(unverified)),
                Observed.of(resend(verified)));

        assertThat(observed).allSatisfy(o -> assertThat(o).isEqualTo(observed.get(0)));
        assertThat(observed.get(0).status()).isEqualTo(202);
        assertThat(observed.get(0).body()).isEmpty();
    }
}
