package com.lifeos.notification.channel;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * NTF-05: FCM HTTP v1 giả lập bằng {@code com.sun.net.httpserver} (JDK, không thêm thư viện — thay cho WireMock trong
 * đặc tả) [PROPOSED]: lấy access token một lần, ánh xạ 200 → SENT, 404/UNREGISTERED → UNREGISTERED, 5xx/429 →
 * TRANSIENT, 400 → PERMANENT.
 */
class FcmPushSenderTest {

    private HttpServer server;
    private final AtomicInteger tokenCalls = new AtomicInteger();
    private final List<String> authorizations = new CopyOnWriteArrayList<>();
    private volatile int sendStatus = 200;
    private volatile String sendBody = "{}";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/token", exchange -> {
            tokenCalls.incrementAndGet();
            byte[] body = "{\"access_token\":\"at-1\",\"expires_in\":3600}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/v1/projects/demo/messages:send", exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = sendBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(sendStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        try {
            server.stop(0);
        } catch (IllegalStateException e) {
            // đã dừng trong test
        }
    }

    private FcmPushSender sender() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        return new FcmPushSender(new FcmPushSender.Properties(true, "demo", "svc@demo.iam", pem, base + "/token", base),
                JsonMapper.builder().build(), Clock.systemUTC());
    }

    private static PushSender.PushMessage message() {
        return new PushSender.PushMessage("device-token", "ANDROID", "Nhắc nhở", "Bạn có nhắc nhở",
                Map.of("dedupeKey", "reminder:1:2"));
    }

    @Test
    void Ntf_05_fcmResponsesMapToDeliveryOutcomes() throws Exception {
        FcmPushSender sender = sender();
        assertThat(sender.send(message()).outcome()).isEqualTo(PushSender.Outcome.SENT);
        sendStatus = 404;
        assertThat(sender.send(message()).outcome()).isEqualTo(PushSender.Outcome.UNREGISTERED);
        sendStatus = 400;
        sendBody = "{\"error\":{\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}";
        assertThat(sender.send(message()).outcome()).isEqualTo(PushSender.Outcome.UNREGISTERED);
        sendBody = "{\"error\":{\"status\":\"INVALID_ARGUMENT\"}}";
        assertThat(sender.send(message()).outcome()).isEqualTo(PushSender.Outcome.PERMANENT);
        sendStatus = 503;
        assertThat(sender.send(message()).outcome()).isEqualTo(PushSender.Outcome.TRANSIENT);
        sendStatus = 429;
        assertThat(sender.send(message()).outcome()).isEqualTo(PushSender.Outcome.TRANSIENT);
        assertThat(tokenCalls.get()).as("access token dùng lại").isEqualTo(1);
        assertThat(authorizations).allMatch("Bearer at-1"::equals);
        server.stop(0);
        assertThat(sender.send(message()).outcome()).as("mất kết nối").isEqualTo(PushSender.Outcome.TRANSIENT);
    }
}
