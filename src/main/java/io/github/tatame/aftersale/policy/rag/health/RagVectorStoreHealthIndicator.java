package io.github.tatame.aftersale.policy.rag.health;

import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.infrastructure.pgvector.PgVectorProperties;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agent.rag.health", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagVectorStoreHealthIndicator implements HealthIndicator {

    private final String provider;
    private final ObjectProvider<List<PolicyVectorRepository>> vectorRepositoriesProvider;
    private final ObjectProvider<PgVectorProperties> pgVectorPropertiesProvider;
    private final boolean includeDetails;

    public RagVectorStoreHealthIndicator(
            @Value("${agent.rag.vector-store.provider:none}") String provider,
            ObjectProvider<List<PolicyVectorRepository>> vectorRepositoriesProvider,
            ObjectProvider<PgVectorProperties> pgVectorPropertiesProvider,
            RagHealthProperties properties) {
        this.provider = provider;
        this.vectorRepositoriesProvider = vectorRepositoriesProvider;
        this.pgVectorPropertiesProvider = pgVectorPropertiesProvider;
        this.includeDetails = properties.isIncludeDetails();
    }

    @Override
    public Health health() {
        String normalizedProvider = RagHealthDetailSanitizer.provider(provider);
        List<PolicyVectorRepository> repositories = vectorRepositoriesProvider.getIfAvailable(List::of);
        return switch (normalizedProvider) {
            case "fake" -> fakeHealth(repositories);
            case "pgvector" -> pgVectorHealth();
            case "none", "disabled" -> disabledHealth(normalizedProvider, repositories);
            default -> unknownProviderHealth(normalizedProvider, repositories);
        };
    }

    private Health fakeHealth(List<PolicyVectorRepository> repositories) {
        boolean available = !repositories.isEmpty();
        Health.Builder builder = available ? Health.up() : Health.down();
        if (includeDetails) {
            baseDetails(builder, "fake", repositories)
                    .withDetail("message", available
                            ? "Fake in-memory vector repository bean is available; no vector search was executed."
                            : "Fake vector provider is configured but no PolicyVectorRepository bean is available.");
        }
        return builder.build();
    }

    private Health pgVectorHealth() {
        PgVectorProperties pgVectorProperties = pgVectorPropertiesProvider.getIfAvailable();
        boolean configured = pgVectorProperties != null
                && pgVectorProperties.enabled()
                && !RagHealthDetailSanitizer.isBlank(pgVectorProperties.jdbcUrl())
                && !RagHealthDetailSanitizer.isBlank(pgVectorProperties.username())
                && !RagHealthDetailSanitizer.isBlank(pgVectorProperties.password())
                && pgVectorProperties.dimensions() > 0;
        Health.Builder builder = configured ? Health.status(Status.UNKNOWN) : Health.down();
        if (includeDetails) {
            builder.withDetail("component", "rag-vector-store")
                    .withDetail("provider", "pgvector")
                    .withDetail("offlineReadinessOnly", true)
                    .withDetail("databaseConnectionAttempted", false)
                    .withDetail("vectorSearchExecuted", false)
                    .withDetail("pgvectorEnabled", pgVectorProperties != null && pgVectorProperties.enabled())
                    .withDetail("jdbcLocation", pgVectorProperties == null
                            ? "not-configured"
                            : RagHealthDetailSanitizer.safeJdbcLocation(pgVectorProperties.jdbcUrl()))
                    .withDetail("usernameConfigured", pgVectorProperties != null
                            && !RagHealthDetailSanitizer.isBlank(pgVectorProperties.username()))
                    .withDetail("passwordConfigured", pgVectorProperties != null
                            && !RagHealthDetailSanitizer.isBlank(pgVectorProperties.password()))
                    .withDetail("dimensions", pgVectorProperties == null ? 0 : pgVectorProperties.dimensions())
                    .withDetail("message", configured
                            ? "PGvector configuration is present; live connectivity is not checked by this indicator."
                            : "PGvector provider is configured but required offline configuration is missing.");
        }
        return builder.build();
    }

    private Health disabledHealth(String normalizedProvider, List<PolicyVectorRepository> repositories) {
        Health.Builder builder = Health.up();
        if (includeDetails) {
            baseDetails(builder, normalizedProvider, repositories)
                    .withDetail("disabled", true)
                    .withDetail("message", "Vector store provider is disabled; KEYWORD retrieval remains available.");
        }
        return builder.build();
    }

    private Health unknownProviderHealth(String normalizedProvider, List<PolicyVectorRepository> repositories) {
        Health.Builder builder = Health.down();
        if (includeDetails) {
            baseDetails(builder, normalizedProvider, repositories)
                    .withDetail("message", "Unknown vector store provider configuration.");
        }
        return builder.build();
    }

    private Health.Builder baseDetails(
            Health.Builder builder,
            String normalizedProvider,
            List<PolicyVectorRepository> repositories) {
        return builder.withDetail("component", "rag-vector-store")
                .withDetail("provider", normalizedProvider)
                .withDetail("repositoryBeans", repositories.size())
                .withDetail("offlineReadinessOnly", true)
                .withDetail("databaseConnectionAttempted", false)
                .withDetail("vectorSearchExecuted", false);
    }
}
