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

/** Sync cho entityType "routine_step_log" (D243): trùng (lượt, bước) → CONFLICT ALREADY_EXISTS; completedAt của lượt do server. */
@Component
public class RoutineStepLogSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "routine_step_log";

    private final RoutineCommandService commands;
    private final RoutineLogCommandService runs;
    private final RoutineQueryService queries;
    private final JsonMapper json;

    public RoutineStepLogSyncHandler(RoutineCommandService commands, RoutineLogCommandService runs, RoutineQueryService queries,
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
        return "routine_step_logs";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(runs.createStepLogForSync(userId, new RoutineCommands.CreateStepLog(m.entityId(),
                    PayloadFields.uuid(p, "routineLogId"), PayloadFields.uuid(p, "stepId"),
                    PayloadFields.string(p, "status")), m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(runs.updateStepLogForSync(userId, m.entityId(), new RoutineCommands.UpdateStepLog(m.baseVersion(),
                    PayloadFields.uuid(p, "routineLogId"), PayloadFields.uuid(p, "stepId"),
                    PayloadFields.string(p, "status")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(runs.deleteStepLogForSync(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.stepLogStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
