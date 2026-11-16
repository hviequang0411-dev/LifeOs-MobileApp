package com.lifeos.habit;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.MvcResult;

/** Hỗ trợ test Habit: tạo qua REST, đặt múi giờ người dùng, đọc dòng DB. */
abstract class HabitTestBase extends ApiTestBase {

    static final String HABITS = "/api/v1/habits";

    /** Tạo Habit DAILY bắt đầu từ startOn (hoặc trường bổ sung). Trả id. */
    UUID habit(TestUser user, Object... extra) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult result = createHabit(user, id, extra);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    MvcResult createHabit(TestUser user, UUID id, Object... extra) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id.toString());
        body.put("name", "Chạy bộ");
        body.put("scheduleType", "DAILY");
        body.put("startOn", today(user).minusDays(30).toString());
        for (int i = 0; i < extra.length; i += 2) {
            body.put((String) extra[i], extra[i + 1]);
        }
        return post(user, HABITS, json.writeValueAsString(body));
    }

    MvcResult checkin(TestUser user, UUID habitId, LocalDate date, String status) throws Exception {
        return call(user, org.springframework.http.HttpMethod.PUT, HABITS + "/" + habitId + "/checkins/" + date,
                body("status", status));
    }

    /** Dòng user_settings với múi giờ cho trước (user test tạo bằng SQL chưa có dòng này). */
    void timezone(TestUser user, String zone) {
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, :z)"
                        + " ON CONFLICT (user_id) DO UPDATE SET timezone = EXCLUDED.timezone")
                .param("id", UUID.randomUUID()).param("u", user.id()).param("z", zone).update();
    }

    LocalDate today(TestUser user) {
        String zone = jdbc.sql("SELECT coalesce((SELECT timezone FROM user_settings WHERE user_id = :u),"
                + " 'Asia/Ho_Chi_Minh')").param("u", user.id()).query(String.class).single();
        return LocalDate.ofInstant(clock.instant(), ZoneId.of(zone));
    }

    int liveCheckins(UUID habitId, LocalDate date) {
        return jdbc.sql("SELECT count(*) FROM habit_checkins WHERE habit_id = :h AND local_date = :d"
                + " AND deleted_at IS NULL").param("h", habitId).param("d", date).query(Integer.class).single();
    }

    Map<String, Object> row(String table, UUID id) {
        return jdbc.sql("SELECT * FROM " + table + " WHERE id = :id").param("id", id).query().singleRow();
    }
}
