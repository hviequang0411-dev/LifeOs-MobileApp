package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.identity.service.UserProvisioningService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** Tài khoản, số dư, đối soát, danh mục mặc định (PHASE_3 FIN-01..08, FIN-15, FIN-22). */
class FinanceAccountIT extends FinanceTestBase {

    @Autowired
    UserProvisioningService provisioning;

    @Test
    void Fin_01_moneyTravelsAsStringsAndJsonNumbersAreRejected() throws Exception {
        TestUser user = newUser();
        UUID usd = account(user, "USD");
        for (int i = 0; i < 10; i++) {
            tx(user, usd, "INCOME", "0.1", today(user));
            tx(user, usd, "INCOME", "0.2", today(user));
        }
        assertThat(balance(user, usd)).isEqualTo("3");
        UUID big = account(user, "VND", "openingBalance", "9999999999999");
        assertThat(balance(user, big)).isEqualTo("9999999999999");

        String numeric = "{\"id\":\"" + UUID.randomUUID() + "\",\"accountId\":\"" + usd
                + "\",\"kind\":\"EXPENSE\",\"amount\":0.3,\"occurredOn\":\"" + today(user) + "\"}";
        MvcResult rejected = post(user, F + "/transactions", numeric);
        expect(rejected, 400);
        assertThat(errorCode(rejected)).isEqualTo("MALFORMED_REQUEST");
        Map<String, Object> created = readJson(expect(createTx(user, UUID.randomUUID(), usd, "EXPENSE", "19.99",
                today(user)), 201));
        assertThat(created.get("amount")).isEqualTo("19.99");
        assertThat(created.get("currency")).isEqualTo("USD");
    }

    @Test
    void Fin_02_amountScaleIsCheckedAgainstTheAccountCurrency() throws Exception {
        TestUser user = newUser();
        UUID vnd = account(user, "VND");
        UUID usd = account(user, "USD");
        MvcResult vndScale = createTx(user, UUID.randomUUID(), vnd, "EXPENSE", "10.5", today(user));
        expect(vndScale, 400);
        assertThat(errorCode(vndScale)).isEqualTo("VALIDATION_FAILED");
        assertThat(fieldErrorCode(vndScale, "amount")).isEqualTo("SCALE");
        MvcResult usdScale = createTx(user, UUID.randomUUID(), usd, "EXPENSE", "10.555", today(user));
        assertThat(fieldErrorCode(usdScale, "amount")).isEqualTo("SCALE");
        MvcResult tooBig = createTx(user, UUID.randomUUID(), vnd, "EXPENSE", "10000000000000", today(user));
        assertThat(fieldErrorCode(tooBig, "amount")).isEqualTo("OUT_OF_RANGE");
        MvcResult zero = createTx(user, UUID.randomUUID(), vnd, "EXPENSE", "0", today(user));
        assertThat(fieldErrorCode(zero, "amount")).isEqualTo("OUT_OF_RANGE");
        MvcResult oldDate = createTx(user, UUID.randomUUID(), vnd, "EXPENSE", "1", LocalDate.of(1999, 12, 31));
        assertThat(fieldErrorCode(oldDate, "occurredOn")).isEqualTo("OUT_OF_RANGE");
        MvcResult farFuture = createTx(user, UUID.randomUUID(), vnd, "EXPENSE", "1", today(user).plusYears(1)
                .plusDays(1));
        assertThat(fieldErrorCode(farFuture, "occurredOn")).isEqualTo("OUT_OF_RANGE");
        expect(createTx(user, UUID.randomUUID(), vnd, "EXPENSE", "1", today(user).plusYears(1)), 201);
    }

