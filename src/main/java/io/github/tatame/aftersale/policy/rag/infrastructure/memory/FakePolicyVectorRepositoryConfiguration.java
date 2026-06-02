package io.github.tatame.aftersale.policy.rag.infrastructure.memory;

import io.github.tatame.aftersale.policy.rag.domain.CosineSimilarityCalculator;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the opt-in fake vector repository without enabling RAG runtime behavior.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "agent.rag.vector-store.provider", havingValue = "fake")
public class FakePolicyVectorRepositoryConfiguration {

    @Bean
    CosineSimilarityCalculator cosineSimilarityCalculator() {
        return new CosineSimilarityCalculator();
    }

    @Bean
    PolicyVectorRepository policyVectorRepository(CosineSimilarityCalculator cosineSimilarityCalculator) {
        return new InMemoryPolicyVectorRepository(cosineSimilarityCalculator);
    }
}
