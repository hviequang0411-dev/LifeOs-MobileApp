package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SEC-02 và các quy tắc ở SECURITY_RULES §4.1 cho access token. */
class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");
    private static final String SECRET = randomSecret(32);
    private static final String OTHER_SECRET = randomSecret(32);

    private final UUID userId = UUID.randomUUID();
    private final UUID deviceId = UUID.randomUUID();

    private static String randomSecret(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private static JwtProperties props(String secret, String kid, String previousSecret, String previousKid) {
        return new JwtProperties(secret, kid, previousSecret, previousKid, "lifeos-backend", "lifeos-app",
                Duration.ofMinutes(15), Duration.ofSeconds(30));
    }

    private static JwtService service(Instant at) {
        return new JwtService(props(SECRET, "k1", null, null), Clock.fixed(at, ZoneOffset.UTC));
    }

    private static JwtValidationException.Reason reasonOf(JwtService service, String token) {
        try {
            service.verify(token);
            return null;
        } catch (JwtValidationException e) {
            return e.reason();
        }
    }

    /** Ký token tùy ý để thử các trường hợp sai claim. */
    private static String sign(String secret, JWSAlgorithm alg, String kid, JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(alg).type(JOSEObjectType.JWT).keyID(kid).build(),
                claims);
        jwt.sign(new MACSigner(Base64.getDecoder().decode(secret)));
        return jwt.serialize();
    }

    private JWTClaimsSet.Builder validClaims() {
        return new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim("did", deviceId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issuer("lifeos-backend")
                .audience("lifeos-app")
                .issueTime(Date.from(NOW))
                .expirationTime(Date.from(NOW.plus(Duration.ofMinutes(15))));
    }

    @Test
    void issuedTokenVerifiesAndCarriesUserAndDevice() throws Exception {
        JwtService service = service(NOW);
        JwtService.IssuedAccessToken issued = service.issue(userId, deviceId);

        assertThat(issued.expiresIn()).isEqualTo(900);
        AuthenticatedUser user = service.verify(issued.token());
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.deviceId()).isEqualTo(deviceId);

        SignedJWT parsed = SignedJWT.parse(issued.token());
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        assertThat(parsed.getHeader().getKeyID()).isEqualTo("k1");
        JWTClaimsSet claims = parsed.getJWTClaimsSet();
        assertThat(claims.getJWTID()).isNotBlank();
        assertThat(claims.getIssueTime()).isNotNull();
        assertThat(claims.getExpirationTime().toInstant()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        // Không chứa dữ liệu cá nhân ngoài định danh (SECURITY_RULES §4.1).
        assertThat(claims.getClaims().keySet()).containsExactlyInAnyOrder("sub", "did", "jti", "iss", "aud", "iat",
                "exp");
    }

    @Test
    void tokenWithinClockSkewIsAcceptedButBeyondIsExpired() {
        String token = service(NOW).issue(userId, deviceId).token();
        Instant exp = NOW.plus(Duration.ofMinutes(15));

        assertThat(reasonOf(service(exp.plusSeconds(30)), token)).isNull();
        assertThat(reasonOf(service(exp.plusSeconds(31)), token)).isEqualTo(JwtValidationException.Reason.EXPIRED);
    }

    @Test
    void algNoneIsRejected() {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\",\"kid\":\"k1\"}"
                .getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(("{\"sub\":\"" + userId + "\",\"did\":\"" + deviceId
                + "\",\"iss\":\"lifeos-backend\",\"aud\":\"lifeos-app\",\"exp\":" + (NOW.getEpochSecond() + 900)
                + "}").getBytes(StandardCharsets.UTF_8));
        String unsigned = header + "." + payload + ".";

        assertThat(reasonOf(service(NOW), unsigned)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void otherHmacAlgorithmWithSameSecretIsRejected() throws Exception {
        String hs512 = sign(randomSecret(64), JWSAlgorithm.HS512, "k1", validClaims().build());
        assertThat(reasonOf(service(NOW), hs512)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void wrongSignatureIsRejected() throws Exception {
        String forged = sign(OTHER_SECRET, JWSAlgorithm.HS256, "k1", validClaims().build());
        assertThat(reasonOf(service(NOW), forged)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void forgedExpiredTokenIsInvalidNotExpired() throws Exception {
        // Chữ ký được kiểm trước hạn: token giả mạo không được báo "hết hạn".
        JWTClaimsSet expired = validClaims().expirationTime(Date.from(NOW.minus(Duration.ofHours(1)))).build();
        String forged = sign(OTHER_SECRET, JWSAlgorithm.HS256, "k1", expired);
        assertThat(reasonOf(service(NOW), forged)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void tamperedPayloadIsRejected() {
        String token = service(NOW).issue(userId, deviceId).token();
        String[] parts = token.split("\\.");
        String otherPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"" + UUID.randomUUID() + "\"}").getBytes(StandardCharsets.UTF_8));
        assertThat(reasonOf(service(NOW), parts[0] + "." + otherPayload + "." + parts[2]))
                .isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void wrongIssuerIsRejected() throws Exception {
        String token = sign(SECRET, JWSAlgorithm.HS256, "k1", validClaims().issuer("evil").build());
        assertThat(reasonOf(service(NOW), token)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void wrongAudienceIsRejected() throws Exception {
        String token = sign(SECRET, JWSAlgorithm.HS256, "k1", validClaims().audience("other-app").build());
        assertThat(reasonOf(service(NOW), token)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void missingDeviceClaimIsRejected() throws Exception {
        String token = sign(SECRET, JWSAlgorithm.HS256, "k1", validClaims().claim("did", null).build());
        assertThat(reasonOf(service(NOW), token)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void nonUuidSubjectIsRejected() throws Exception {
        String token = sign(SECRET, JWSAlgorithm.HS256, "k1", validClaims().subject("admin").build());
        assertThat(reasonOf(service(NOW), token)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void missingExpirationIsRejected() throws Exception {
        String token = sign(SECRET, JWSAlgorithm.HS256, "k1", validClaims().expirationTime(null).build());
        assertThat(reasonOf(service(NOW), token)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void unknownOrMissingKidIsRejected() throws Exception {
        assertThat(reasonOf(service(NOW), sign(SECRET, JWSAlgorithm.HS256, "k9", validClaims().build())))
                .isEqualTo(JwtValidationException.Reason.INVALID);
        assertThat(reasonOf(service(NOW), sign(SECRET, JWSAlgorithm.HS256, null, validClaims().build())))
                .isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void garbageIsRejected() {
        JwtService service = service(NOW);
        assertThat(reasonOf(service, "not-a-jwt")).isEqualTo(JwtValidationException.Reason.INVALID);
        assertThat(reasonOf(service, "")).isEqualTo(JwtValidationException.Reason.INVALID);
        assertThat(reasonOf(service, null)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void previousKeyStillVerifiesDuringRotationButIsNotUsedToSign() throws Exception {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        JwtService beforeRotation = new JwtService(props(OTHER_SECRET, "old", null, null), clock);
        String oldToken = beforeRotation.issue(userId, deviceId).token();

        JwtService rotated = new JwtService(props(SECRET, "new", OTHER_SECRET, "old"), clock);
        assertThat(rotated.verify(oldToken).userId()).isEqualTo(userId);
        assertThat(SignedJWT.parse(rotated.issue(userId, deviceId).token()).getHeader().getKeyID())
                .isEqualTo("new");

        // Hết thời gian chuyển đổi: bỏ khóa cũ thì token cũ bị từ chối.
        JwtService afterRotation = new JwtService(props(SECRET, "new", null, null), clock);
        assertThat(reasonOf(afterRotation, oldToken)).isEqualTo(JwtValidationException.Reason.INVALID);
    }

    @Test
    void refusesToStartWithMissingShortOrNonBase64Secret() {
        Clock clock = Clock.systemUTC();
        assertThatThrownBy(() -> new JwtService(props(null, "k1", null, null), clock))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(props(randomSecret(31), "k1", null, null), clock))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 byte");
        assertThatThrownBy(() -> new JwtService(props("không-phải-base64!!", "k1", null, null), clock))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("base64");
        assertThatThrownBy(() -> new JwtService(props(SECRET, "k1", OTHER_SECRET, "k1"), clock))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void secretsAreNeverPrinted() {
        assertThat(props(SECRET, "k1", OTHER_SECRET, "old").toString()).doesNotContain(SECRET)
                .doesNotContain(OTHER_SECRET);
        JwtService.IssuedAccessToken issued = service(NOW).issue(userId, deviceId);
        assertThat(issued.toString()).doesNotContain(issued.token());
    }
}
