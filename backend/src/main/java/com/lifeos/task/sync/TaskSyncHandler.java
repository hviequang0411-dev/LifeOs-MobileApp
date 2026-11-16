package com.lifeos.task.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.service.TaskCommandService;
import com.lifeos.task.service.TaskCommands;
import com.lifeos.task.service.TaskQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "task" (SYNC_PROTOCOL §3.1). Chỉ DỊCH payload → TaskCommands và gọi TaskCommandService; giá trị
 * mặc định (OPEN, NONE), trim/độ dài/enum, dueTime cần dueDate, completedAt, tham chiếu projectId/categoryId (422,
 * khóa Project trước Category) đều ở service như REST. Khóa do server quản (id, userId, version, completedAt,
 * createdAt, updatedAt, deletedAt) không được đọc nên bị bỏ qua (SYN-18).
 */
@Component
public class TaskSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "task";

    private final TaskCommandService commands;
    private final TaskQueryService queries;
    private final JsonMapper json;

    public TaskSyncHandler(TaskCommandService commands, TaskQueryService queries, JsonMapper json) {
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
        return "tasks";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> applied(commands.create(userId, new TaskCommands.Create(m.entityId(),
                    PayloadFields.string(p, "title"), PayloadFields.string(p, "description"),
                    PayloadFields.string(p, "status"), PayloadFields.string(p, "priority"),
                    PayloadFields.string(p, "dueDate"), PayloadFields.string(p, "dueTime"),
                    PayloadFields.uuid(p, "projectId"), PayloadFields.uuid(p, "categoryId"),
                    PayloadFields.uuid(p, "goalId"), PayloadFields.uuid(p, "milestoneId")), m.mutationId()));
            case UPDATE -> applied(commands.update(userId, m.entityId(), new TaskCommands.Update(m.baseVersion(),
                    PayloadFields.string(p, "title"), PayloadFields.string(p, "description"),
                    PayloadFields.string(p, "status"), PayloadFields.string(p, "priority"),
                    PayloadFields.string(p, "dueDate"), PayloadFields.string(p, "dueTime"),
                    PayloadFields.uuid(p, "projectId"), PayloadFields.uuid(p, "categoryId"),
                    PayloadFields.uuid(p, "goalId"), PayloadFields.uuid(p, "milestoneId")), m.mutationId()));
            case DELETE -> new MutationOutcome.Applied(
                    commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.syncStates(userId, entityIds).stream()
                .map(state -> new EntitySnapshot(ENTITY_TYPE, state.task().id(), state.task().version(),
                        state.deleted(), state.deleted() ? null : SnapshotData.of(json, state.task())))
                .toList();
    }

    private static MutationOutcome applied(TaskResponse response) {
        return new MutationOutcome.Applied(response.version());
    }
}