    /**
     * Số dư = opening + thu − chi + chuyển đến − chuyển đi; bản đã xóa không tính; giao dịch tương lai không vào số dư
     * hôm nay. So với mô hình tham chiếu viết độc lập trên 500 thao tác ngẫu nhiên (seed cố định).
     */
    @Test
    void Fin_03_balanceMatchesAnIndependentModelOver500RandomOperations() throws Exception {
        TestUser user = newUser();
        LocalDate today = today(user);
        Random random = new Random(20261107);
        List<UUID> accounts = new ArrayList<>();
        Map<UUID, BigDecimal> opening = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            String open = (random.nextInt(2000000) - 500000) + "." + random.nextInt(10) + random.nextInt(10);
            UUID id = account(user, "USD", "openingBalance", open);
            accounts.add(id);
            opening.put(id, new BigDecimal(open));
        }
        record Op(UUID account, UUID other, BigDecimal signedForAccount, LocalDate day, boolean transfer) {
        }
        Map<UUID, Op> live = new HashMap<>();
        Map<UUID, Long> versions = new HashMap<>();
        List<UUID> liveIds = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            int action = random.nextInt(10);
            LocalDate day = today.plusDays(random.nextInt(40) - 30);
            BigDecimal amount = new BigDecimal(1 + random.nextInt(100000)).movePointLeft(2);
            if (action < 5 || liveIds.isEmpty()) {
                UUID acc = accounts.get(random.nextInt(3));
                boolean income = random.nextBoolean();
                UUID id = tx(user, acc, income ? "INCOME" : "EXPENSE", amount.toPlainString(), day);
                live.put(id, new Op(acc, null, income ? amount : amount.negate(), day, false));
                versions.put(id, 1L);
                liveIds.add(id);
            } else if (action < 7) {
                int a = random.nextInt(3);
                int b = (a + 1 + random.nextInt(2)) % 3;
                UUID id = transfer(user, accounts.get(a), accounts.get(b), amount.toPlainString(), day);
                live.put(id, new Op(accounts.get(a), accounts.get(b), amount.negate(), day, true));
                versions.put(id, 1L);
                liveIds.add(id);
            } else if (action < 9) {
                UUID id = liveIds.remove(random.nextInt(liveIds.size()));
                Op op = live.remove(id);
                String path = (op.transfer() ? "/transfers/" : "/transactions/") + id;
                expect(delete(user, F + path + "?version=" + versions.remove(id)), 204);
            } else {
                UUID id = liveIds.get(random.nextInt(liveIds.size()));
                Op op = live.get(id);
                String path = (op.transfer() ? "/transfers/" : "/transactions/") + id;
                Map<String, Object> updated = readJson(expect(patch(user, F + path, body("version", versions.get(id),
                        "amount", amount.toPlainString())), 200));
                versions.put(id, version(updated));
                BigDecimal signed = op.signedForAccount().signum() < 0 ? amount.negate() : amount;
                live.put(id, new Op(op.account(), op.other(), signed, op.day(), op.transfer()));
            }
        }
        for (LocalDate asOf : List.of(today, today.plusDays(15), today.minusDays(10))) {
            Map<UUID, BigDecimal> expected = new HashMap<>(opening);
            for (Op op : live.values()) {
                if (op.day().isAfter(asOf)) {
                    continue;
                }
                expected.merge(op.account(), op.signedForAccount(), BigDecimal::add);
                if (op.transfer()) {
                    expected.merge(op.other(), op.signedForAccount().negate(), BigDecimal::add);
                }
            }
            for (UUID acc : accounts) {
                Map<String, Object> got = readJson(expect(get(user, F + "/accounts/" + acc + "/balance?asOf=" + asOf),
                        200));
                assertThat(new BigDecimal((String) got.get("balance"))).as("số dư " + acc + " tại " + asOf)
                        .isEqualByComparingTo(expected.get(acc));
            }
        }
        Map<String, Object> view = readJson(get(user, F + "/accounts/" + accounts.get(0)));
        assertThat(view.get("balance")).isEqualTo(balance(user, accounts.get(0)));
    }

    @Test
    void Fin_05_transfersNeedTwoDistinctAccountsInTheSameCurrency() throws Exception {
        TestUser user = newUser();
        UUID vnd = account(user, "VND");
        UUID vnd2 = account(user, "VND");
        UUID usd = account(user, "USD");
        MvcResult mismatch = post(user, F + "/transfers", body("id", UUID.randomUUID().toString(),
                "fromAccountId", vnd.toString(), "toAccountId", usd.toString(), "amount", "1000",
                "occurredOn", today(user).toString()));
        expect(mismatch, 422);
        assertThat(errorCode(mismatch)).isEqualTo("FINANCE_CURRENCY_MISMATCH");
        MvcResult same = post(user, F + "/transfers", body("id", UUID.randomUUID().toString(),
                "fromAccountId", vnd.toString(), "toAccountId", vnd.toString(), "amount", "1000",
                "occurredOn", today(user).toString()));
        expect(same, 400);
        assertThat(errorCode(same)).isEqualTo("VALIDATION_FAILED");
        UUID ok = transfer(user, vnd, vnd2, "250000", today(user));
        assertThat(balance(user, vnd)).isEqualTo("-250000");
        assertThat(balance(user, vnd2)).isEqualTo("250000");
        long version = version(readJson(get(user, F + "/transfers/" + ok)));
        MvcResult moved = patch(user, F + "/transfers/" + ok, body("version", version,
                "toAccountId", account(user, "VND").toString()));
        expect(moved, 400);
        assertThat(fieldErrorCode(moved, "toAccountId")).isEqualTo("IMMUTABLE");
    }

    @Test
    void Fin_06_archivedAccountRejectsNewTransactionsButKeepsItsBalance() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND", "openingBalance", "1000");
        UUID other = account(user, "VND");
        UUID existing = tx(user, acc, "INCOME", "500", today(user));
        expect(post(user, F + "/accounts/" + acc + "/archive", body("version", 1)), 200);
        MvcResult blocked = createTx(user, UUID.randomUUID(), acc, "EXPENSE", "1", today(user));
        expect(blocked, 422);
        assertThat(errorCode(blocked)).isEqualTo("FINANCE_ACCOUNT_ARCHIVED");
        MvcResult blockedTransfer = post(user, F + "/transfers", body("id", UUID.randomUUID().toString(),
                "fromAccountId", other.toString(), "toAccountId", acc.toString(), "amount", "1",
                "occurredOn", today(user).toString()));
        assertThat(errorCode(blockedTransfer)).isEqualTo("FINANCE_ACCOUNT_ARCHIVED");
        assertThat(balance(user, acc)).isEqualTo("1500");
        long version = version(readJson(get(user, F + "/transactions/" + existing)));
        expect(patch(user, F + "/transactions/" + existing, body("version", version, "note", "Sửa ghi chú")), 200);
        assertThat(items(get(user, F + "/accounts?archived=true"))).extracting(m -> m.get("id"))
                .contains(acc.toString());
    }

    @Test
    void Fin_07_accountWithTransactionsAndCategoryInUseCannotBeDeleted() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        UUID food = category(user, "EXPENSE");
        UUID t = tx(user, acc, "EXPENSE", "50000", today(user), "categoryId", food.toString());
        MvcResult accDelete = delete(user, F + "/accounts/" + acc + "?version=1");
        expect(accDelete, 409);
        assertThat(errorCode(accDelete)).isEqualTo("FINANCE_ACCOUNT_HAS_TRANSACTIONS");
        MvcResult catDelete = delete(user, F + "/categories/" + food + "?version=1");
        expect(catDelete, 409);
        assertThat(errorCode(catDelete)).isEqualTo("FINANCE_CATEGORY_IN_USE");
        expect(post(user, F + "/categories/" + food + "/archive", body("version", 1)), 200);
        expect(delete(user, F + "/transactions/" + t + "?version=1"), 204);
        expect(delete(user, F + "/accounts/" + acc + "?version=1"), 204);
        expect(delete(user, F + "/categories/" + food + "?version=2"), 204);
        UUID budgeted = category(user, "EXPENSE");
        budget(user, budgeted, "VND", "1000000");
        assertThat(errorCode(delete(user, F + "/categories/" + budgeted + "?version=1")))
                .isEqualTo("FINANCE_CATEGORY_IN_USE");
        UUID income = category(user, "INCOME");
        MvcResult mismatch = createTx(user, UUID.randomUUID(), account(user, "VND"), "EXPENSE", "1", today(user),
                "categoryId", income.toString());
        expect(mismatch, 422);
        assertThat(errorCode(mismatch)).isEqualTo("FINANCE_CATEGORY_KIND_MISMATCH");
    }

    @Test
    void Fin_08_currencyIsLockedOnceTheAccountHasTransactions() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND", "openingBalance", "100");
        Map<String, Object> changed = readJson(expect(patch(user, F + "/accounts/" + acc,
                body("version", 1, "currency", "USD")), 200));
        assertThat(changed.get("currency")).isEqualTo("USD");
        UUID t = tx(user, acc, "INCOME", "1.5", today(user));
        MvcResult locked = patch(user, F + "/accounts/" + acc, body("version", 2, "currency", "VND"));
        expect(locked, 409);
        assertThat(errorCode(locked)).isEqualTo("FINANCE_CURRENCY_LOCKED");
        expect(delete(user, F + "/transactions/" + t + "?version=1"), 204);
        expect(patch(user, F + "/accounts/" + acc, body("version", 2, "currency", "EUR")), 200);
        UUID cent = account(user, "USD", "openingBalance", "10.25");
        MvcResult scale = patch(user, F + "/accounts/" + cent, body("version", 1, "currency", "VND"));
        assertThat(fieldErrorCode(scale, "openingBalance")).isEqualTo("SCALE");
        MvcResult badCurrency = patch(user, F + "/accounts/" + cent, body("version", 1, "currency", "XAU"));
        assertThat(fieldErrorCode(badCurrency, "currency")).isEqualTo("INVALID_VALUE");
        MvcResult nameTaken = post(user, F + "/accounts", body("id", UUID.randomUUID().toString(),
                "name", readJson(get(user, F + "/accounts/" + cent)).get("name").toString().toUpperCase(),
                "accountType", "CASH", "currency", "VND"));
        expect(nameTaken, 409);
        assertThat(errorCode(nameTaken)).isEqualTo("FINANCE_NAME_TAKEN");
    }

    @Test
    void Fin_15_reconcileReturnsTheDifferenceAndWritesNothing() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND", "openingBalance", "1000000");
        tx(user, acc, "EXPENSE", "250000", today(user).minusDays(2));
        tx(user, acc, "EXPENSE", "100000", today(user).plusDays(5));
        int logs = changeLogsOfUser(user.id());
        Map<String, Object> result = readJson(expect(post(user, F + "/accounts/" + acc + "/reconcile",
                body("statementBalance", "700000", "asOfDate", today(user).toString())), 200));
        assertThat(result).containsEntry("computedBalance", "750000").containsEntry("difference", "-50000")
                .containsEntry("statementBalance", "700000").containsEntry("currency", "VND");
        assertThat(changeLogsOfUser(user.id())).isEqualTo(logs);
        assertThat(version(readJson(get(user, F + "/accounts/" + acc)))).isEqualTo(1);
        MvcResult numeric = post(user, F + "/accounts/" + acc + "/reconcile",
                "{\"statementBalance\":700000,\"asOfDate\":\"" + today(user) + "\"}");
        expect(numeric, 400);
    }

    @Test
    void Fin_22_registrationCreatesDefaultCategoriesAndTheBackfillNeverDuplicates() throws Exception {
        String email = "fin22-" + UUID.randomUUID() + "@lifeos.test";
        expect(post(null, "/api/v1/auth/register", body("email", email, "password", "Mat-khau-du-dai-1",
                "displayName", "Fin")), 202);
        UUID userId = jdbc.sql("SELECT id FROM users WHERE lower(email) = :e").param("e", email)
                .query(UUID.class).single();
        assertThat(systemCategories(userId)).isEqualTo(15);
        assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type ="
                + " 'financial_category' AND operation = 'CREATE' AND mutation_id IS NULL").param("u", userId)
                .query(Integer.class).single()).isEqualTo(15);
        assertThat(provisioning.provisionMissing(userId)).isZero();
        assertThat(systemCategories(userId)).isEqualTo(15);

        TestUser old = newUser();
        UUID renamed = category(old, "EXPENSE");
        jdbc.sql("UPDATE financial_categories SET name = 'Ăn uống' WHERE id = :id").param("id", renamed).update();
        assertThat(provisioning.provisionMissing(old.id())).as("bỏ qua mục trùng tên đã có").isEqualTo(14);
        assertThat(provisioning.provisionMissing(old.id())).isZero();
        assertThat(systemCategories(old.id())).isEqualTo(14);
        List<Map<String, Object>> adjustments = jdbc.sql("SELECT kind, name FROM financial_categories"
                + " WHERE user_id = :u AND system_key = 'ADJUSTMENT' ORDER BY kind").param("u", old.id()).query()
                .listOfRows();
        assertThat(adjustments).extracting(r -> r.get("kind")).containsExactly("EXPENSE", "INCOME");
    }

    private int systemCategories(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM financial_categories WHERE user_id = :u AND system_key IS NOT NULL"
                + " AND deleted_at IS NULL").param("u", userId).query(Integer.class).single();
    }

    private int changeLogsOfUser(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single();
    }
}
