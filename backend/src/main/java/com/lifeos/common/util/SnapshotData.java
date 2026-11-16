package com.lifeos.common.util;

import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dựng {@code data} của snapshot sync từ Response REST (SYNC_PROTOCOL §6.1): cùng tập trường, cùng cách serialize
 * (cùng JsonMapper của ứng dụng — ngày giờ ISO-8601, dueTime HH:mm), bỏ {@code id} và {@code version} vì đã nằm ngoài
 * {@code data}. Giá trị trong Map là kiểu JSON thuần (chuỗi, số, boolean, null), nên lưu/phát lại được nguyên văn.
 */
public final class SnapshotData {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() {
    };

    private SnapshotData() {
    }

    public static Map<String, Object> of(JsonMapper json, Object response) {
        LinkedHashMap<String, Object> data = json.convertValue(response, MAP);
        data.remove("id");
        data.remove("version");
        return data;
    }
}
