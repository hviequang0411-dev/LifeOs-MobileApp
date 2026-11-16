package com.lifeos.note.sync;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.note.dto.NoteSyncState;
import com.lifeos.note.service.TagCommandService;
import com.lifeos.note.service.NoteCommands;
import com.lifeos.note.service.NoteQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Sync cho entityType "tag" (D184): CREATE trùng tên → CONFLICT ALREADY_EXISTS kèm thẻ đang có. */
@Component
public class TagSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "tag";

    private final TagCommandService commands;
    private final NoteQueryService queries;
    private final JsonMapper json;

    public TagSyncHandler(TagCommandService commands, NoteQueryService queries, JsonMapper json) {
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
        return "tags";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, new NoteCommands.CreateTag(m.entityId(),
                    PayloadFields.string(p, "name"), PayloadFields.string(p, "color")), m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(),
                    new NoteCommands.UpdateTag(m.baseVersion(), PayloadFields.string(p, "name"),
                            PayloadFields.string(p, "color")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(commands.delete(userId, m.entityId(), m.baseVersion(),
                    m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return snapshots(queries.tagStates(userId, entityIds));
    }

    private List<EntitySnapshot> snapshots(List<NoteSyncState> states) {
        return states.stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
