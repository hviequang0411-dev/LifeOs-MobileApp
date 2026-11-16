package com.lifeos.analytics.sync;

import com.lifeos.analytics.service.ReviewCommandService;
import com.lifeos.analytics.service.ReviewCommands;
import com.lifeos.analytics.service.ReviewQueryService;
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

/** Sync cho entityType "review" (D202): CREATE trùng kỳ → CONFLICT ALREADY_EXISTS; periodType/periodKey bất biến. */
@Component
public class ReviewSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "review";

    private final ReviewCommandService commands;
    private final ReviewQueryService queries;
    private final JsonMapper json;

    public ReviewSyncHandler(ReviewCommandService commands, ReviewQueryService queries, JsonMapper json) {
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
        return "reviews";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, new ReviewCommands.Create(m.entityId(),
                    PayloadFields.string(p, "periodType"), PayloadFields.string(p, "periodKey"),
                    PayloadFields.string(p, "reflection"), PayloadFields.integer(p, "mood")), m.mutationId())
                    .version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(),
                    new ReviewCommands.Update(m.baseVersion(), PayloadFields.string(p, "periodType"),
                            PayloadFields.string(p, "periodKey"), PayloadFields.string(p, "reflection"),
                            PayloadFields.integer(p, "mood")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(commands.delete(userId, m.entityId(), m.baseVersion(),
                    m.mutationId()));
        };
    }

    @Override
    public List<EntitySnapshot> loadSnapshots(UUID userId, Collection<UUID> entityIds) {
        return queries.syncStates(userId, entityIds).stream()
                .map(s -> new EntitySnapshot(ENTITY_TYPE, s.id(), s.version(), s.deleted(),
                        s.deleted() ? null : SnapshotData.of(json, s.data())))
                .toList();
    }
}
