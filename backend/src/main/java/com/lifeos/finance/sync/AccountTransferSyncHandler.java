package com.lifeos.finance.sync;

import com.lifeos.common.sync.EntitySnapshot;
import com.lifeos.common.sync.Mutation;
import com.lifeos.common.sync.MutationOutcome;
import com.lifeos.common.sync.SyncEntityHandler;
import com.lifeos.common.util.PayloadFields;
import com.lifeos.common.util.SnapshotData;
import com.lifeos.finance.service.TransferCommandService;
import com.lifeos.finance.service.FinanceCommands;
import com.lifeos.finance.service.FinanceQueryService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Sync cho entityType "account_transfer" (PHASE_3 §8, D115). Chỉ dịch payload → FinanceCommands; quy tắc ở service. Khóa do
 * server quản (currency, version…) không được đọc (SYN-18). Mọi lệch baseVersion → CONFLICT, không hợp nhất (D105).
 */
@Component
public class AccountTransferSyncHandler implements SyncEntityHandler {

    public static final String ENTITY_TYPE = "account_transfer";

    private final TransferCommandService commands;
    private final FinanceQueryService queries;
    private final JsonMapper json;

    public AccountTransferSyncHandler(TransferCommandService commands, FinanceQueryService queries, JsonMapper json) {
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
        return "account_transfers";
    }

    @Override
    public MutationOutcome apply(UUID userId, Mutation m) {
        Map<String, Object> p = m.payload();
        return switch (m.operation()) {
            case CREATE -> new MutationOutcome.Applied(commands.create(userId, new FinanceCommands.CreateTransfer(m.entityId(), PayloadFields.uuid(p, "fromAccountId"), PayloadFields.uuid(p, "toAccountId"),
                            PayloadFields.string(p, "amount"), PayloadFields.string(p, "occurredOn"),
                            PayloadFields.string(p, "note")),
                    m.mutationId()).version());
            case UPDATE -> new MutationOutcome.Applied(commands.update(userId, m.entityId(),
                    new FinanceCommands.UpdateTransfer(m.baseVersion(), PayloadFields.uuid(p, "fromAccountId"), PayloadFields.uuid(p, "toAccountId"),
                            PayloadFields.string(p, "amount"), PayloadFields.string(p, "occurredOn"),
                            PayloadFields.string(p, "note")), m.mutationId()).version());
            case DELETE -> new MutationOutcome.Applied(
                    commands.delete(userId, m.entityId(), m.baseVersion(), m.mutationId()));
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
