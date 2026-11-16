package com.lifeos.habit.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.habit.service.CheckinCommandService;
import com.lifeos.habit.service.HabitCommands;
import com.lifeos.habit.service.HabitQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "habit_checkin" (PHASE_2 §7, D235): CREATE trùng (habit, ngày) với check-in đang có → CONFLICT
 * {@code ALREADY_EXISTS} kèm bản đang có (service ném NaturalKeyConflictException); habitId/localDate bất biến.
 */
@Component
public class HabitCheckinSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "habit_checkin";

    private final CheckinCommandService commands;
    private final HabitQueryService queries;
    private final JsonMapper json;

    public HabitCheckinSyncHandler(CheckinCommandService commands, HabitQueryService queries, JsonMapper json) {
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
        return "habit_checkins";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.createForSync(userId,
                    new HabitCommands.CreateCheckin(m.entityId(), PayloadFields.uuid(p, "habitId"),
                            PayloadFields.string(p, "localDate"), PayloadFields.string(p, "status"),
                            PayloadFields.string(p, "note")), m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.updateForSync(userId, m.entityId(),
                    new HabitCommands.UpdateCheckin(m.baseVersion(), PayloadFields.uuid(p, "habitId"),
                            PayloadFields.string(p, "localDate"), PayloadFields.string(p, "status"),
                            PayloadFields.string(p, "note")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(
                    commands.deleteForSync(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.checkinSyncStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.checkin().id(), s.checkin().version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.checkin())))
                .toList();
    }
}
