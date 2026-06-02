package io.github.tatame.aftersale.policy.rag.health;

import io.github.tatame.aftersale.policy.rag.ingestion.application.PolicyChunkingService;
import io.github.tatame.aftersale.policy.rag.ingestion.application.PolicyContentChecksumService;
import io.github.tatame.aftersale.policy.rag.ingestion.application.PolicyEmbeddingPipelineService;
import io.github.tatame.aftersale.policy.rag.ingestion.application.PolicyIngestionDedupService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agent.rag.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagIngestionHealthIndicator implements HealthIndicator {

    private final ObjectProvider<PolicyChunkingService> chunkingServiceProvider;
    private final ObjectProvider<PolicyContentChecksumService> checksumServiceProvider;
    private final ObjectProvider<PolicyIngestionDedupService> dedupServiceProvider;
    private final ObjectProvider<PolicyEmbeddingPipelineService> pipelineServiceProvider;
    private final boolean includeDetails;

    public RagIngestionHealthIndicator(
            ObjectProvider<PolicyChunkingService> chunkingServiceProvider,
            ObjectProvider<PolicyContentChecksumService> checksumServiceProvider,
            ObjectProvider<PolicyIngestionDedupService> dedupServiceProvider,
            ObjectProvider<PolicyEmbeddingPipelineService> pipelineServiceProvider,
            RagHealthProperties properties) {
        this.chunkingServiceProvider = chunkingServiceProvider;
        this.checksumServiceProvider = checksumServiceProvider;
        this.dedupServiceProvider = dedupServiceProvider;
        this.pipelineServiceProvider = pipelineServiceProvider;
        this.includeDetails = properties.isIncludeDetails();
    }

    @Override
    public Health health() {
        boolean chunkingAvailable = chunkingServiceProvider.getIfAvailable() != null;
        boolean checksumAvailable = checksumServiceProvider.getIfAvailable() != null;
        boolean dedupAvailable = dedupServiceProvider.getIfAvailable() != null;
        boolean pipelineAvailable = pipelineServiceProvider.getIfAvailable() != null;
        Health.Builder builder = Health.up();
        if (includeDetails) {
            builder.withDetail("component", "rag-ingestion")
                    .withDetail("chunkingContractAvailable", true)
                    .withDetail("checksumContractAvailable", true)
                    .withDetail("dedupContractAvailable", true)
                    .withDetail("embeddingPipelineContractAvailable", true)
                    .withDetail("chunkingBeanAvailable", chunkingAvailable)
                    .withDetail("checksumBeanAvailable", checksumAvailable)
                    .withDetail("dedupBeanAvailable", dedupAvailable)
                    .withDetail("embeddingPipelineBeanAvailable", pipelineAvailable)
                    .withDetail("fileReadExecuted", false)
                    .withDetail("chunkingExecuted", false)
                    .withDetail("embeddingCallExecuted", false)
                    .withDetail("repositoryWriteExecuted", false)
                    .withDetail("message", "Ingestion contracts are present; no ingestion work was executed.");
        }
        return builder.build();
    }
}
