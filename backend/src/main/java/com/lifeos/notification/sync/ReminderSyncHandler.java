package com.lifeos.notification.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.notification.service.ReminderCommandService;
import com.lifeos.notification.service.ReminderQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "reminder" (PHASE_4 §7, D130): client gửi mode/offsetMinutes/localTime/localDate/repeatRule/
 * urgent/paused; nextFireAt, lastFiredAt, status DONE do server (bỏ qua nếu gửi).
 */
@Component
public class ReminderSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "reminder";

    private final ReminderCommandService commands;
    private final ReminderQueryService queries;
    private final JsonMapper json;

    public ReminderSyncHandler(ReminderCommandService commands, ReminderQueryService queries, JsonMapper json) {
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
        return "reminders";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, m.entityId(),
                    PayloadFields.string(p, "targetType"), PayloadFields.uuid(p, "targetId"), fields(p),
                    m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(), m.baseVersion(),
                    PayloadFields.string(p, "targetType"), PayloadFields.uuid(p, "targetId"), fields(p),
                    m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(
                    commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    private static ReminderCommandService.Fields fields(Map<String, Object> p) {
        return new ReminderCommandService.Fields(PayloadFields.string(p, "mode"),
                PayloadFields.integer(p, "offsetMinutes"), PayloadFields.string(p, "localTime"),
                PayloadFields.string(p, "localDate"), PayloadFields.object(p, "repeatRule"),
                PayloadFields.bool(p, "urgent"), PayloadFields.bool(p, "paused"));
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.syncStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.reminder().id(), s.reminder().version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.reminder())))
                .toList();
    }
}
