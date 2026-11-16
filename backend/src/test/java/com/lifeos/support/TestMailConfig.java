package com.lifeos.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Thay EmailSender SMTP bằng stub trong context test dùng chung (MailpitIT dùng SMTP thật tới mailpit). */
@TestConfiguration(proxyBeanMethods = false)
public class TestMailConfig {

    @Bean
    @Primary
    public RecordingEmailSender recordingEmailSender() {
        return new RecordingEmailSender();
    }

    /** Push giả lập (WP4): context test không gọi FCM thật. */
    @Bean
    @Primary
    public RecordingPushSender recordingPushSender() {
        return new RecordingPushSender();
    }
}
