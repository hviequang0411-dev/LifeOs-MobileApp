package com.lifeos.notification.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mẫu nội dung thông báo (PHASE_4 D138, D144): file resource {@code notification/templates/{category}.{lang}.properties}
 * (UTF-8), khóa {@code <templateKey>.title|body}, biến {@code {name}} chỉ trong WHITELIST. Giá trị biến bị bỏ ký tự
 * điều khiển (CR/LF — chặn header injection ở tiêu đề email) và cắt độ dài. Ngôn ngữ: phần đầu của locale người dùng
 * ("vi-VN" → vi), thiếu thì vi.
 */
public final class Templates {

    /** Biến được phép thế (không có số tiền, ghi chú, email, token). */
    public static final Set<String> WHITELIST = Set.of("threshold", "month", "title", "time");
    private static final Pattern VAR = Pattern.compile("\\{([a-zA-Z]+)}");
    private static final int VALUE_MAX = 80;
    private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();

    public record Rendered(String title, String body) {
        @Override
        public String toString() {
            return "Rendered[]";
        }
    }

    private Templates() {
    }

    /** @throws IllegalArgumentException nếu không có mẫu cho khóa (lỗi lập trình hoặc payload hỏng) */
    public static Rendered render(String category, String locale, String templateKey, Map<String, ?> variables) {
        Properties props = load(category, language(locale));
        String title = props.getProperty(templateKey + ".title");
        String body = props.getProperty(templateKey + ".body");
        if (title == null || body == null) {
            props = load(category, "vi");
            title = props.getProperty(templateKey + ".title");
            body = props.getProperty(templateKey + ".body");
        }
        if (title == null || body == null) {
            throw new IllegalArgumentException("Không có mẫu thông báo");
        }
        return new Rendered(fill(title, variables, 200), fill(body, variables, 500));
    }

    public static boolean exists(String category, String templateKey) {
        return load(category, "vi").getProperty(templateKey + ".title") != null;
    }

    static String language(String locale) {
        if (locale == null || locale.isBlank()) {
            return "vi";
        }
        String lang = locale.split("[-_]")[0].toLowerCase();
        return lang.matches("[a-z]{2}") ? lang : "vi";
    }

    private static String fill(String template, Map<String, ?> variables, int max) {
        Matcher m = VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            Object value = WHITELIST.contains(name) ? variables.get(name) : null;
            m.appendReplacement(out, Matcher.quoteReplacement(sanitize(value)));
        }
        m.appendTail(out);
        String text = out.toString();
        return text.codePointCount(0, text.length()) > max ? text.substring(0, text.offsetByCodePoints(0, max)) : text;
    }

    /** Bỏ ký tự điều khiển (CR, LF, TAB…) và cắt {@link #VALUE_MAX} ký tự. */
    public static String sanitize(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replaceAll("\\p{Cntrl}", " ").strip();
        return text.codePointCount(0, text.length()) > VALUE_MAX
                ? text.substring(0, text.offsetByCodePoints(0, VALUE_MAX)) : text;
    }

    private static Properties load(String category, String lang) {
        return CACHE.computeIfAbsent(category + "." + lang, key -> {
            Properties props = new Properties();
            try (InputStream in = Templates.class.getResourceAsStream("/notification/templates/" + key
                    + ".properties")) {
                if (in != null) {
                    props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Không đọc được mẫu thông báo", e);
            }
            return props;
        });
    }
}
