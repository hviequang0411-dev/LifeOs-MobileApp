package com.lifeos.notification.channel;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * FCM HTTP v1 (PHASE_4 D137, §8): access token OAuth2 ký bằng khóa service account (RS256, nimbus — đã có trong stack),
 * gửi {@code POST {endpoint}/v1/projects/{projectId}/messages:send}. Thông tin xác thực CHỈ từ biến môi trường
 * (SECURITY_RULES §12): {@code FCM_PROJECT_ID}, {@code FCM_CLIENT_EMAIL}, {@code FCM_PRIVATE_KEY} (PKCS#8 PEM). Không
 * log token thiết bị hay nội dung (D147).
 * <ul>
 *   <li>200 → SENT; 404 hoặc lỗi {@code UNREGISTERED} → UNREGISTERED (gỡ token).</li>
 *   <li>429, 5xx, lỗi mạng → TRANSIENT; 4xx khác → PERMANENT.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "lifeos.fcm.enabled", havingValue = "true")
@EnableConfigurationProperties(FcmPushSender.Properties.class)
class FcmPushSender implements PushSender {

    @ConfigurationProperties("lifeos.fcm")
    record Properties(boolean enabled, String projectId, String clientEmail, String privateKey, String tokenUri,
            String endpoint) {
        @Override
        public String toString() {
            return "FcmProperties[projectId=" + projectId + "]";
        }
    }

    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final Properties props;
    private final JsonMapper json;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private volatile String accessToken;
    private volatile Instant accessTokenExpiry = Instant.EPOCH;

    FcmPushSender(Properties props, JsonMapper json, Clock clock) {
        this.props = props;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public PushResult send(PushMessage message) {
        try {
            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("title", message.title());
            notification.put("body", message.body());
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("token", message.token());
            msg.put("notification", notification);
            msg.put("data", message.data());
            HttpRequest request = HttpRequest.newBuilder(URI.create(props.endpoint() + "/v1/projects/"
                            + props.projectId() + "/messages:send"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken())
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("message", msg))))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) {
                return PushResult.sent();
            }
            if (status == 404 || response.body().contains("UNREGISTERED")) {
                return new PushResult(Outcome.UNREGISTERED, "FCM_UNREGISTERED");
            }
            if (status == 429 || status >= 500) {
                return new PushResult(Outcome.TRANSIENT, "FCM_" + status);
            }
            return new PushResult(Outcome.PERMANENT, "FCM_" + status);
        } catch (java.io.IOException e) {
            return new PushResult(Outcome.TRANSIENT, "FCM_IO");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PushResult(Outcome.TRANSIENT, "FCM_INTERRUPTED");
        }
    }

    /** Access token OAuth2 (JWT bearer grant), dùng lại tới khi còn ≤ 1 phút. */
    private synchronized String accessToken() throws java.io.IOException, InterruptedException {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiry.minusSeconds(60))) {
            return accessToken;
        }
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(props.clientEmail()).audience(props.tokenUri())
                    .claim("scope", SCOPE).issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(3600)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                    claims);
            jwt.sign(new RSASSASigner(privateKey()));
            String form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer",
                    StandardCharsets.UTF_8) + "&assertion=" + jwt.serialize();
            HttpResponse<String> response = http.send(HttpRequest.newBuilder(URI.create(props.tokenUri()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new java.io.IOException("token endpoint " + response.statusCode());
            }
            JsonNode body = json.readTree(response.body());
            accessToken = body.get("access_token").asString();
            accessTokenExpiry = now.plusSeconds(body.has("expires_in") ? body.get("expires_in").asLong() : 3600);
            return accessToken;
        } catch (com.nimbusds.jose.JOSEException | java.security.GeneralSecurityException e) {
            throw new java.io.IOException("Không ký được JWT FCM", e);
        }
    }

    private RSAPrivateKey privateKey() throws java.security.GeneralSecurityException {
        String pem = props.privateKey().replace("\\n", "\n").replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
    }
}
