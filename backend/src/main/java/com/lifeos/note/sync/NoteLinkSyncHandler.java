package com.lifeos.note.sync;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.note.dto.NoteSyncState;
import com.lifeos.note.service.NoteRelationService;
import com.lifeos.note.service.NoteQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Sync cho entityType "note_link" (D189): chỉ CREATE/DELETE; mục tiêu TASK/GOAL của user (sai → 422). */
@Component
public class NoteLinkSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "note_link";

    private final NoteRelationService commands;
    private final NoteQueryService queries;
    private final JsonMapper json;

    public NoteLinkSyncHandler(NoteRelationService commands, NoteQueryService queries, JsonMapper json) {
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
        return "note_links";
    }

    /** Chỉ CREATE/DELETE (UPDATE → REJECTED SYNC_OPERATION_NOT_ALLOWED, §7). */
    @Override
    public boolean supports(ChangeOperation operation) {
        return operation != ChangeOperation.UPDATE;
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.addLink(userId, m.entityId(),
                    PayloadFields.uuid(p, "noteId").value(), PayloadFields.string(p, "targetType").value(),
                    PayloadFields.uuid(p, "targetId").value(), false, m.mutationId()).value().version());
            case DELETE -> new MutationOutcome.Applied(commands.removeLink(userId, null, m.entityId(),
                    m.baseVersion(), m.mutationId()));
            case UPDATE -> throw new IllegalStateException("UPDATE bị chặn ở supports()");
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return snapshots(queries.linkStates(userId, entityIds));
    }

    private List<EntitySnapshot> snapshots(List<NoteSyncState> states) {
        return states.stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
