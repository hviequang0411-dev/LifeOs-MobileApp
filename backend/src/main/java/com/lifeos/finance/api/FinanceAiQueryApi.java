package com.lifeos.finance.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Minimal Finance aggregate input. No account number or transaction note is exposed. */
public interface FinanceAiQueryApi {
    record Snapshot(UUID id, String kind, String amount, String currency, LocalDate occurredOn, long version) { }

    List<Snapshot> aiTransactions(UUID userId, LocalDate from, LocalDate to, String currency, int limit);
}
