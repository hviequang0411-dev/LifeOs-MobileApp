package com.lifeos.routine.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.routine.service.RoutineCommandService;
import com.lifeos.routine.service.RoutineCommands;
import com.lifeos.routine.service.RoutineLogCommandService;
import com.lifeos.routine.service.RoutineQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Sync cho entityType "routine" (PHASE_2 §7): xóa cascade bước, lượt, kết quả bước ở service (D246). */
@Component
public class RoutineSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "routine";

    private final RoutineCommandService commands;
    private final RoutineLogCommandService runs;
    private final RoutineQueryService queries;
    private final JsonMapper json;

    public RoutineSyncHandler(RoutineCommandService commands, RoutineLogCommandService runs, RoutineQueryService queries,
            JsonMapper json) {
        this.commands = commands;
        this.runs = runs;
        this.queries = queries;
        this.json = json;
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public String table() {
        return "routines";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, new RoutineCommands.Create(m.entityId(),
                    PayloadFields.string(p, "name"), PayloadFields.string(p, "kind"),
                    PayloadFields.integer(p, "weekdays"), PayloadFields.string(p, "reminderTime")),
                    m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(), new RoutineCommands.Update(m.baseVersion(),
                    PayloadFields.string(p, "name"), PayloadFields.string(p, "kind"),
                    PayloadFields.integer(p, "weekdays"), PayloadFields.string(p, "reminderTime")),
                    m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.routineStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
