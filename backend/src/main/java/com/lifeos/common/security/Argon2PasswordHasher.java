package com.lifeos.common.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Argon2id qua Argon2PasswordEncoder của Spring Security (cần Bouncy Castle). Tham số nằm trong chuỗi hash nên
 * nâng cấp được về sau (needsRehash).
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder;
    /** Hash của một mật khẩu ngẫu nhiên, chỉ để simulateMatch có cùng chi phí với so khớp thật. */
    private final String dummyHash;

    public Argon2PasswordHasher(PasswordHashProperties props) {
        this.encoder = new Argon2PasswordEncoder(props.saltLength(), props.hashLength(), props.parallelism(),
                props.memoryKib(), props.iterations());
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        this.dummyHash = encoder.encode(Base64.getEncoder().encodeToString(random));
    }

    @Override
    public String hash(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        return encoder.encode(PasswordHasher.normalize(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, String encodedHash) {
        Objects.requireNonNull(rawPassword, "rawPassword");
        if (encodedHash == null || encodedHash.isEmpty()) {
            return false;
        }
        return encoder.matches(PasswordHasher.normalize(rawPassword), encodedHash);
    }

    @Override
    public boolean needsRehash(String encodedHash) {
        return encoder.upgradeEncoding(encodedHash);
    }

    @Override
    public void simulateMatch(String rawPassword) {
        encoder.matches(PasswordHasher.normalize(rawPassword == null ? "" : rawPassword), dummyHash);
    }
}
