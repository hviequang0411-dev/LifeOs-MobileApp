package com.lifeos.goal.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.goal.service.GoalCommands;
import com.lifeos.goal.service.GoalQueryService;
import com.lifeos.goal.service.MilestoneCommandService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Sync cho entityType "milestone" (D187): goalId tham chiếu (422 GOAL_NOT_FOUND), bất biến; completed → completedAt. */
@Component
public class MilestoneSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "milestone";

    private final MilestoneCommandService commands;
    private final GoalQueryService queries;
    private final JsonMapper json;

    public MilestoneSyncHandler(MilestoneCommandService commands, GoalQueryService queries, JsonMapper json) {
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
        return "milestones";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId,
                    new GoalCommands.CreateMilestone(m.entityId(), PayloadFields.uuid(p, "goalId"),
                            PayloadFields.string(p, "title"), PayloadFields.string(p, "dueOn"),
                            PayloadFields.longValue(p, "position"), PayloadFields.bool(p, "completed")),
                    false, m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(),
                    new GoalCommands.UpdateMilestone(m.baseVersion(), PayloadFields.uuid(p, "goalId"),
                            PayloadFields.string(p, "title"), PayloadFields.string(p, "dueOn"),
                            PayloadFields.longValue(p, "position"), PayloadFields.bool(p, "completed")),
                    m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(commands.delete(userId, m.entityId(), m.baseVersion(),
                    m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.milestoneStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
