package com.lifeos.finance.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.finance.service.SavingCommandService;
import com.lifeos.finance.service.FinanceCommands;
import com.lifeos.finance.service.FinanceQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "saving_goal" (PHASE_3 §8, D115). Chỉ dịch payload → FinanceCommands; quy tắc ở service. Khóa do
 * server quản (ACHIEVED, version…) không được đọc (SYN-18). Mọi lệch baseVersion → CONFLICT, không hợp nhất (D105).
 */
@Component
public class SavingGoalSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "saving_goal";

    private final SavingCommandService commands;
    private final FinanceQueryService queries;
    private final JsonMapper json;

    public SavingGoalSyncHandler(SavingCommandService commands, FinanceQueryService queries, JsonMapper json) {
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
        return "saving_goals";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.createGoal(userId, new FinanceCommands.CreateGoal(m.entityId(), PayloadFields.string(p, "name"), PayloadFields.string(p, "currency"),
                            PayloadFields.string(p, "targetAmount"), PayloadFields.string(p, "targetOn"),
                            PayloadFields.string(p, "status")),
                    m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.updateGoal(userId, m.entityId(),
                    new FinanceCommands.UpdateGoal(m.baseVersion(), PayloadFields.string(p, "name"), PayloadFields.string(p, "currency"),
                            PayloadFields.string(p, "targetAmount"), PayloadFields.string(p, "targetOn"),
                            PayloadFields.string(p, "status")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(
                    commands.deleteGoal(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.syncStates(ENTITY_TYPE, userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.response())))
                .toList();
    }
}
