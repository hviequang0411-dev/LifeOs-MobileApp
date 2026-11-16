package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.UnauthorizedException;
import com.lifeos.identity.service.AuthService;
import com.lifeos.identity.service.OpaqueTokens;
import com.lifeos.identity.service.TokenPair;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AUTHZ-04 (D72): hai /auth/refresh đồng thời với cùng một refresh token, lặp ≥ 20 lần. Mỗi lần: đúng một request
 * nhận token mới; request thua bị 401 và cả family bị thu hồi; không bao giờ còn hai token hợp lệ trong cùng family.
 * Có timeout cứng cho mỗi lần.
 */
class RefreshConcurrencyIT extends IdentityIntegrationTest {

    private static final int ITERATIONS = 20;

    @Autowired
    AuthService authService;

    @Test
    void concurrentRefreshWithTheSameTokenHasExactlyOneWinner() throws Exception {
        String email = newEmail();
        registerVerified(email);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < ITERATIONS; i++) {
                String refreshToken = str(loginOk(email, null), "refreshToken");
                UUID familyId = jdbc.sql("SELECT family_id FROM refresh_tokens WHERE token_hash = :h")
                        .param("h", OpaqueTokens.sha256Hex(refreshToken)).query(UUID.class).single();

                CountDownLatch start = new CountDownLatch(1);
                List<Future<TokenPair>> futures = new ArrayList<>();
                for (int t = 0; t < 2; t++) {
                    String clientIp = randomIp();
                    futures.add(pool.submit(() -> {
                        start.await();
                        return authService.refresh(refreshToken, clientIp);
                    }));
                }
                start.countDown();

                int winners = 0;
                int losers = 0;
                for (Future<TokenPair> f : futures) {
                    try {
                        assertThat(f.get(30, TimeUnit.SECONDS)).isNotNull();
                        winners++;
                    } catch (ExecutionException e) {
                        assertThat(e.getCause()).isInstanceOf(UnauthorizedException.class);
                        assertThat(((UnauthorizedException) e.getCause()).getErrorCode())
                                .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
                        losers++;
                    }
                }
                assertThat(winners).as("lần %d: số request thắng", i).isEqualTo(1);
                assertThat(losers).as("lần %d: số request thua", i).isEqualTo(1);
                int validInFamily = jdbc.sql("SELECT count(*) FROM refresh_tokens WHERE family_id = :f"
                                + " AND used_at IS NULL AND revoked_at IS NULL")
                        .param("f", familyId).query(Integer.class).single();
                assertThat(validInFamily).as("lần %d: token còn hợp lệ trong family", i).isZero();
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
