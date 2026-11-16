package com.lifeos.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * INT-17 (PHASE_7 D206): metric của LifeOS có nhãn hữu hạn — không nhãn userId/id thực thể/nội dung, không giá trị
 * dạng UUID; các metric của Phase 7 tồn tại sau khi dùng tính năng.
 */
class MetricsTagsIT extends ApiTestBase {

    private static final Set<String> FORBIDDEN_KEYS = Set.of("userId", "user", "user_id", "entityId", "id", "email",
            "title", "deviceId", "token");
    private static final Pattern UUID_LIKE = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-.*");

    @Autowired
    MeterRegistry registry;

    @Test
    void Int_17_lifeosMetricsHaveBoundedTagsAndPhase7MetersExist() throws Exception {
        TestUser user = newUser();
        assertThat(get(user, "/api/v1/dashboard").getResponse().getStatus()).isEqualTo(200);
        assertThat(get(user, "/api/v1/reviews/daily").getResponse().getStatus()).isEqualTo(200);

        List<String> violations = new ArrayList<>();
        for (Meter meter : registry.getMeters()) {
            String name = meter.getId().getName();
            if (!name.startsWith("lifeos.")) {
                continue;
            }
            for (Tag tag : meter.getId().getTags()) {
                if (FORBIDDEN_KEYS.contains(tag.getKey()) || UUID_LIKE.matcher(tag.getValue()).matches()) {
                    violations.add(name + " " + tag);
                }
            }
        }
        assertThat(violations).isEmpty();
        assertThat(registry.find("lifeos.cache.requests").tag("result", "bypass").counter()).isNotNull();
        assertThat(registry.find("lifeos.dashboard.cards").tag("card", "tasks_today").tag("status", "OK").counter())
                .isNotNull();
    }
}
