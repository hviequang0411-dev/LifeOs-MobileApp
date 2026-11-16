package com.lifeos.note.sync;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.note.dto.NoteSyncState;
import com.lifeos.note.service.NoteCommandService;
import com.lifeos.note.service.NoteCommands;
import com.lifeos.note.service.NoteQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Sync cho entityType "note" (D180, D181): payload riêng ≤ 192 KB; pinned/archived là cờ, archivedAt do server. */
@Component
public class NoteSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "note";

    private final NoteCommandService commands;
    private final NoteQueryService queries;
    private final JsonMapper json;

    public NoteSyncHandler(NoteCommandService commands, NoteQueryService queries, JsonMapper json) {
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
        return "notes";
    }

    /** D181 / BACKEND_MASTER_PLAN §5 #3: payload note tối đa 192 KB (50 000 ký tự tiếng Việt ≈ 150 KB UTF-8). */
    public static final int MAX_PAYLOAD_BYTES = 192 * 1024;

    @Override
    public int maxPayloadBytes(int defaultMaxBytes) {
        return Math.max(defaultMaxBytes, MAX_PAYLOAD_BYTES);
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, new NoteCommands.Create(m.entityId(),
                    PayloadFields.string(p, "title"), PayloadFields.string(p, "content"),
                    PayloadFields.bool(p, "pinned"), PayloadFields.bool(p, "archived")), m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(),
                    new NoteCommands.Update(m.baseVersion(), PayloadFields.string(p, "title"),
                            PayloadFields.string(p, "content"), PayloadFields.bool(p, "pinned"),
                            PayloadFields.bool(p, "archived")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(commands.delete(userId, m.entityId(), m.baseVersion(),
                    m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return snapshots(queries.noteStates(userId, entityIds));
    }

    private List<EntitySnapshot> snapshots(List<NoteSyncState> states) {
        return states.stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
