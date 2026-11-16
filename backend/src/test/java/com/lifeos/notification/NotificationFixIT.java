package com.lifeos.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.notification.repository.NotificationStore;
import com.lifeos.notification.service.DeliveryWorker;
import com.lifeos.notification.service.NotificationOrchestrator;
import com.lifeos.support.ApiTestBase;
import com.lifeos.support.RecordingPushSender;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đợt sửa backend 1 (BACKEND_FIX_REPORT): F2 — dọn inbox không được xóa thông báo còn delivery chờ gửi (D121, D135);
 * F3 — kết quả gửi chỉ được ghi bởi worker còn giữ lease (D137).
 */
class NotificationFixIT extends ApiTestBase {

    @Autowired
    NotificationOrchestrator orchestrator;

    @Autowired
    DeliveryWorker deliveries;

    @Autowired
    RecordingPushSender push;

    @Autowired
    NotificationStore store;

    @Autowired
    TransactionTemplate tx;

    private String registerToken(TestUser user) throws Exception {
        UUID device = UUID.randomUUID();
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel', 'ANDROID')")
                .param("id", device).param("u", user.id()).update();
        String token = "fcm-" + UUID.randomUUID();
        MvcResult r = call(user, HttpMethod.PUT, "/api/v1/devices/" + device + "/push-token",
                body("token", token, "platform", "ANDROID"));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(204);
        return token;
    }

    private UUID remind(TestUser user, boolean urgent) {
        return orchestrator.create(new NotificationOrchestrator.Request(user.id(), "REMINDER", "reminder.generic",
                Map.of(), "test:" + UUID.randomUUID(), Map.of("type", "test"), urgent, null)).orElseThrow();
    }

    private List<Map<String, Object>> deliveriesOf(UUID notificationId) {
        return jdbc.sql("SELECT * FROM notification_deliveries WHERE notification_id = :n").param("n", notificationId)
                .query().listOfRows();
    }

    private int cleanup() {
        int total = 0;
        Integer removed;
        do {
            removed = tx.execute(s -> store.deleteExpired(clock.instant(), 1000));
            total += removed == null ? 0 : removed;
        } while (removed != null && removed == 1000);
        return total;
    }

    /** Xử lý hết delivery đến hạn do test khác để lại, để lô 50 của lượt kế tiếp chắc chắn chứa delivery của test. */
    private void drain() {
        for (int i = 0; i < 200 && deliveries.runOnce() > 0; i++) {
            // lặp tới khi hàng đợi đến hạn trống
        }
    }

    private int notifications(UUID id) {
        return jdbc.sql("SELECT count(*) FROM notifications WHERE id = :id").param("id", id).query(Integer.class)
                .single();
    }

    // ------------------------------------------------------------------------------------------------ F2

