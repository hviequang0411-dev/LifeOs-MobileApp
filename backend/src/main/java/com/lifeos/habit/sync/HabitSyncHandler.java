package com.lifeos.habit.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.habit.service.HabitCommandService;
import com.lifeos.habit.service.HabitCommands;
import com.lifeos.habit.service.HabitQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "habit" (PHASE_2 §7). Chỉ dịch payload → HabitCommands; quy tắc (lịch D230, giới hạn D237,
 * cascade xóa check-in D236) ở service. Khóa do server quản (archivedAt, version…) không được đọc (SYN-18).
 */
@Component
public class HabitSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "habit";

    private final HabitCommandService commands;
    private final HabitQueryService queries;
    private final JsonMapper json;

    public HabitSyncHandler(HabitCommandService commands, HabitQueryService queries, JsonMapper json) {
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
        return "habits";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, new HabitCommands.Create(m.entityId(),
                    PayloadFields.string(p, "name"), PayloadFields.string(p, "description"),
                    PayloadFields.string(p, "color"), PayloadFields.string(p, "icon"),
                    PayloadFields.string(p, "scheduleType"), PayloadFields.integer(p, "weekdays"),
                    PayloadFields.integer(p, "timesPerWeek"), PayloadFields.integer(p, "intervalDays"),
                    PayloadFields.string(p, "startOn"), PayloadFields.string(p, "endOn"),
                    PayloadFields.string(p, "reminderTime")), m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(),
                    new HabitCommands.Update(m.baseVersion(),
                            PayloadFields.string(p, "name"), PayloadFields.string(p, "description"),
                            PayloadFields.string(p, "color"), PayloadFields.string(p, "icon"),
                            PayloadFields.string(p, "scheduleType"), PayloadFields.integer(p, "weekdays"),
                            PayloadFields.integer(p, "timesPerWeek"), PayloadFields.integer(p, "intervalDays"),
                            PayloadFields.string(p, "startOn"), PayloadFields.string(p, "endOn"),
                            PayloadFields.string(p, "reminderTime")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(
                    commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.habitSyncStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.habit().id(), s.habit().version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.habit())))
                .toList();
    }
}
