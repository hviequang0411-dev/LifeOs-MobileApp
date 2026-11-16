package com.lifeos.task.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.task.dto.CategoryResponse;
import com.lifeos.task.service.CategoryCommandService;
import com.lifeos.task.service.CategoryCommands;
import com.lifeos.task.service.CategoryQueryService;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "category" (SYNC_PROTOCOL §3.3). Chỉ DỊCH payload → CategoryCommands và gọi
 * CategoryCommandService; trùng tên, thứ tự kiểm, cascade khi xóa đều ở service.
 */
@Component
public class CategorySyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "category";

    private final CategoryCommandService commands;
    private final CategoryQueryService queries;
    private final JsonMapper json;

    public CategorySyncHandler(CategoryCommandService commands, CategoryQueryService queries, JsonMapper json) {
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
        return "categories";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        return switch (m.operation()) {
            case CREATE -> applied(commands.create(userId,
                    new CategoryCommands.Create(m.entityId(), PayloadFields.string(m.payload(), "name")),
                    m.mutationId()));
            case UPDATE -> applied(commands.update(userId, m.entityId(),
                    new CategoryCommands.Update(m.baseVersion(), PayloadFields.string(m.payload(), "name")),
                    m.mutationId()));
            case DELETE -> new MutationOutcome.Applied(
                    commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.syncStates(userId, entityIds).stream()
                .map(state -> new EntitySnapshot(ENTITY_TYPE, state.category().id(), state.category().version(),
                        state.deleted(), state.deleted() ? null : SnapshotData.of(json, state.category())))
                .toList();
    }

    private static MutationOutcome applied(CategoryResponse response) {
        return new MutationOutcome.Applied(response.version());
    }
}
