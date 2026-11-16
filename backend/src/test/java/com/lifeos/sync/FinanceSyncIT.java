package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Sync cho 8 entity Finance (PHASE_3 §8, D105, D115): FIN-18 (CONFLICT, không LWW), FIN-19 (tham chiếu, thứ tự phụ
 * thuộc), FIN-20 (mass assignment qua payload) và các kết quả tương đương SYN (DUPLICATE, tombstone, cascade).
 */
class FinanceSyncIT extends SyncTestBase {

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private Map<String, Object> accountCreate(UUID id, String currency) {
        return mutation("financial_account", id, "CREATE", 0, payload("name", "TK " + id.toString().substring(0, 8),
                "accountType", "CASH", "currency", currency));
    }

    private Map<String, Object> txCreate(UUID id, UUID accountId, String amount) {
        return mutation("financial_transaction", id, "CREATE", 0, payload("accountId", accountId.toString(),
                "kind", "EXPENSE", "amount", amount, "occurredOn", today().toString(), "note", "Ăn trưa"));
    }

    private UUID account(TestUser user, String currency) throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(pushOne(user, accountCreate(id, currency))).containsEntry("status", "APPLIED");
        return id;
    }

    @Test
    @SuppressWarnings("unchecked")
    void Fin_18_amountEditedOnTwoDevicesIsAConflictWithAStringSnapshotNeverLastWriteWins() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        UUID tx = UUID.randomUUID();
        pushOne(user, txCreate(tx, acc, "50000"));
        Map<String, Object> deviceA = mutation("financial_transaction", tx, "UPDATE", 1, payload("amount", "60000"));
        assertThat(pushOne(user, deviceA)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        Map<String, Object> deviceB = pushOne(user, mutation("financial_transaction", tx, "UPDATE", 1,
                payload("amount", "70000")));
        assertThat(deviceB).containsEntry("status", "CONFLICT").doesNotContainKey("conflictReason");
        Map<String, Object> snapshot = (Map<String, Object>) deviceB.get("serverSnapshot");
        Map<String, Object> data = (Map<String, Object>) snapshot.get("data");
        assertThat(data.get("amount")).isEqualTo("60000");
        assertThat(data.get("currency")).isEqualTo("VND");
        assertThat(row("financial_transactions", tx).get("amount").toString()).startsWith("60000");
        assertThat(pushOne(user, deviceA)).as("gửi lại cùng mutationId").containsEntry("status", "DUPLICATE");

        // Mọi entity Finance đều CONFLICT khi lệch version (D105).
        UUID cat = UUID.randomUUID();
        pushOne(user, mutation("financial_category", cat, "CREATE", 0, payload("name", "Cà phê", "kind", "EXPENSE")));
        pushOne(user, mutation("financial_category", cat, "UPDATE", 1, payload("name", "Cafe")));
        assertThat(pushOne(user, mutation("financial_category", cat, "UPDATE", 1, payload("archived", true))))
                .containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("financial_account", acc, "UPDATE", 0, payload("name", "X"))))
                .containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("financial_transaction", tx, "UPDATE", 2, payload("amount", 70000))))
                .as("số JSON bị từ chối").containsEntry("status", "REJECTED")
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("financial_transaction", tx, "UPDATE", 2, payload("amount", "70000.5"))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
    }

    @Test
    void Fin_19_badReferencesAndDependentMutationsAfterARejectedCreate() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID foreignAcc = account(other, "VND");
        UUID badAcc = UUID.randomUUID();
        Map<String, Object> badAccount = mutation("financial_account", badAcc, "CREATE", 0,
                payload("name", "TK", "accountType", "NOPE", "currency", "VND"));
        UUID dependent = UUID.randomUUID();
        List<Map<String, Object>> results = push(user, badAccount, txCreate(dependent, badAcc, "1000"),
                txCreate(UUID.randomUUID(), foreignAcc, "1000"));
        assertThat(results.get(0)).containsEntry("status", "REJECTED").containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(results.get(1)).containsEntry("status", "REJECTED")
                .containsEntry("errorCode", "FINANCE_ACCOUNT_NOT_FOUND");
        assertThat(results.get(2)).as("tài khoản của người khác giống hệt không tồn tại")
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "FINANCE_ACCOUNT_NOT_FOUND");

        UUID acc = account(user, "VND");
        UUID income = UUID.randomUUID();
        pushOne(user, mutation("financial_category", income, "CREATE", 0, payload("name", "Lương X", "kind",
                "INCOME")));
        assertThat(pushOne(user, mutation("financial_transaction", UUID.randomUUID(), "CREATE", 0,
                payload("accountId", acc.toString(), "categoryId", income.toString(), "kind", "EXPENSE",
                        "amount", "1", "occurredOn", today().toString()))))
                .containsEntry("errorCode", "FINANCE_CATEGORY_KIND_MISMATCH");
        assertThat(pushOne(user, mutation("financial_transaction", UUID.randomUUID(), "CREATE", 0,
                payload("accountId", acc.toString(), "categoryId", UUID.randomUUID().toString(), "kind", "EXPENSE",
                        "amount", "1", "occurredOn", today().toString()))))
                .containsEntry("errorCode", "FINANCE_CATEGORY_NOT_FOUND");
        assertThat(pushOne(user, mutation("saving_contribution", UUID.randomUUID(), "CREATE", 0,
                payload("goalId", UUID.randomUUID().toString(), "amount", "1", "occurredOn", today().toString()))))
                .containsEntry("errorCode", "FINANCE_GOAL_NOT_FOUND");
        assertThat(pushOne(user, mutation("financial_account", acc, "UPDATE", 1, payload("archived", true))))
                .containsEntry("status", "APPLIED");
        assertThat(pushOne(user, txCreate(UUID.randomUUID(), acc, "1"))).containsEntry("status", "REJECTED")
                .containsEntry("errorCode", "FINANCE_ACCOUNT_ARCHIVED");
        UUID vnd2 = account(user, "VND");
        UUID usd = account(user, "USD");
        assertThat(pushOne(user, mutation("account_transfer", UUID.randomUUID(), "CREATE", 0,
                payload("fromAccountId", vnd2.toString(), "toAccountId", usd.toString(), "amount", "1",
                        "occurredOn", today().toString())))).containsEntry("errorCode", "FINANCE_CURRENCY_MISMATCH");
        assertThat(pushOne(user, mutation("financial_transaction", UUID.randomUUID(), "UPDATE", 1,
                payload("amount", "1")))).containsEntry("errorCode", "FINANCE_TRANSACTION_NOT_FOUND");
    }

    @Test
    void Fin_20_serverManagedFieldsInPayloadAreIgnored() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        UUID tx = UUID.randomUUID();
        Map<String, Object> created = pushOne(user, mutation("financial_transaction", tx, "CREATE", 0,
                payload("accountId", acc.toString(), "kind", "INCOME", "amount", "5000", "occurredOn",
                        today().toString(), "currency", "USD", "recurringTransactionId", UUID.randomUUID().toString(),
                        "recurrenceDate", today().toString(), "version", 99, "userId", UUID.randomUUID().toString())));
        assertThat(created).containsEntry("status", "APPLIED").containsEntry("entityVersion", 1);
        Map<String, Object> row = row("financial_transactions", tx);
        assertThat(row.get("currency")).isEqualTo("VND");
        assertThat(row.get("recurring_transaction_id")).isNull();
        assertThat(row.get("recurrence_date")).isNull();
        assertThat(row.get("user_id")).isEqualTo(user.id());

        UUID recurring = UUID.randomUUID();
        pushOne(user, mutation("recurring_transaction", recurring, "CREATE", 0, payload("accountId", acc.toString(),
                "kind", "EXPENSE", "amount", "1000", "frequency", "MONTHLY", "startOn", today().plusDays(3).toString(),
                "nextRunOn", "2000-01-01", "currency", "USD")));
        assertThat(row("recurring_transactions", recurring).get("next_run_on").toString())
                .isEqualTo(today().plusDays(3).toString());
        assertThat(row("recurring_transactions", recurring).get("currency")).isEqualTo("VND");

        UUID goal = UUID.randomUUID();
        pushOne(user, mutation("saving_goal", goal, "CREATE", 0, payload("name", "Du lịch", "currency", "VND",
                "targetAmount", "1000", "status", "ACHIEVED")));
        assertThat(row("saving_goals", goal).get("status")).isEqualTo("ACTIVE");
        UUID cat = UUID.randomUUID();
        pushOne(user, mutation("financial_category", cat, "CREATE", 0, payload("name", "Riêng", "kind", "EXPENSE",
                "systemKey", "ADJUSTMENT")));
        assertThat(row("financial_categories", cat).get("system_key")).isNull();
        assertThat(pushOne(user, mutation("financial_category", cat, "UPDATE", 1, payload("kind", "INCOME"))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
    }

    @Test
    void goalCascadeDeleteAndContributionMatrix() throws Exception {
        TestUser user = newUser();
        UUID goal = UUID.randomUUID();
        pushOne(user, mutation("saving_goal", goal, "CREATE", 0, payload("name", "Xe", "currency", "VND",
                "targetAmount", "1000")));
        List<UUID> contributions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            UUID c = UUID.randomUUID();
            contributions.add(c);
            assertThat(pushOne(user, mutation("saving_contribution", c, "CREATE", 0, payload("goalId", goal.toString(),
                    "amount", "400", "occurredOn", today().toString())))).containsEntry("status", "APPLIED");
        }
        assertThat(row("saving_goals", goal).get("status")).isEqualTo("ACHIEVED");
        assertThat(changeLogRows(user.id(), goal)).last().satisfies(l -> assertThat(l)
                .containsEntry("operation", "UPDATE").containsEntry("mutation_id", null));
        UUID first = contributions.get(0);
        assertThat(pushOne(user, mutation("saving_contribution", first, "UPDATE", 1,
                payload("goalId", UUID.randomUUID().toString())))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("saving_contribution", first, "UPDATE", 1, payload("amount", "100"))))
                .containsEntry("status", "APPLIED");
        assertThat(row("saving_goals", goal).get("status")).isEqualTo("ACTIVE");
        long goalVersion = (Long) row("saving_goals", goal).get("version");
        Map<String, Object> delete = mutation("saving_goal", goal, "DELETE", goalVersion, null);
        assertThat(pushOne(user, delete)).containsEntry("status", "APPLIED");
        for (UUID c : contributions) {
            assertThat(row("saving_contributions", c).get("deleted_at")).isNotNull();
            assertThat(changeLogRows(user.id(), c)).last().satisfies(l -> assertThat(l)
                    .containsEntry("operation", "DELETE").containsEntry("mutation_id", null));
        }
        assertThat(changeLogRows(user.id(), goal)).last().satisfies(l -> assertThat(l)
                .containsEntry("mutation_id", mutationIdOf(delete)));
        assertThat(pushOne(user, mutation("saving_goal", goal, "DELETE", goalVersion, null)))
                .as("tombstone DELETE").containsEntry("status", "APPLIED");
        assertThat(pushOne(user, mutation("saving_goal", goal, "UPDATE", goalVersion + 1, payload("name", "Y"))))
                .containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("saving_contribution", UUID.randomUUID(), "UPDATE", 1,
                payload("amount", "1")))).containsEntry("errorCode", "NOT_FOUND");
    }

    @Test
    @SuppressWarnings("unchecked")
    void everyFinanceEntityTypeRoundTripsCreateUpdateDelete() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        UUID acc2 = account(user, "VND");
        UUID cat = UUID.randomUUID();
        String day = today().toString();
        List<Object[]> cases = List.of(
                new Object[] {"financial_category", cat, payload("name", "Đi lại", "kind", "EXPENSE"),
                        payload("name", "Đi lại 2")},
                new Object[] {"financial_transaction", UUID.randomUUID(), payload("accountId", acc.toString(),
                        "categoryId", cat.toString(), "kind", "EXPENSE", "amount", "1000", "occurredOn", day),
                        payload("note", "x")},
                new Object[] {"account_transfer", UUID.randomUUID(), payload("fromAccountId", acc.toString(),
                        "toAccountId", acc2.toString(), "amount", "1000", "occurredOn", day), payload("amount", "2")},
                new Object[] {"budget", UUID.randomUUID(), payload("currency", "VND", "amount", "1000000"),
                        payload("archived", true)},
                new Object[] {"recurring_transaction", UUID.randomUUID(), payload("accountId", acc.toString(),
                        "kind", "INCOME", "amount", "1000", "frequency", "YEARLY", "startOn", day),
                        payload("status", "PAUSED")},
                new Object[] {"saving_goal", UUID.randomUUID(), payload("name", "G", "currency", "VND",
                        "targetAmount", "1000"), payload("status", "ARCHIVED")});
        for (Object[] c : cases) {
            String type = (String) c[0];
            UUID id = (UUID) c[1];
            assertThat(pushOne(user, mutation(type, id, "CREATE", 0, (Map<String, Object>) c[2]))).as(type)
                    .containsEntry("status", "APPLIED");
            assertThat(pushOne(user, mutation(type, id, "UPDATE", 1, (Map<String, Object>) c[3]))).as(type)
                    .containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        }
        for (int i = cases.size() - 1; i >= 0; i--) {
            String type = (String) cases.get(i)[0];
            UUID id = (UUID) cases.get(i)[1];
            assertThat(pushOne(user, mutation(type, id, "DELETE", 2, null))).as(type)
                    .containsEntry("status", "APPLIED").containsEntry("entityVersion", 3);
        }
        assertThat(pushOne(user, mutation("financial_account", acc2, "DELETE", 1, null))).as("acc2 hết chuyển khoản")
                .containsEntry("status", "APPLIED");
    }
}
