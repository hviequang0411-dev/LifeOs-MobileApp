package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Băm mật khẩu Argon2id (SECURITY_RULES §3.2, D46/D47). */
class Argon2PasswordHasherTest {

    private static final PasswordHashProperties DEFAULTS = new PasswordHashProperties(16, 32, 1, 19456, 2);

    private final Argon2PasswordHasher hasher = new Argon2PasswordHasher(DEFAULTS);

    @Test
    void hashesWithArgon2idAndVerifies() {
        String hash = hasher.hash("correct horse battery");

        assertThat(hash).startsWith("$argon2id$").doesNotContain("correct horse battery");
        assertThat(hash).contains("m=19456").contains("t=2").contains("p=1");
        assertThat(hasher.matches("correct horse battery", hash)).isTrue();
        assertThat(hasher.matches("correct horse batterY", hash)).isFalse();
    }

    @Test
    void samePasswordHashesDifferentlyBecauseOfSalt() {
        assertThat(hasher.hash("password-123")).isNotEqualTo(hasher.hash("password-123"));
    }

    @Test
    void passwordIsNormalizedWithNfkcBeforeHashing() {
        // "ｐａｓｓ１２３４" (full-width) và "pass1234" giống nhau sau NFKC (D47).
        String hash = hasher.hash("ｐａｓｓ１２３４");
        assertThat(hasher.matches("pass1234", hash)).isTrue();
        // Dạng tổ hợp và dạng dựng sẵn của "é" cũng tương đương.
        String composed = hasher.hash("café-password");
        assertThat(hasher.matches("café-password", composed)).isTrue();
    }

    @Test
    void emptyOrMissingHashNeverMatches() {
        assertThat(hasher.matches("anything", null)).isFalse();
        assertThat(hasher.matches("anything", "")).isFalse();
    }

    @Test
    void hashWithWeakerParametersNeedsRehash() {
        Argon2PasswordHasher weaker = new Argon2PasswordHasher(new PasswordHashProperties(16, 32, 1, 4096, 1));
        String oldHash = weaker.hash("password-123");

        assertThat(hasher.needsRehash(oldHash)).isTrue();
        assertThat(hasher.needsRehash(hasher.hash("password-123"))).isFalse();
        // Hash cũ vẫn xác thực được để còn băm lại sau khi đăng nhập thành công.
        assertThat(hasher.matches("password-123", oldHash)).isTrue();
    }

    @Test
    void simulateMatchAcceptsAnyInputWithoutError() {
        hasher.simulateMatch("whatever");
        hasher.simulateMatch(null);
    }
}
