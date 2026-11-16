package com.lifeos.calendar.sync;

import com.lifeos.calendar.service.CalendarCommands;
import com.lifeos.calendar.service.CalendarQueryService;
import com.lifeos.calendar.service.TimeBlockCommandService;
import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Sync cho entityType "time_block" (PHASE_4 §7). Chỉ dịch payload; khóa do server quản (version…) bị bỏ qua. */
@Component
public class TimeBlockSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "time_block";

    private final TimeBlockCommandService commands;
    private final CalendarQueryService queries;
    private final JsonMapper json;

    public TimeBlockSyncHandler(TimeBlockCommandService commands, CalendarQueryService queries, JsonMapper json) {
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
        return "time_blocks";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, new CalendarCommands.CreateTimeBlock(m.entityId(),
                    PayloadFields.uuid(p, "taskId"), PayloadFields.string(p, "startAt"),
                    PayloadFields.string(p, "endAt"), PayloadFields.string(p, "note")), m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(), new CalendarCommands.UpdateTimeBlock(m.baseVersion(),
                    PayloadFields.uuid(p, "taskId"), PayloadFields.string(p, "startAt"),
                    PayloadFields.string(p, "endAt"), PayloadFields.string(p, "note")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
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
