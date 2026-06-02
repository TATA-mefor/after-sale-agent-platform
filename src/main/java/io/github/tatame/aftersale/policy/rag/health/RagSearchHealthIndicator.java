package io.github.tatame.aftersale.policy.rag.health;

import io.github.tatame.aftersale.policy.rag.application.RagPolicySearchApplicationService;
import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import java.util.Arrays;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agent.rag.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagSearchHealthIndicator implements HealthIndicator {

    private final ObjectProvider<RagPolicySearchApplicationService> searchServiceProvider;
    private final boolean includeDetails;

    public RagSearchHealthIndicator(
            ObjectProvider<RagPolicySearchApplicationService> searchServiceProvider,
            RagHealthProperties properties) {
        this.searchServiceProvider = searchServiceProvider;
        this.includeDetails = properties.isIncludeDetails();
    }

    @Override
    public Health health() {
        RagPolicySearchApplicationService searchService = searchServiceProvider.getIfAvailable();
        Health.Builder builder = searchService == null ? Health.down() : Health.up();
        if (includeDetails) {
            builder.withDetail("component", "rag-policy-search")
                    .withDetail("searchServiceAvailable", searchService != null)
                    .withDetail("supportedRetrievalModes", supportedModes())
                    .withDetail("liveSearchExecuted", false)
                    .withDetail("message", searchService == null
                            ? "RAG policy search service bean is missing."
                            : "RAG search contract is available; no live search was executed.");
        }
        return builder.build();
    }

    private static String supportedModes() {
        return String.join(",", Arrays.stream(RetrievalMode.values())
                .map(Enum::name)
                .toList());
    }
}
