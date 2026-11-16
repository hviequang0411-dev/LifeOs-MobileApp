package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.ai.provider.FakeLLMProvider;
import com.lifeos.ai.provider.LLMProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FakeLLMProviderTest {
    @Test
    void fakeIsDeterministicCountsCallsAndNeedsNoNetworkOrSecret() {
        FakeLLMProvider fake = new FakeLLMProvider();
        LLMProvider.Request request = new LLMProvider.Request("CHAT", "test", Map.of(), Map.of());
        assertThat(fake.generate(request).content()).isEqualTo("fake-response");
        assertThat(fake.generate(request).latencyMs()).isEqualTo(3);
        assertThat(fake.calls()).isEqualTo(2);
    }
}
