package com.lifeos.ai.service;

import org.springframework.stereotype.Component;

@Component
public class AiOutputSanitizer {
    public String sanitize(String value) {
        if (value == null) return "";
        String clean = value.replaceAll("(?is)<script[^>]*>.*?</script>", "[blocked]")
                .replaceAll("(?is)<[^>]+>", "")
                .replaceAll("(?i)(javascript|data):\\S+", "[blocked]")
                .replaceAll("(?i)(system\\s+prompt|OPENAI_API_KEY|api[_ -]?key)", "[redacted]");
        return clean.length() <= 16_000 ? clean : clean.substring(0, 16_000);
    }
}
