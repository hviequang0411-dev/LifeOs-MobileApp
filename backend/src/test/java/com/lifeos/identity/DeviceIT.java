package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-09 Xem và thu hồi thiết bị (FR-ID-10). IDOR-01/02 cho /devices/{id}; SEC-04 / AUTHZ-03: thiết bị bị thu hồi
 * thì refresh thất bại ngay VÀ access token còn hạn bị từ chối ngay (D45).
 */
class DeviceIT extends IdentityIntegrationTest {

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(MvcResult result) throws Exception {
        return (List<Map<String, Object>>) readJson(result).get("items");
    }

    @Test
    void listsOwnDevicesAndMarksTheCurrentOne() throws Exception {
        String email = newEmail();
        registerVerified(email);
        Map<String, Object> phone = loginOk(email, null);
        Map<String, Object> tablet = loginOk(email, null);

        MvcResult result = getDevices(str(phone, "accessToken"));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        List<Map<String, Object>> items = items(result);
        assertThat(items).extracting(d -> d.get("id"))
                .containsExactlyInAnyOrder(phone.get("deviceId"), tablet.get("deviceId"));
        Map<String, Object> current = items.stream().filter(d -> d.get("id").equals(phone.get("deviceId")))
                .findFirst().orElseThrow();
        // WP4 (PHASE_4 D147): thêm pushTokenSuffix (null khi chưa đăng ký token push).
        assertThat(current).containsOnlyKeys("id", "name", "platform", "appVersion", "lastSeenAt", "current",
                "revokedAt", "pushTokenSuffix");
        assertThat(current.get("pushTokenSuffix")).isNull();
        assertThat(current).containsEntry("current", true).containsEntry("name", "Pixel Test")
                .containsEntry("platform", "ANDROID").containsEntry("appVersion", "1.0.0")
                .containsEntry("revokedAt", null);
        assertThat(items).filteredOn(d -> Boolean.TRUE.equals(d.get("current"))).hasSize(1);
    }

    @Test
    void revokingADeviceBlocksItsRefreshAndItsAccessTokenImmediately() throws Exception {
        String email = newEmail();
        registerVerified(email);
        Map<String, Object> phone = loginOk(email, null);
        Map<String, Object> laptop = loginOk(email, null);
        UUID phoneId = UUID.fromString(str(phone, "deviceId"));

        assertThat(deleteDevice(phoneId, str(laptop, "accessToken")).getResponse().getStatus()).isEqualTo(204);

        // Refresh của thiết bị bị thu hồi thất bại ngay.
        assertThat(readJson(refresh(str(phone, "refreshToken"))))
                .containsEntry("errorCode", "AUTH_REFRESH_TOKEN_INVALID");
        // Access token còn hạn của thiết bị đó bị từ chối ngay (không phải chờ 15 phút).
        MvcResult withRevokedAccess = getDevices(str(phone, "accessToken"));
        assertThat(withRevokedAccess.getResponse().getStatus()).isEqualTo(401);
        assertThat(readJson(withRevokedAccess)).containsEntry("errorCode", "AUTH_TOKEN_INVALID");
        // Thiết bị khác của cùng user không bị ảnh hưởng; danh sách vẫn hiển thị thiết bị đã thu hồi.
        MvcResult list = getDevices(str(laptop, "accessToken"));
        assertThat(list.getResponse().getStatus()).isEqualTo(200);
        assertThat(items(list)).filteredOn(d -> d.get("id").equals(phoneId.toString()))
                .singleElement().satisfies(d -> assertThat(d.get("revokedAt")).isNotNull());
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_tokens WHERE device_id = :d AND revoked_at IS NULL")
                .param("d", phoneId).query(Integer.class).single()).isZero();
    }

    @Test
    void revokingTheCurrentDeviceIsAllowedAndIsIdempotent() throws Exception {
        String email = newEmail();
        registerVerified(email);
        Map<String, Object> session = loginOk(email, null);
        UUID deviceId = UUID.fromString(str(session, "deviceId"));
        String access = str(session, "accessToken");

        assertThat(deleteDevice(deviceId, access).getResponse().getStatus()).isEqualTo(204);
        assertThat(getDevices(access).getResponse().getStatus()).isEqualTo(401);

        // Gọi lại (bằng thiết bị khác) trên thiết bị đã thu hồi: vẫn 204, thời điểm thu hồi không đổi.
        Object revokedAt = jdbc.sql("SELECT revoked_at FROM devices WHERE id = :d").param("d", deviceId)
                .query().singleValue();
        Map<String, Object> other = loginOk(email, null);
        assertThat(deleteDevice(deviceId, str(other, "accessToken")).getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT revoked_at FROM devices WHERE id = :d").param("d", deviceId)
                .query().singleValue()).isEqualTo(revokedAt);
    }

    @Test
    void anotherUsersDeviceIsIndistinguishableFromANonExistentOneAndIsNotRevoked() throws Exception {
        String victimEmail = newEmail();
        registerVerified(victimEmail);
        Map<String, Object> victim = loginOk(victimEmail, null);
        UUID victimDevice = UUID.fromString(str(victim, "deviceId"));

        String attackerEmail = newEmail();
        registerVerified(attackerEmail);
        String attacker = str(loginOk(attackerEmail, null), "accessToken");

        MvcResult foreign = deleteDevice(victimDevice, attacker); // IDOR-01/02
        MvcResult missing = deleteDevice(UUID.randomUUID(), attacker);

        assertThat(foreign.getResponse().getStatus()).isEqualTo(404);
        assertThat(missing.getResponse().getStatus()).isEqualTo(404);
        assertThat(problemWithoutCorrelationId(foreign)).isEqualTo(problemWithoutCorrelationId(missing))
                .containsEntry("errorCode", "DEVICE_NOT_FOUND");
        // Dữ liệu của nạn nhân không đổi.
        assertThat(deviceRevoked(victimDevice)).isFalse();
        assertThat(refresh(str(victim, "refreshToken")).getResponse().getStatus()).isEqualTo(200);
        // Danh sách của kẻ tấn công không chứa thiết bị của nạn nhân.
        assertThat(items(getDevices(attacker))).extracting(d -> d.get("id"))
                .doesNotContain(victimDevice.toString());
    }

    @Test
    void deviceEndpointsRequireAuthentication() throws Exception {
        MvcResult list = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/devices").with(fromIp(ip))).andReturn();
        MvcResult delete = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/devices/" + UUID.randomUUID()).with(fromIp(ip))).andReturn();
        assertThat(list.getResponse().getStatus()).isEqualTo(401);
        assertThat(delete.getResponse().getStatus()).isEqualTo(401);
    }
}
