package com.lifeos.common.mail;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình email ứng dụng (SYSTEM_ARCHITECTURE §3.14: MAIL_FROM, APP_BASE_URL). Liên kết trong email CHỈ dựng từ
 * appBaseUrl, không từ header Host (SECURITY_RULES §3.4). appBaseUrl phải là https, trừ máy dev/test (localhost,
 * 127.0.0.1, *.localhost, *.test) — token trong liên kết chỉ đi qua HTTPS (SECURITY_RULES §13.6). Sai cấu hình →
 * app không khởi động.
 */
@ConfigurationProperties(prefix = "lifeos.mail")
public record MailProperties(String from, String appBaseUrl) {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1");

    public MailProperties {
        if (from == null || from.isBlank() || from.indexOf('\r') >= 0 || from.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("lifeos.mail.from (MAIL_FROM) là bắt buộc");
        }
        URI uri;
        try {
            uri = URI.create(appBaseUrl == null ? "" : appBaseUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("lifeos.mail.app-base-url (APP_BASE_URL) không hợp lệ");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean local = LOCAL_HOSTS.contains(host) || host.endsWith(".localhost") || host.endsWith(".test");
        boolean secure = "https".equalsIgnoreCase(uri.getScheme());
        if (host.isEmpty() || uri.getQuery() != null || uri.getFragment() != null
                || !(secure || ("http".equalsIgnoreCase(uri.getScheme()) && local))) {
            throw new IllegalArgumentException(
                    "lifeos.mail.app-base-url (APP_BASE_URL) phải là URL https tuyệt đối (http chỉ cho máy dev/test)");
        }
        appBaseUrl = appBaseUrl.endsWith("/") ? appBaseUrl.substring(0, appBaseUrl.length() - 1) : appBaseUrl;
    }
}
