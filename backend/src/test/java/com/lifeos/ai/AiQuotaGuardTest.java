package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.repository.AiStore;
import com.lifeos.ai.service.AiCostCalculator;
import com.lifeos.ai.service.AiQuotaGuard;
import com.lifeos.common.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiQuotaGuardTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void AI_16_blocksMinuteAndDailyQuotaBeforeProvider() {
        AiProperties properties = enabled();
        AiStore store = new FixedTotalsStore(new AiStore.Totals(5, 0, 0, 0));
        assertThatThrownBy(() -> new AiQuotaGuard(store, properties, CLOCK)
                .check(UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getProperties())
                        .containsEntry("errorCode", "AI_RATE_LIMITED"));
    }

    @Test
    void AI_18_killSwitchAndBudgetStopAreStable() {
        AiStore store = new FixedTotalsStore(new AiStore.Totals(0, 0, 0, 0));
        AiProperties disabled = new AiProperties();
        assertThatThrownBy(() -> new AiQuotaGuard(store, disabled, CLOCK).check(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getProperties())
                        .containsEntry("errorCode", "AI_DISABLED"));
        AiProperties budget = enabled();
        budget.setBudgetAvailable(false);
        assertThatThrownBy(() -> new AiQuotaGuard(store, budget, CLOCK).check(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getProperties())
                        .containsEntry("errorCode", "AI_BUDGET_EXCEEDED"));
    }

    @Test
    void AI_17_costIsExactIntegerMicrousd() {
        AiCostCalculator calculator = new AiCostCalculator();
        assertThat(calculator.estimate(1_000_000, 1_000_000)).isEqualTo(12_000_000L);
        assertThat(calculator.estimate(31, 7)).isEqualTo(132L);
        assertThatCode(() -> calculator.estimate(0, 0)).doesNotThrowAnyException();
    }

    private static AiProperties enabled() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        return properties;
    }

    private static final class FixedTotalsStore extends AiStore {
        private final Totals totals;

        private FixedTotalsStore(Totals totals) {
            super(null, null);
            this.totals = totals;
        }

        @Override
        public Totals totals(UUID userId, Instant start, Instant end) {
            return totals;
        }
    }
}
