package io.github.tatame.aftersale.agent.infrastructure.llm;

import io.github.tatame.aftersale.agent.infrastructure.springai.SpringAiLlmClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;

/**
 * 根据配置创建具体 LLM Provider Client。
 *
 * <p>边界：本工厂只做 Provider 适配和端点选择，不校验 AgentPlan、不记录 prompt，也不决定工具执行策略。
 */
public class LlmClientFactory {

    private static final String RESPONSES_PATH = "/responses";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final ObjectMapper objectMapper;

    private final Optional<SpringAiLlmClient> springAiLlmClient;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed ObjectMapper is stored to construct provider clients consistently.")
    public LlmClientFactory(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring AI LLM client is an optional infrastructure adapter selected by provider config.")
    public LlmClientFactory(ObjectMapper objectMapper, SpringAiLlmClient springAiLlmClient) {
        this.objectMapper = objectMapper;
        this.springAiLlmClient = Optional.ofNullable(springAiLlmClient);
    }

    public LlmClient create(AgentPlannerProperties.Llm properties) {
        LlmProvider provider = LlmProvider.from(properties.getProvider());
        return switch (provider) {
            case OPENAI_RESPONSES, DASHSCOPE_RESPONSES -> new OpenAiLlmClient(
                    settings(provider, properties),
                    objectMapper);
            case DASHSCOPE_CHAT_COMPATIBLE -> new ChatCompletionsLlmClient(
                    settings(provider, properties),
                    objectMapper);
            case SPRING_AI_CHAT -> springAiLlmClient.orElseThrow(() -> new IllegalStateException(
                    "provider=spring-ai-chat requires SpringAiLlmClient configuration"));
        };
    }

    /**
     * 将 Spring 配置折叠成单个 Provider 调用配置。
     *
     * <p>DashScope 支持显式 endpoint 和 baseUrl 拼接两种配置方式，保留这层兼容可以避免调用侧理解
     * 不同 Provider 的 URL 细节。
     */
    public LlmProviderSettings settings(LlmProvider provider, AgentPlannerProperties.Llm properties) {
        return switch (provider) {
            case OPENAI_RESPONSES -> new LlmProviderSettings(
                    provider,
                    properties.getModel(),
                    properties.getApiKey(),
                    properties.getEndpoint(),
                    properties.getTimeoutSeconds());
            case DASHSCOPE_RESPONSES -> new LlmProviderSettings(
                    provider,
                    properties.getModel(),
                    properties.getDashscope().getApiKey(),
                    endpointOrDefault(
                            properties.getDashscope().getResponsesEndpoint(),
                            properties.getDashscope().getBaseUrl(),
                            RESPONSES_PATH),
                    properties.getTimeoutSeconds());
            case DASHSCOPE_CHAT_COMPATIBLE -> new LlmProviderSettings(
                    provider,
                    properties.getModel(),
                    properties.getDashscope().getApiKey(),
                    endpointOrDefault(
                            properties.getDashscope().getChatCompletionsEndpoint(),
                            properties.getDashscope().getBaseUrl(),
                            CHAT_COMPLETIONS_PATH),
                    properties.getTimeoutSeconds());
            case SPRING_AI_CHAT -> throw new IllegalArgumentException(
                    "spring-ai-chat is selected through SpringAiLlmClient and does not use HTTP provider settings");
        };
    }

    private static String endpointOrDefault(String explicitEndpoint, String baseUrl, String path) {
        if (explicitEndpoint != null && !explicitEndpoint.isBlank()) {
            return explicitEndpoint;
        }
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.strip();
        if (normalizedBaseUrl.endsWith("/")) {
            return normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1) + path;
        }
        return normalizedBaseUrl + path;
    }
}
