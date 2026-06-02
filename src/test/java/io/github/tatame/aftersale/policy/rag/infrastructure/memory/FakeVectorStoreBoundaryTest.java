package io.github.tatame.aftersale.policy.rag.infrastructure.memory;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class FakeVectorStoreBoundaryTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FakePolicyVectorRepositoryConfiguration.class);

    @Test
    void defaultContextDoesNotCreateFakeRepositoryOrExternalClients() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(PolicyVectorRepository.class);
            assertThat(context).doesNotHaveBean(DataSource.class);
            assertThat(context).doesNotHaveBean(JdbcTemplate.class);
            assertThat(beanNamesContaining(context.getBeanDefinitionNames(), "VectorStore")).isEmpty();
            assertThat(beanNamesContaining(context.getBeanDefinitionNames(), "Embedding")).isEmpty();
        });
    }

    @Test
    void fakeProviderCreatesInMemoryRepositoryOnly() {
        contextRunner
                .withPropertyValues("agent.rag.vector-store.provider=fake")
                .run(context -> {
                    assertThat(context).hasSingleBean(PolicyVectorRepository.class);
                    assertThat(context.getBean(PolicyVectorRepository.class))
                            .isInstanceOf(InMemoryPolicyVectorRepository.class);
                    assertThat(context).doesNotHaveBean(DataSource.class);
                    assertThat(context).doesNotHaveBean(JdbcTemplate.class);
                    assertThat(beanNamesContaining(context.getBeanDefinitionNames(), "VectorStore")).isEmpty();
                });
    }

    private static String[] beanNamesContaining(String[] beanNames, String text) {
        return java.util.Arrays.stream(beanNames)
                .filter(beanName -> beanName.contains(text))
                .toArray(String[]::new);
    }
}
