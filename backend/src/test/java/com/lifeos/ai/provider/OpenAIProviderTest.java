package com.lifeos.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.common.exception.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class OpenAIProviderTest {
    @Test
    void AI_09_responsesRequestNeverStoresOrEnablesTools() {
        AiProperties properties = new AiProperties();
        properties.setModel("test-model");
        OpenAIProvider provider = new OpenAIProvider(properties, JsonMapper.builder().build());

        var params = provider.params(new LLMProvider.Request("CHAT", "hello",
                Map.of("value", "untrusted"), Map.of()));

        assertThat(params.store()).contains(false);
        assertThat(params.tools()).isEmpty();
        assertThat(params.model()).isPresent();
        assertThat(params.instructions()).get().asString().contains("untrusted user data");
    }

    @Test
    void AI_09_missingSecretFailsClosedWithoutNetwork() {
        OpenAIProvider provider = new OpenAIProvider(new AiProperties(), JsonMapper.builder().build());
        assertThatThrownBy(() -> provider.generate(new LLMProvider.Request("CHAT", "hello", Map.of(), Map.of())))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getProperties())
                        .containsEntry("errorCode", "AI_PROVIDER_UNAVAILABLE"));
    }
}
