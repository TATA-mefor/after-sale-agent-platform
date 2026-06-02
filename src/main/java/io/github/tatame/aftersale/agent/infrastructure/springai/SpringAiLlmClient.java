package io.github.tatame.aftersale.agent.infrastructure.springai;

import io.github.tatame.aftersale.agent.application.planner.AgentPlanValidationException;
import io.github.tatame.aftersale.agent.infrastructure.llm.LlmClient;
import io.github.tatame.aftersale.agent.infrastructure.llm.LlmRequest;
import io.github.tatame.aftersale.agent.infrastructure.llm.LlmResponse;
import io.github.tatame.aftersale.common.ai.SpringAiProviderErrorFormatter;
import io.github.tatame.aftersale.common.ai.SpringAiProviderProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;

/**
 * Adapts Spring AI chat output to the existing LlmClient boundary.
 */
public class SpringAiLlmClient implements LlmClient {

    public static final String PROVIDER_NAME = "spring-ai-chat";

    private final SpringAiProviderProperties properties;

    private final Optional<SpringAiChatGateway> chatGateway;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Configuration properties are managed by Spring and read during provider calls.")
    public SpringAiLlmClient(
            SpringAiProviderProperties properties,
            SpringAiChatGateway chatGateway) {
        this.properties = properties;
        this.chatGateway = Optional.ofNullable(chatGateway);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        validateConfiguration();
        try {
            String content = chatGateway.orElseThrow().complete(
                    request.model(),
                    request.systemPrompt(),
                    request.userPrompt());
            return new LlmResponse(content);
        } catch (RuntimeException exception) {
            throw new AgentPlanValidationException(SpringAiProviderErrorFormatter.format(
                    PROVIDER_NAME,
                    request.model(),
                    properties,
                    exception), exception);
        }
    }

    public void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException(SpringAiProviderErrorFormatter.configuration(
                    "agent.spring-ai.enabled must be true when provider=spring-ai-chat",
                    properties));
        }
        if (!properties.isChatEnabled()) {
            throw new IllegalStateException(SpringAiProviderErrorFormatter.configuration(
                    "agent.spring-ai.chat-enabled must be true when provider=spring-ai-chat",
                    properties));
        }
        if (chatGateway.isEmpty()) {
            throw new IllegalStateException(SpringAiProviderErrorFormatter.configuration(
                    "Spring AI chat gateway is not available; check Spring AI model and ChatClient configuration",
                    properties));
        }
    }
}
