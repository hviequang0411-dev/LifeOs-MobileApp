package com.lifeos.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Cấp và xác thực access token (SECURITY_RULES §4.1, D15). HS256 với thuật toán được ghim: mọi thuật toán khác,
 * kể cả "none", bị từ chối. Hỗ trợ xoay khóa bằng header kid: khóa hiện tại dùng để ký và xác thực, khóa cũ chỉ để
 * xác thực. Không truy vấn DB.
 */
@Component
public class JwtService {

    public static final String DEVICE_CLAIM = "did";
    private static final int MIN_SECRET_BYTES = 32;

    private final String currentKid;
    private final MACSigner signer;
    private final Map<String, MACVerifier> verifiers = new LinkedHashMap<>();
    private final String issuer;
    private final String audience;
    private final Duration ttl;
    private final Duration clockSkew;
    private final Clock clock;

    public JwtService(JwtProperties props, Clock clock) {
        this.clock = clock;
        this.issuer = requireText(props.issuer(), "issuer");
        this.audience = requireText(props.audience(), "audience");
        this.ttl = Objects.requireNonNull(props.accessTokenTtl(), "accessTokenTtl");
        this.clockSkew = Objects.requireNonNull(props.clockSkew(), "clockSkew");
        this.currentKid = requireText(props.kid(), "kid");
        byte[] current = decodeSecret(props.secret(), "lifeos.security.jwt.secret (JWT_SECRET)");
        try {
            this.signer = new MACSigner(current);
            verifiers.put(currentKid, new MACVerifier(current));
            if (hasText(props.previousSecret())) {
                String previousKid = requireText(props.previousKid(), "previousKid (JWT_PREVIOUS_KID)");
                if (previousKid.equals(currentKid)) {
                    throw new IllegalStateException("JWT_PREVIOUS_KID phải khác JWT_KID");
                }
                verifiers.put(previousKid, new MACVerifier(
                        decodeSecret(props.previousSecret(), "lifeos.security.jwt.previous-secret")));
            }
        } catch (JOSEException e) {
            throw new IllegalStateException("Cấu hình khóa JWT không hợp lệ", e);
        }
    }

    public Duration accessTokenTtl() {
        return ttl;
    }

    public IssuedAccessToken issue(UUID userId, UUID deviceId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(deviceId, "deviceId");
        Instant now = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .claim(DEVICE_CLAIM, deviceId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT)
                .keyID(currentKid)
                .build();
        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("Không ký được access token", e);
        }
        return new IssuedAccessToken(jwt.serialize(), ttl.toSeconds());
    }

    /** Kiểm tra theo thứ tự: định dạng → thuật toán → kid → chữ ký → hạn → iss/aud → sub/did. */
    public AuthenticatedUser verify(String token) throws JwtValidationException {
        if (token == null || token.isBlank()) {
            throw invalid("token rỗng");
        }
        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(token);
        } catch (ParseException | RuntimeException e) {
            throw invalid("không phải JWS hợp lệ");
        }
        if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())) {
            throw invalid("thuật toán không được phép");
        }
        String kid = jwt.getHeader().getKeyID();
        MACVerifier verifier = kid == null ? null : verifiers.get(kid);
        if (verifier == null) {
            throw invalid("kid không xác định");
        }
        try {
            if (!jwt.verify(verifier)) {
                throw invalid("sai chữ ký");
            }
        } catch (JOSEException e) {
            throw invalid("không xác thực được chữ ký");
        }
        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw invalid("claims không hợp lệ");
        }
        Date exp = claims.getExpirationTime();
        if (exp == null) {
            throw invalid("thiếu exp");
        }
        if (clock.instant().isAfter(exp.toInstant().plus(clockSkew))) {
            throw new JwtValidationException(JwtValidationException.Reason.EXPIRED, "token đã hết hạn");
        }
        if (!issuer.equals(claims.getIssuer())) {
            throw invalid("sai iss");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
            throw invalid("sai aud");
        }
        UUID userId = parseUuid(claims.getSubject(), "sub");
        Object did = claims.getClaim(DEVICE_CLAIM);
        UUID deviceId = parseUuid(did instanceof String s ? s : null, DEVICE_CLAIM);
        return new AuthenticatedUser(userId, deviceId);
    }

    private static UUID parseUuid(String value, String claim) throws JwtValidationException {
        if (value == null) {
            throw invalid("thiếu " + claim);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw invalid(claim + " không phải UUID");
        }
    }

    private static JwtValidationException invalid(String message) {
        return new JwtValidationException(JwtValidationException.Reason.INVALID, message);
    }

    private static byte[] decodeSecret(String base64, String name) {
        if (!hasText(base64)) {
            throw new IllegalStateException(name + " chưa được cấu hình");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(name + " phải là chuỗi base64");
        }
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(name + " phải có tối thiểu " + MIN_SECRET_BYTES + " byte (256 bit)");
        }
        return bytes;
    }

    private static String requireText(String value, String name) {
        if (!hasText(value)) {
            throw new IllegalStateException("Thiếu cấu hình JWT: " + name);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Access token vừa cấp; expiresIn tính bằng giây (API_CONVENTIONS §8.1). */
    public record IssuedAccessToken(String token, long expiresIn) {

        @Override
        public String toString() {
            return "IssuedAccessToken[expiresIn=" + expiresIn + "]";
        }
    }
}
