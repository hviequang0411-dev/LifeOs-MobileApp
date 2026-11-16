package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.IntegrationTestBase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * SEC-13 (SECURITY_RULES §11.4, D50): chỉ health (liveness/readiness) công khai và chỉ trả trạng thái tối thiểu; các
 * endpoint Actuator nhạy cảm không truy cập được — không token thì 401, có token cũng không tồn tại (không được bật).
 */
class ActuatorExposureIT extends IntegrationTestBase {

    private static final List<String> SENSITIVE = List.of("env", "beans", "heapdump", "threaddump", "loggers",
            "metrics", "prometheus", "info", "configprops", "mappings", "conditions", "scheduledtasks", "shutdown",
            "caches", "sbom");

    @Autowired
    JwtService jwtService;

    private final String ip = randomIp();

    private MvcResult get(String path, String bearer) throws Exception {
        var request = MockMvcRequestBuilders.get(path).with(fromIp(ip));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return mvc.perform(request).andReturn();
    }

    @Test
    void healthIsPublicAndMinimal() throws Exception {
        for (String path : List.of("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness")) {
            MvcResult result = get(path, null);
            assertThat(result.getResponse().getStatus()).as(path).isEqualTo(200);
            assertThat(result.getResponse().getContentAsString()).as(path).contains("\"status\":\"UP\"")
                    .doesNotContain("components").doesNotContain("details").doesNotContain("db")
                    .doesNotContain("diskSpace");
        }
    }

    @Test
    void sensitiveEndpointsAreUnreachable() throws Exception {
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();
        for (String name : SENSITIVE) {
            String path = "/actuator/" + name;
            assertThat(get(path, null).getResponse().getStatus()).as(path + " không token").isEqualTo(401);
            assertThat(get(path, token).getResponse().getStatus()).as(path + " có token").isEqualTo(404);
        }
    }

    @Test
    void discoveryPageDoesNotAdvertiseSensitiveEndpoints() throws Exception {
        assertThat(get("/actuator", null).getResponse().getStatus()).isEqualTo(401);
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();
        String body = get("/actuator", token).getResponse().getContentAsString();
        for (String name : SENSITIVE) {
            assertThat(body).as(name).doesNotContain("/actuator/" + name);
        }
    }
}
