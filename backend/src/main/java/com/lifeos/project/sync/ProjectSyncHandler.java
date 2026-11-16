package com.lifeos.project.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.project.dto.ProjectResponse;
import com.lifeos.project.service.ProjectCommandService;
import com.lifeos.project.service.ProjectCommands;
import com.lifeos.project.service.ProjectQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "project" (SYNC_PROTOCOL §3.2). Chỉ DỊCH: payload → ProjectCommands, gọi ProjectCommandService
 * (mọi quy tắc, kể cả thứ tự tồn tại → version → giá trị, ở service), kết quả → Applied. Lỗi nghiệp vụ của service để
 * nguyên cho module sync ánh xạ. Khóa do server quản (id, userId, version, archivedAt, createdAt, updatedAt,
 * deletedAt) không được đọc nên bị bỏ qua (SYN-18).
 */
@Component
public class ProjectSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "project";

    private final ProjectCommandService commands;
    private final ProjectQueryService queries;
    private final JsonMapper json;

    public ProjectSyncHandler(ProjectCommandService commands, ProjectQueryService queries, JsonMapper json) {
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
        return "projects";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> payload = m.payload();
        return switch (m.operation()) {
            case CREATE -> applied(commands.create(userId, new ProjectCommands.Create(m.entityId(),
                    PayloadFields.string(payload, "name"), PayloadFields.string(payload, "description")),
                    m.mutationId()));
            case UPDATE -> applied(commands.update(userId, m.entityId(), new ProjectCommands.Update(m.baseVersion(),
                    PayloadFields.string(payload, "name"), PayloadFields.string(payload, "description"),
                    PayloadFields.bool(payload, "archived")), m.mutationId()));
            case DELETE -> new MutationOutcome.Applied(
                    commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.syncStates(userId, entityIds).stream()
                .map(state -> snapshot(state.project(), state.deleted()))
                .toList();
    }

    private EntitySnapshot snapshot(ProjectResponse project, boolean deleted) {
        return new EntitySnapshot(ENTITY_TYPE, project.id(), project.version(), deleted,
                deleted ? null : SnapshotData.of(json, project));
    }

    private static MutationOutcome applied(ProjectResponse response) {
        return new MutationOutcome.Applied(response.version());
    }
}
