package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.read.ListAppender;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * LOG-01 / SEC-11 (SECURITY_RULES §11.1) và SYN-47 (SYNC_PROTOCOL §7.2) cho CẢ log dạng chữ (profile dev) lẫn log JSON
 * (profile prod, ECS).
 * <p>
 * Định dạng console của Logback không đổi được theo từng context trong cùng JVM test (Spring Boot đánh dấu
 * LoggerContext đã khởi tạo), nên test không dựa vào định dạng đang áp dụng: nó thu mọi sự kiện log của các luồng
 * bằng ListAppender (mức DEBUG cho com.lifeos và Spring MVC — Spring MVC in cả DTO đọc/ghi body), rồi render từng sự
 * kiện bằng đúng hai encoder của ứng dụng — PatternLayout với mẫu của application-dev.yml và StructuredLogEncoder
 * "ecs" của application-prod.yml — và quét cả hai bản render lẫn stdout thô. Ngoài mật khẩu/token, test kiểm cả địa
 * chỉ email (PII) không xuất hiện, gồm luồng hai đăng ký đồng thời cùng email (vi phạm unique, D76).
 */
@ExtendWith(OutputCaptureExtension.class)
class SensitiveLogIT extends SensitiveLogScenario {

    @org.springframework.beans.factory.annotation.Autowired
    com.lifeos.common.outbox.OutboxWorker outboxWorker;

    /** Mẫu log của application-dev.yml. */
    private static final String DEV_PATTERN =
            "%d{HH:mm:ss.SSS} %-5level [%X{correlationId:-}] %logger{36} - %msg %kvp%n";

    /** Log đã thu, render theo hai định dạng của ứng dụng. */
    private record Captured<T>(T result, List<ILoggingEvent> events, String text, String json) {
    }

    private <T> Captured<T> capture(Callable<T> flows) throws Exception {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
        Map<String, Level> verbose = Map.of("com.lifeos", Level.DEBUG, "org.springframework.web", Level.DEBUG);
        Map<String, Level> previous = new java.util.HashMap<>();
        verbose.keySet().forEach(name -> previous.put(name, ctx.getLogger(name).getLevel()));

        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.setContext(ctx);
        captured.start();
        root.addAppender(captured);
        verbose.forEach((name, level) -> ctx.getLogger(name).setLevel(level));
        T result;
        try {
            result = flows.call();
        } finally {
            root.detachAppender(captured);
            previous.forEach((name, level) -> ctx.getLogger(name).setLevel(level));
        }

        PatternLayoutEncoder text = new PatternLayoutEncoder();
        text.setContext(ctx);
        text.setPattern(DEV_PATTERN);
        text.setCharset(StandardCharsets.UTF_8);
        text.start();
        StructuredLogEncoder json = new StructuredLogEncoder();
        json.setContext(ctx);
        json.setFormat("ecs");
        json.setCharset(StandardCharsets.UTF_8);
        json.start();
        return new Captured<>(result, captured.list, render(captured.list, text), render(captured.list, json));
    }

