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

/** Sync cho entityType "routine_log" (D242): một lượt/ngày (CREATE trùng → CONFLICT ALREADY_EXISTS); trường bất biến. */
@Component
public class RoutineLogSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "routine_log";

    private final RoutineCommandService commands;
    private final RoutineLogCommandService runs;
    private final RoutineQueryService queries;
    private final JsonMapper json;

    public RoutineLogSyncHandler(RoutineCommandService commands, RoutineLogCommandService runs, RoutineQueryService queries,
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
        return "routine_logs";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(runs.startForSync(userId, new RoutineCommands.StartLog(m.entityId(),
                    PayloadFields.uuid(p, "routineId"), PayloadFields.string(p, "localDate"),
                    PayloadFields.integer(p, "totalSteps")), m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(runs.updateLogForSync(userId, m.entityId(), new RoutineCommands.UpdateLog(m.baseVersion(),
                    PayloadFields.uuid(p, "routineId"), PayloadFields.string(p, "localDate"),
                    PayloadFields.integer(p, "totalSteps"))).version());
            case DELETE -> new MutationOutcome.Applied(runs.deleteLogForSync(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.logStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
