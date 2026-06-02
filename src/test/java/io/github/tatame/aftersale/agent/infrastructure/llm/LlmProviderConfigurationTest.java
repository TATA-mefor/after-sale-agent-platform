package io.github.tatame.aftersale.agent.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;
import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiChatGateway;
import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiLlmClient;
import io.github.tatame.aftersale.common.ai.SpringAiProviderProperties;
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
    void springAiChatProviderUsesSpringAiLlmClient() {
        AgentPlannerProperties.Llm properties = baseProperties("spring-ai-chat");
        SpringAiLlmClient springAiClient = new SpringAiLlmClient(enabledSpringAiProperties(), stubGateway());
        LlmClientFactory springAiFactory = new LlmClientFactory(objectMapper, springAiClient);

        LlmClient client = springAiFactory.create(properties);

        assertThat(client).isSameAs(springAiClient);
    }

    @Test
    void springAiChatProviderWithoutClientFailsClearly() {
        AgentPlannerProperties.Llm properties = baseProperties("spring-ai-chat");

        assertThatThrownBy(() -> factory.create(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("provider=spring-ai-chat")
                .hasMessageContaining("SpringAiLlmClient");
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

    private static SpringAiProviderProperties enabledSpringAiProperties() {
        SpringAiProviderProperties properties = new SpringAiProviderProperties();
        properties.setEnabled(true);
        properties.setChatEnabled(true);
        return properties;
    }

    private static SpringAiChatGateway stubGateway() {
        return (model, systemPrompt, userPrompt) -> """
                {
                  "intent": "RETURN_AND_REFUND",
                  "riskLevel": "MEDIUM",
                  "policyQuery": "质量问题 退货 退款",
                  "noteToAdd": "用户反馈质量问题，建议进入人工审核。",
                  "finalSuggestion": "建议根据质量问题规则处理。",
                  "evidenceHints": ["质量问题"],
                  "plannedTools": [
                    {
                      "toolName": "search_aftersale_policy",
                      "reason": "检索政策"
                    },
                    {
                      "toolName": "add_ticket_note",
                      "reason": "记录建议"
                    }
                  ]
                }
                """;
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
