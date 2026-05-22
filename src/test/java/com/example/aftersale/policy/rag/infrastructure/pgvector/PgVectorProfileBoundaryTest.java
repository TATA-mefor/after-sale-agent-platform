package com.example.aftersale.policy.rag.infrastructure.pgvector;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class PgVectorProfileBoundaryTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PgVectorConfiguration.class);

    @Test
    void defaultContextDoesNotCreatePgVectorOrDataSourceBeans() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(PgVectorProfileGuard.class);
            assertThat(context).doesNotHaveBean(DataSource.class);
            assertThat(beanNamesContaining(context.getBeanDefinitionNames(), "VectorStore")).isEmpty();
        });
    }

    @Test
    void pgVectorPropertiesBindAsDisabledByDefault() {
        new ApplicationContextRunner()
                .withUserConfiguration(PgVectorPropertiesBindingConfiguration.class)
                .withPropertyValues(
                        "agent.rag.vector-store.pgvector.schema=public",
                        "agent.rag.vector-store.pgvector.dimensions=1536")
                .run(context -> {
                    PgVectorProperties properties = context.getBean(PgVectorProperties.class);

                    assertThat(properties.enabled()).isFalse();
                    assertThat(properties.schema()).isEqualTo("public");
                    assertThat(properties.dimensions()).isEqualTo(1536);
                });
    }

    @Test
    void enabledPgVectorWithoutRequiredSettingsFailsWithoutPasswordLeak() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=rag-postgres",
                        "agent.rag.vector-store.pgvector.enabled=true",
                        "agent.rag.vector-store.pgvector.password=secret-value",
                        "agent.rag.vector-store.pgvector.dimensions=1536")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("AFTERSALE_PGVECTOR_URL")
                            .hasMessageContaining("AFTERSALE_PGVECTOR_USERNAME");
                    assertThat(context.getStartupFailure().getMessage()).doesNotContain("secret-value");
                });
    }

    @Test
    void completePgVectorConfigurationCreatesGuardOnly() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=rag-postgres",
                        "agent.rag.vector-store.pgvector.enabled=true",
                        "agent.rag.vector-store.pgvector.jdbc-url=jdbc:postgresql://localhost:5432/aftersale_rag",
                        "agent.rag.vector-store.pgvector.username=aftersale_rag",
                        "agent.rag.vector-store.pgvector.password=secret-value",
                        "agent.rag.vector-store.pgvector.schema=rag",
                        "agent.rag.vector-store.pgvector.initialize-schema=false",
                        "agent.rag.vector-store.pgvector.dimensions=1024")
                .run(context -> {
                    PgVectorProfileGuard guard = context.getBean(PgVectorProfileGuard.class);

                    assertThat(guard.enabled()).isTrue();
                    assertThat(guard.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/aftersale_rag");
                    assertThat(guard.username()).isEqualTo("aftersale_rag");
                    assertThat(guard.schema()).isEqualTo("rag");
                    assertThat(guard.initializeSchema()).isFalse();
                    assertThat(guard.dimensions()).isEqualTo(1024);
                    assertThat(context).doesNotHaveBean(DataSource.class);
                    assertThat(beanNamesContaining(context.getBeanDefinitionNames(), "VectorStore")).isEmpty();
                });
    }

    private static String[] beanNamesContaining(String[] beanNames, String text) {
        return java.util.Arrays.stream(beanNames)
                .filter(beanName -> beanName.contains(text))
                .toArray(String[]::new);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PgVectorProperties.class)
    static class PgVectorPropertiesBindingConfiguration {
    }
}
