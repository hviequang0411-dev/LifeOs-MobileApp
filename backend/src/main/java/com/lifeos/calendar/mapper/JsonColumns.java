package com.lifeos.calendar.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Chuyển cột jsonb (chuỗi JSON chuẩn) ↔ Map cho mapper/service. */
@Component
public class JsonColumns {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() {
    };

    private final JsonMapper json;

    public JsonColumns(JsonMapper json) {
        this.json = json;
    }

    public Map<String, Object> toMap(String raw) {
        return raw == null ? null : json.readValue(raw, MAP);
    }

    public String toJson(Map<String, Object> map) {
        return map == null ? null : json.writeValueAsString(map);
    }
}
