package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * SYN-A10 (PHASE_5 D168), phía server: 3 "thiết bị" mô phỏng cùng một user thao tác ngẫu nhiên có seed trên Task
 * (tạo, đổi tiêu đề, xóa, đồng bộ). Thiết bị giải quyết CONFLICT bằng bản server (không mất dữ liệu im lặng: bản local
 * bị thay chỉ khi server trả CONFLICT kèm snapshot). Sau khi tất cả đồng bộ xong, mọi thiết bị hội tụ đúng trạng thái
 * server; không có bản ghi trùng. Mặc định 3 seed × 150 bước trong {@code mvn verify}; bản đầy đủ của đặc tả (20 seed ×
 * 1000 bước) chạy với {@code -Dlifeos.sync.simulation.full=true} (nightly) [PROPOSED].
 */
class SyncMultiDeviceSimulationIT extends SyncTestBase {

    static List<Long> seeds() {
        int count = Boolean.getBoolean("lifeos.sync.simulation.full") ? 20 : 3;
        List<Long> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(20261107L + i);
        }
        return out;
    }

    private static int steps() {
        return Boolean.getBoolean("lifeos.sync.simulation.full") ? 1000 : 150;
    }

    private record Local(String title, long version) {
    }

    private final class Device {
        final Map<UUID, Local> tasks = new HashMap<>();
        final List<Map<String, Object>> outbox = new ArrayList<>();
        String cursor;

        void sync(TestUser user) throws Exception {
            while (!outbox.isEmpty()) {
                List<Map<String, Object>> batch = new ArrayList<>(outbox.subList(0, Math.min(50, outbox.size())));
                List<Map<String, Object>> results = push(user, batch);
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> m = batch.get(i);
                    Map<String, Object> r = results.get(i);
                    UUID id = UUID.fromString((String) m.get("entityId"));
                    String status = (String) r.get("status");
                    if ("APPLIED".equals(status) || "DUPLICATE".equals(status)) {
                        if ("DELETE".equals(m.get("operation"))) {
                            tasks.remove(id);
                        } else if (tasks.containsKey(id)) {
                            tasks.put(id, new Local(tasks.get(id).title(), longOf(r.get("entityVersion"))));
                        }
                    } else if ("CONFLICT".equals(status)) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> snap = (Map<String, Object>) r.get("serverSnapshot");
                        if (Boolean.TRUE.equals(snap.get("deleted"))) {
                            tasks.remove(id);
                        } else {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) snap.get("data");
                            tasks.put(id, new Local((String) data.get("title"), longOf(snap.get("version"))));
                        }
                    } else {
                        tasks.remove(id);
                    }
                }
                outbox.subList(0, batch.size()).clear();
            }
            boolean more;
            do {
                Map<String, Object> page = readJson(get(user, "/api/v1/sync/pull?limit=50"
                        + (cursor == null ? "" : "&cursor=" + cursor)));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> changes = (List<Map<String, Object>>) page.get("changes");
                for (Map<String, Object> c : changes) {
                    if (!"task".equals(c.get("entityType"))) {
                        continue;
                    }
                    UUID id = UUID.fromString((String) c.get("entityId"));
                    if ("DELETE".equals(c.get("operation"))) {
                        tasks.remove(id);
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) c.get("data");
                        tasks.put(id, new Local((String) data.get("title"), longOf(c.get("version"))));
                    }
                }
                cursor = (String) page.get("nextCursor");
                more = Boolean.TRUE.equals(page.get("hasMore"));
            } while (more);
        }
    }

    @ParameterizedTest
    @MethodSource("seeds")
    void SynA_10_threeDevicesConvergeWithoutDuplicates(long seed) throws Exception {
        TestUser user = newUser();
        Random random = new Random(seed);
        List<Device> devices = List.of(new Device(), new Device(), new Device());
        int created = 0;
        for (int step = 0; step < steps(); step++) {
            Device d = devices.get(random.nextInt(3));
            int action = random.nextInt(10);
            List<UUID> known = new ArrayList<>(d.tasks.keySet());
            if (action < 3 || known.isEmpty()) {
                UUID id = UUID.randomUUID();
                String title = "T" + step;
                d.tasks.put(id, new Local(title, 0));
                d.outbox.add(mutation("task", id, "CREATE", 0, payload("title", title)));
                created++;
            } else if (action < 6) {
                UUID id = known.get(random.nextInt(known.size()));
                Local local = d.tasks.get(id);
                if (local.version() > 0 && d.outbox.stream().noneMatch(m -> m.get("entityId").equals(id.toString()))) {
                    String title = "U" + step;
                    d.tasks.put(id, new Local(title, local.version()));
                    d.outbox.add(mutation("task", id, "UPDATE", local.version(), payload("title", title)));
                }
            } else if (action < 7) {
                UUID id = known.get(random.nextInt(known.size()));
                Local local = d.tasks.get(id);
                if (local.version() > 0 && d.outbox.stream().noneMatch(m -> m.get("entityId").equals(id.toString()))) {
                    d.outbox.add(mutation("task", id, "DELETE", local.version(), null));
                }
            } else {
                d.sync(user);
            }
        }
        for (int round = 0; round < 2; round++) {
            for (Device d : devices) {
                d.sync(user);
            }
        }
        Map<UUID, Local> server = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.sql("SELECT id, title, version FROM tasks WHERE user_id = :u"
                + " AND deleted_at IS NULL").param("u", user.id()).query().listOfRows()) {
            server.put((UUID) row.get("id"), new Local((String) row.get("title"), (Long) row.get("version")));
        }
        for (Device d : devices) {
            assertThat(d.tasks).as("thiết bị hội tụ về server (seed " + seed + ")").isEqualTo(server);
            assertThat(d.outbox).isEmpty();
        }
        int rows = jdbc.sql("SELECT count(*) FROM tasks WHERE user_id = :u").param("u", user.id())
                .query(Integer.class).single();
        assertThat(rows).as("mỗi CREATE đúng một dòng, không trùng").isEqualTo(created);
    }
}
