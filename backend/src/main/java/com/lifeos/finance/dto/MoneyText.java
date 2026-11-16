package com.lifeos.finance.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Trường tiền trong request REST chỉ nhận CHUỖI JSON (D100, FIN-01): số JSON ({@code 125000}, {@code 0.1}) bị từ chối
 * với 400 MALFORMED_REQUEST thay vì bị Jackson ép thành chuỗi (số JSON có thể đã mất chính xác ở client). Kiểm giá trị
 * (định dạng, SCALE, giới hạn) ở service.
 */
public class MoneyText extends ValueDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.getString();
        }
        return ctxt.reportInputMismatch(this, "Số tiền phải là chuỗi thập phân");
    }
}