    @Test
    void authFlowsNeverLogPasswordsOrTokensInTextOrJsonLogs(CapturedOutput output) throws Exception {
        Captured<List<List<String>>> log = capture(() -> {
            List<String> secrets = runSensitiveFlows();
            runConcurrentDuplicateRegistration();
            List<String> projectContent = new java.util.ArrayList<>(runProjectFlows());
            projectContent.addAll(runHabitRoutineFlows());
            projectContent.addAll(runFinanceFlows());
            projectContent.addAll(runCalendarNotificationFlows());
            projectContent.addAll(runKnowledgeGoalFlows());
            projectContent.addAll(runAnalyticsAccountFlows());
            // Giai đoạn 4: worker gửi các email vừa sinh (token thô, địa chỉ email trong payload) — log của worker
            // và handler cũng phải sạch.
            for (int i = 0; i < 1_000 && outboxWorker.runOnce() > 0; i++) {
                // tiếp tục
            }
            return List.of(secrets, projectContent);
        });
        List<String> secrets = log.result().get(0);
        List<String> userContent = log.result().get(1);
        String textLog = log.text();
        String jsonLog = log.json();

        assertThat(log.events()).as("đã thu được log DEBUG của luồng auth").hasSizeGreaterThan(20);
        assertThat(textLog).as("bản render dạng chữ")
                .containsPattern("\\d{2}:\\d{2}:\\d{2}\\.\\d{3} INFO  \\[[0-9a-f-]{36}] .*security_event");
        assertThat(jsonLog).as("bản render JSON")
                .containsPattern("\\{\"@timestamp\".*\"message\":\"security_event");
        assertThat(textLog).as("Spring MVC DEBUG có in DTO request (đã che)").contains("RegisterRequest[");
        assertThat(textLog).as("log của outbox worker đã được thu").contains("outbox_event");

        assertNoSecretIn(textLog, secrets, "log chữ");
        assertNoSecretIn(jsonLog, secrets, "log JSON");
        assertNoSecretIn(output.getAll(), secrets, "stdout thô");

        // Email là PII (CODE_GUIDELINES §14), kể cả trong lỗi SQL của Hibernate khi hai đăng ký trùng email (D76).
        assertNoEmailIn(textLog, emailsUsed, "log chữ");
        assertNoEmailIn(jsonLog, emailsUsed, "log JSON");
        assertNoEmailIn(output.getAll(), emailsUsed, "stdout thô");
        // Q18: name/description của Project không xuất hiện (Spring MVC DEBUG in DTO qua toString đã che).
        assertThat(textLog).as("Spring MVC DEBUG có in DTO Project (đã che)").contains("CreateProjectRequest[");
        assertNoSecretIn(textLog, userContent, "log chữ (nội dung Project)");
        assertNoSecretIn(jsonLog, userContent, "log JSON (nội dung Project)");
        assertNoSecretIn(output.getAll(), userContent, "stdout thô (nội dung Project)");
        assertThat(((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.hibernate.orm.jdbc.error")
                .getEffectiveLevel()).as("cấu hình logging.level.org.hibernate.orm.jdbc.error").isEqualTo(Level.ERROR);
    }

    /** SYN-47: luồng sync chỉ log userId/correlationId/số lượng/thời gian; không payload, title/description/name, token. */
    @Test
    void Sync_47_syncFlowsNeverLogPayloadContentOrTokensInTextOrJsonLogs(CapturedOutput output) throws Exception {
        Captured<List<String>> log = capture(this::runSyncFlows);
        List<String> syncContent = log.result();

        assertThat(log.text()).as("có log sự kiện sync").contains("sync_push").contains("sync_pull")
                .contains("sync_mutation_id_reuse");
        assertThat(log.json()).contains("\"event\":\"sync_push\"").contains("\"event\":\"sync_mutation_id_reuse\"");
        assertThat(log.text()).as("Spring MVC DEBUG có in thân/phản hồi sync (đã che)")
                .contains("PushRequest[mutations=").contains("MutationResult[mutationId=")
                .contains("PullChange[entityType=");
        assertAbsent(log.text(), syncContent, "log chữ (luồng sync)");
        assertAbsent(log.json(), syncContent, "log JSON (luồng sync)");
        assertAbsent(output.getAll(), syncContent, "stdout thô (luồng sync)");
    }

    private static void assertAbsent(String log, List<String> values, String label) {
        assertThat(log).as(label + ": phải có nội dung để phép quét có ý nghĩa").contains("sync_push");
        for (String value : values) {
            for (String form : forms(value)) {
                assertThat(log).as("%s chứa nội dung/bí mật (bắt đầu bằng %s…)", label,
                        form.substring(0, Math.min(6, form.length()))).doesNotContain(form);
            }
        }
        assertThat(log).as(label).doesNotContain("Bearer ey");
    }

    private static String render(List<ILoggingEvent> events, Encoder<ILoggingEvent> encoder) {
        StringBuilder sb = new StringBuilder();
        for (ILoggingEvent event : events) {
            sb.append(new String(encoder.encode(event), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
