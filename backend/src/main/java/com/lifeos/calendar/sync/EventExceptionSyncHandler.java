package com.lifeos.calendar.sync;

import com.lifeos.calendar.service.CalendarCommands;
import com.lifeos.calendar.service.CalendarQueryService;
import com.lifeos.calendar.service.EventExceptionCommandService;
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

/** Sync cho entityType "event_exception" (PHASE_4 §7). Chỉ dịch payload; khóa do server quản (version…) bị bỏ qua. */
@Component
public class EventExceptionSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "event_exception";

    private final EventExceptionCommandService commands;
    private final CalendarQueryService queries;
    private final JsonMapper json;

    public EventExceptionSyncHandler(EventExceptionCommandService commands, CalendarQueryService queries, JsonMapper json) {
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
        return "event_exceptions";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, null, new CalendarCommands.CreateException(m.entityId(), fields(p)),
                    m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(), new CalendarCommands.UpdateException(m.baseVersion(),
                    fields(p)), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(commands.delete(userId, null, m.entityId(), m.baseVersion(), m.mutationId()));
        };
    }

    private static CalendarCommands.ExceptionFields fields(Map<String, Object> p) {
        return new CalendarCommands.ExceptionFields(PayloadFields.uuid(p, "eventId"),
                PayloadFields.string(p, "originalStartLocal"), PayloadFields.string(p, "kind"),
                PayloadFields.string(p, "title"), PayloadFields.string(p, "description"),
                PayloadFields.string(p, "location"), PayloadFields.string(p, "startLocal"),
                PayloadFields.string(p, "endLocal"));
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.syncStates(ENTITY_TYPE, userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.response())))
                .toList();
    }
}
