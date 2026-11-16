package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hợp đồng backend ↔ Flutter cho Finance bằng fixture JSON dùng chung (D68, TESTING_RULES §10; BACKEND_MASTER_PLAN §7):
 * tiền là CHUỖI (D100). Endpoint thật nhận request fixture (chỉ thay id/accountId/ngày) và trả phản hồi cùng hình dạng
 * (cùng tập trường, cùng kiểu JSON từng trường; null ở fixture khớp null) với response fixture.
 */
class FinanceFixtureContractIT extends FinanceTestBase {

    private static final Path FIXTURES = Path.of("..", "docs", "api", "fixtures");

    private JsonNode fixture(String name) throws Exception {
        return json.readTree(Files.readString(FIXTURES.resolve(name)));
    }

    private static Object shape(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> fields = new TreeMap<>();
            node.properties().forEach(e -> fields.put(e.getKey(), shape(e.getValue())));
            return fields;
        }
        if (node.isArray()) {
            List<Object> items = new ArrayList<>();
            node.forEach(item -> items.add(shape(item)));
            return items;
        }
        return node.getNodeType().name();
    }

    @Test
    void transactionAndAccountResponsesMatchTheSharedFixturesWithStringMoney() throws Exception {
        TestUser user = newUser();
        UUID account = account(user, "VND", "openingBalance", "500000");
        ObjectNode request = (ObjectNode) fixture("finance-transaction.request.json");
        request.put("id", UUID.randomUUID().toString());
        request.put("accountId", account.toString());
        request.put("occurredOn", today(user).toString());
        JsonNode response = json.readTree(expect(post(user, F + "/transactions", json.writeValueAsString(request)), 201)
                .getResponse().getContentAsString());
        JsonNode expected = fixture("finance-transaction.response.json");
        assertThat(shape(response)).isEqualTo(shape(expected));
        assertThat(response.get("amount").isString()).isTrue();
        assertThat(response.get("amount").asString()).isEqualTo(expected.get("amount").asString());

        JsonNode view = json.readTree(expect(get(user, F + "/accounts/" + account), 200).getResponse()
                .getContentAsString());
        JsonNode expectedAccount = fixture("finance-account.response.json");
        ObjectNode comparable = ((ObjectNode) view).deepCopy();
        comparable.put("name", "Tiền mặt");
        assertThat(shape(comparable)).isEqualTo(shape(expectedAccount));
        assertThat(view.get("balance").asString()).isEqualTo(expectedAccount.get("balance").asString());
    }
}
