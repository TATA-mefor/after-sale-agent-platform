package com.example.aftersale.agent.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.agent.application.planner.AgentPlanValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmProviderConfigurationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmClientFactory factory = new LlmClientFactory(objectMapper);

    @Test
    void openAiResponsesProviderUsesResponsesClient() {
        AgentPlannerProperties.Llm properties = baseProperties("openai-responses");
        properties.setApiKey("openai-test-key");
        properties.setEndpoint("https://api.openai.com/v1/responses");

        LlmClient client = factory.create(properties);

        assertThat(client).isInstanceOf(OpenAiLlmClient.class);
        assertThat(factory.settings(LlmProvider.OPENAI_RESPONSES, properties).endpoint())
                .isEqualTo("https://api.openai.com/v1/responses");
    }

    @Test
    void dashScopeResponsesProviderUsesResponsesEndpoint() {
        AgentPlannerProperties.Llm properties = baseProperties("dashscope-responses");
        properties.getDashscope().setApiKey("dashscope-test-key");

        LlmClient client = factory.create(properties);
        LlmProviderSettings settings = factory.settings(LlmProvider.DASHSCOPE_RESPONSES, properties);

        assertThat(client).isInstanceOf(OpenAiLlmClient.class);
        assertThat(settings.endpoint()).endsWith("/responses");
    }

    @Test
    void dashScopeChatCompatibleProviderUsesChatCompletionsClient() {
        AgentPlannerProperties.Llm properties = baseProperties("dashscope-chat-compatible");
        properties.getDashscope().setApiKey("dashscope-test-key");

        LlmClient client = factory.create(properties);
        LlmProviderSettings settings = factory.settings(LlmProvider.DASHSCOPE_CHAT_COMPATIBLE, properties);

        assertThat(client).isInstanceOf(ChatCompletionsLlmClient.class);
        assertThat(settings.endpoint()).endsWith("/chat/completions");
    }

    @Test
    void chatCompletionsRequestConvertsSystemAndUserPromptToMessages() {
        ChatCompletionsLlmClient client = new ChatCompletionsLlmClient(
                settings(),
                objectMapper);

        Map<String, Object> body = client.requestBody(request());

        assertThat(body).containsEntry("model", "test-model");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        assertThat(messages)
                .containsExactly(
                        Map.of("role", "system", "content", "system prompt"),
                        Map.of("role", "user", "content", "user prompt"));
        assertThat(body).containsKey("response_format");
    }

    @Test
    void chatCompletionsResponseParsesChoiceMessageContent() {
        ChatCompletionsLlmClient client = new ChatCompletionsLlmClient(
                settings(),
                objectMapper);

        String text = client.extractText("""
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"intent\\":\\"RETURN_AND_REFUND\\"}"
                      }
                    }
                  ]
                }
                """);

        assertThat(text).isEqualTo("{\"intent\":\"RETURN_AND_REFUND\"}");
    }

    @Test
    void providerErrorSummaryDoesNotContainApiKey() {
        LlmProviderSettings settings = new LlmProviderSettings(
                LlmProvider.DASHSCOPE_CHAT_COMPATIBLE,
                "qwen3.6-plus",
                "dashscope-secret-key",
                "https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1/chat/completions",
                30);

        String summary = LlmProviderErrorFormatter.format(settings, 401, "invalid key dashscope-secret-key");

        assertThat(summary).contains("provider=dashscope-chat-compatible");
        assertThat(summary).contains("endpointHost=dashscope.aliyuncs.com");
        assertThat(summary).contains("model=qwen3.6-plus");
        assertThat(summary).doesNotContain("dashscope-secret-key");
    }

    @Test
    void unsupportedProviderFailsClearly() {
        assertThatThrownBy(() -> LlmProvider.from("unknown-provider"))
                .isInstanceOf(AgentPlanValidationException.class)
                .hasMessageContaining("Unsupported LLM provider");
    }

    private static AgentPlannerProperties.Llm baseProperties(String provider) {
        AgentPlannerProperties.Llm properties = new AgentPlannerProperties.Llm();
        properties.setProvider(provider);
        properties.setModel("test-model");
        return properties;
    }

    private static LlmRequest request() {
        return new LlmRequest("test-model", "system prompt", "user prompt", 30);
    }

    private static LlmProviderSettings settings() {
        return new LlmProviderSettings(
                LlmProvider.DASHSCOPE_CHAT_COMPATIBLE,
                "test-model",
                "test-key",
                "https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1/chat/completions",
                30);
    }

}
