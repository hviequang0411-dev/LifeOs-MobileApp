package com.lifeos.identity.sync;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.identity.dto.SettingsResponse;
import com.lifeos.identity.service.UpdateSettingsCommand;
import com.lifeos.identity.service.UserSettingsCommandService;
import com.lifeos.identity.service.UserSettingsQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "user_settings" (SYNC_PROTOCOL §3.4, D88). Mỗi user đúng một dòng, tạo lúc đăng ký; chỉ cho
 * UPDATE (CREATE/DELETE → SYNC_OPERATION_NOT_ALLOWED, do module sync kiểm qua {@link #supports}). entityId không phải
 * dòng settings của user hiện tại → USER_SETTINGS_NOT_FOUND (cùng kết quả cho không tồn tại và của user khác) — đây là
 * bước "tồn tại" của thứ tự D96; version và giá trị do UserSettingsCommandService kiểm như PATCH /me/settings.
 */
@Component
public class UserSettingsSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "user_settings";

    private final UserSettingsCommandService commands;
    private final UserSettingsQueryService queries;
    private final JsonMapper json;

    public UserSettingsSyncHandler(UserSettingsCommandService commands, UserSettingsQueryService queries,
            JsonMapper json) {
        this.commands = commands;
        this.queries = queries;
        this.json = json;
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public String table() {
        return "user_settings";
    }

    @Override
    public boolean supports(ChangeOperation operation) {
        return operation == ChangeOperation.UPDATE;
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        if (!queries.get(userId).id().equals(m.entityId())) {
            return new MutationOutcome.Rejected(ErrorCode.USER_SETTINGS_NOT_FOUND);
        }
        Map<String, Object> p = m.payload();
        SettingsResponse updated = commands.update(userId, new UpdateSettingsCommand(m.baseVersion(),
                PayloadFields.string(p, "timezone"), PayloadFields.string(p, "locale"),
                PayloadFields.string(p, "defaultCurrency"), PayloadFields.integer(p, "weekStartDay"),
                PayloadFields.string(p, "quietStart"), PayloadFields.string(p, "quietEnd"),
                PayloadFields.bool(p, "showDetailsOnLockScreen")), m.mutationId());
        return new MutationOutcome.Applied(updated.version());
    }

    /** Dòng settings không bao giờ bị xóa mềm; chỉ trả khi id trùng dòng của chính user. */
    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        SettingsResponse settings = queries.get(userId);
        if (!entityIds.contains(settings.id())) {
            return List.of();
        }
        return List.of(new EntitySnapshot(ENTITY_TYPE, settings.id(), settings.version(), false,
                SnapshotData.of(json, settings)));
    }
}
