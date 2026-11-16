package com.lifeos.sync.controller;

import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.Mutation;
import com.lifeos.sync.service.PushMutation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kiểm tra cấu trúc toàn request POST /sync/push (SYNC_PROTOCOL §2.1, D90, D93). Lỗi cấu trúc làm CẢ request thất bại
 * (400, không có results, không mutation nào được xử lý); vị trí lỗi trả trong fieldErrors, ví dụ
 * {@code mutations[3].entityId}. entityType LẠ (≤ 30 ký tự) không phải lỗi cấu trúc: mutation đó nhận REJECTED ở
 * service. Trường lạ ở mức mutation/payload bị bỏ qua. Chỉ kiểm hình dạng, không kiểm nghiệp vụ.
 */
@Component
class SyncPushRequestParser {

    /** Độ dài cột sync_mutations.entity_type (D93). */
    static final int ENTITY_TYPE_MAX = 30;
    private static final Pattern UUID_TEXT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() {
    };

    private final JsonMapper json;
    private final int maxBatch;

    SyncPushRequestParser(JsonMapper json, @Value("${lifeos.sync.push-max-batch}") int maxBatch) {
        this.json = json;
        this.maxBatch = maxBatch;
    }

    /** @param mutations giá trị của khóa "mutations" (null nếu vắng); thân không phải object bị Jackson chặn trước. */
    List<PushMutation> parse(JsonNode mutations) {
        if (mutations == null || mutations.isNull()) {
            throw malformed(List.of(error("mutations", "REQUIRED")));
        }
        if (!mutations.isArray()) {
            throw malformed(List.of(error("mutations", "INVALID_VALUE")));
        }
        if (mutations.isEmpty()) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "mutations", "SIZE",
                    "Lô đồng bộ phải có ít nhất một mutation.");
        }
        if (mutations.size() > maxBatch) {
            throw new BusinessException(ErrorCode.SYNC_BATCH_TOO_LARGE);
        }
        List<Map<String, Object>> errors = new ArrayList<>();
        List<PushMutation> parsed = new ArrayList<>(mutations.size());
        for (int i = 0; i < mutations.size(); i++) {
            PushMutation mutation = parseOne(mutations.get(i), "mutations[" + i + "]", errors);
            if (mutation != null) {
                parsed.add(mutation);
            }
        }
        if (!errors.isEmpty()) {
            throw malformed(errors);
        }
        return parsed;
    }

    private PushMutation parseOne(JsonNode node, String at, List<Map<String, Object>> errors) {
        if (!node.isObject()) {
            errors.add(error(at, "INVALID_VALUE"));
            return null;
        }
        int before = errors.size();
        UUID mutationId = uuid(node.get("mutationId"), at + ".mutationId", errors);
        UUID entityId = uuid(node.get("entityId"), at + ".entityId", errors);

        JsonNode typeNode = node.get("entityType");
        String entityType = null;
        if (typeNode == null || typeNode.isNull()) {
            errors.add(error(at + ".entityType", "REQUIRED"));
        } else if (!typeNode.isString() || typeNode.asString().isBlank()) {
            errors.add(error(at + ".entityType", "INVALID_VALUE"));
        } else if (typeNode.asString().length() > ENTITY_TYPE_MAX) {
            errors.add(error(at + ".entityType", "SIZE"));
        } else {
            entityType = typeNode.asString();
        }

        JsonNode operationNode = node.get("operation");
        ChangeOperation operation = null;
        if (operationNode == null || operationNode.isNull()) {
            errors.add(error(at + ".operation", "REQUIRED"));
        } else {
            operation = operationNode.isString() ? operation(operationNode.asString()) : null;
            if (operation == null) {
                errors.add(error(at + ".operation", "INVALID_VALUE"));
            }
        }

        JsonNode baseNode = node.get("baseVersion");
        long baseVersion = 0;
        if (baseNode == null || baseNode.isNull()) {
            errors.add(error(at + ".baseVersion", "REQUIRED"));
        } else if (!baseNode.isIntegralNumber() || !baseNode.canConvertToLong() || baseNode.asLong() < 0) {
            errors.add(error(at + ".baseVersion", "INVALID_VALUE"));
        } else {
            baseVersion = baseNode.asLong();
        }

        // payload vắng mặt hoặc null = {} (D93); có mặt nhưng không phải object → lỗi cấu trúc.
        JsonNode payloadNode = node.get("payload");
        Map<String, Object> payload = Map.of();
        int payloadBytes = 0;
        if (payloadNode != null && !payloadNode.isNull()) {
            if (!payloadNode.isObject()) {
                errors.add(error(at + ".payload", "INVALID_VALUE"));
            } else {
                payloadBytes = json.writeValueAsBytes(payloadNode).length;
                payload = json.convertValue(payloadNode, MAP);
            }
        }
        if (errors.size() > before) {
            return null;
        }
        return new PushMutation(new Mutation(mutationId, entityType, entityId, operation, baseVersion, payload),
                payloadBytes);
    }

    private static UUID uuid(JsonNode node, String field, List<Map<String, Object>> errors) {
        if (node == null || node.isNull()) {
            errors.add(error(field, "REQUIRED"));
            return null;
        }
        if (!node.isString() || !UUID_TEXT.matcher(node.asString()).matches()) {
            errors.add(error(field, "INVALID_VALUE"));
            return null;
        }
        return UUID.fromString(node.asString());
    }

    private static ChangeOperation operation(String text) {
        for (ChangeOperation op : ChangeOperation.values()) {
            if (op.name().equals(text)) {
                return op;
            }
        }
        return null;
    }

    private static Map<String, Object> error(String field, String code) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("field", field);
        error.put("code", code);
        error.put("message", "Sai cấu trúc.");
        return error;
    }

    /** MALFORMED_REQUEST kèm vị trí trong fieldErrors — ngoại lệ có chủ ý so với API_CONVENTIONS §5 (D93). */
    private static ValidationException malformed(List<Map<String, Object>> errors) {
        ValidationException ex = new ValidationException(ErrorCode.MALFORMED_REQUEST, HttpStatus.BAD_REQUEST,
                ErrorCode.MALFORMED_REQUEST.title());
        ex.with(FieldErrors.PROPERTY, List.copyOf(errors));
        return ex;
    }
}