    @Test
    void F2_cleanupKeepsANotificationWhosePushIsWaitingForQuietHoursToEnd() throws Exception {
        TestUser user = newUser();
        String token = registerToken(user);
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, :z) ON CONFLICT (user_id) DO"
                + " NOTHING").param("id", UUID.randomUUID()).param("u", user.id()).param("z", zone.getId()).update();
        LocalTime now = LocalTime.ofInstant(clock.instant(), zone);
        long version = version(readJson(get(user, "/api/v1/me/settings")));
        assertThat(patch(user, "/api/v1/me/settings", body("version", version, "quietStart",
                now.minusHours(1).withSecond(0).withNano(0).toString(), "quietEnd",
                now.plusHours(2).withSecond(0).withNano(0).toString())).getResponse().getStatus()).isEqualTo(200);
        assertThat(call(user, HttpMethod.PUT, "/api/v1/notifications/preferences", json.writeValueAsString(Map.of(
                "items", List.of(Map.of("category", "REMINDER", "channel", "IN_APP", "enabled", false)))))
                .getResponse().getStatus()).isEqualTo(200);

        UUID n = remind(user, false);
        assertThat(deliveriesOf(n)).hasSize(1);
        assertThat(deliveriesOf(n).get(0).get("status")).isEqualTo("PENDING");
        assertThat(items(get(user, "/api/v1/notifications"))).as("IN_APP tắt: không hiện trong hộp thư").isEmpty();

        cleanup();
        assertThat(notifications(n)).as("thông báo cha sống tới khi mọi delivery kết thúc").isEqualTo(1);
        assertThat(deliveriesOf(n)).as("delivery chờ hết giờ yên tĩnh không bị cascade xóa").hasSize(1);
        deliveries.runOnce();
        assertThat(push.sentTo(token)).as("còn trong giờ yên tĩnh").isEmpty();

        clock.advance(Duration.ofHours(2).plusMinutes(2));
        for (int i = 0; i < 20 && !"SENT".equals(deliveriesOf(n).get(0).get("status")); i++) {
            deliveries.runOnce();
        }
        assertThat(deliveriesOf(n).get(0).get("status")).isEqualTo("SENT");
        assertThat(push.sentTo(token)).hasSize(1);

        cleanup();
        assertThat(notifications(n)).as("gửi xong thì thông báo hết hạn được dọn").isZero();
    }

    // ------------------------------------------------------------------------------------------------ F3

    @Test
    void F3_aWorkerWhoseLeaseWasReclaimedCannotOverwriteTheNewOwnersResult() throws Exception {
        TestUser user = newUser();
        String token = registerToken(user);
        drain();
        CountDownLatch aEntered = new CountDownLatch(1);
        CountDownLatch bEntered = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        CountDownLatch releaseB = new CountDownLatch(1);
        push.onSend(token, call -> {
            try {
                if (call == 1) {
                    aEntered.countDown();
                    assertThat(releaseA.await(60, TimeUnit.SECONDS)).isTrue();
                    throw new IllegalStateException("worker A: lỗi mạng sau khi lease đã hết");
                }
                if (call == 2) {
                    bEntered.countDown();
                    assertThat(releaseB.await(60, TimeUnit.SECONDS)).isTrue();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
        UUID n = remind(user, true);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> workerA = pool.submit(deliveries::runOnce);
            assertThat(aEntered.await(60, TimeUnit.SECONDS)).as("worker A đang gửi").isTrue();
            clock.advance(Duration.ofMinutes(6)); // quá lease 5 phút: worker B được lấy lại
            Future<Integer> workerB = pool.submit(deliveries::runOnce);
            assertThat(bEntered.await(60, TimeUnit.SECONDS)).as("worker B đã lấy lại và đang gửi").isTrue();
            releaseA.countDown();
            workerA.get(60, TimeUnit.SECONDS);
            releaseB.countDown();
            workerB.get(60, TimeUnit.SECONDS);
        } finally {
            releaseA.countDown();
            releaseB.countDown();
            pool.shutdownNow();
        }
        Map<String, Object> delivery = deliveriesOf(n).get(0);
        assertThat(delivery.get("status")).as("kết quả của chủ lease hiện tại (B) được giữ").isEqualTo("SENT");
        assertThat(((Number) delivery.get("attempt_count")).intValue()).isEqualTo(1);

        clock.advance(Duration.ofHours(7));
        deliveries.runOnce();
        assertThat(push.sentTo(token)).as("không gửi lại sau khi B đã gửi thành công").hasSize(1);
    }

    @RepeatedTest(20)
    void F3_twoWorkersRacingForOneDeliverySendItExactlyOnce() throws Exception {
        TestUser user = newUser();
        String token = registerToken(user);
        drain();
        UUID n = remind(user, true);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> runs = new java.util.ArrayList<>();
            for (int i = 0; i < 2; i++) {
                runs.add(pool.submit(() -> {
                    barrier.await(30, TimeUnit.SECONDS);
                    return deliveries.runOnce();
                }));
            }
            for (Future<Integer> run : runs) {
                run.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        for (int i = 0; i < 20 && !"SENT".equals(deliveriesOf(n).get(0).get("status")); i++) {
            deliveries.runOnce();
        }
        assertThat(deliveriesOf(n).get(0).get("status")).isEqualTo("SENT");
        assertThat(push.sentTo(token)).hasSize(1);
    }
}
