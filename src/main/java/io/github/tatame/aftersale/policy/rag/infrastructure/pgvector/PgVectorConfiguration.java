package io.github.tatame.aftersale.policy.rag.infrastructure.pgvector;

import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Holds the explicit PGvector profile boundary without enabling it in the default profile.
 */
@Configuration(proxyBeanMethods = false)
@Profile("rag-postgres")
@EnableConfigurationProperties(PgVectorProperties.class)
public class PgVectorConfiguration {

    @Bean
    public PgVectorProfileGuard pgVectorProfileGuard(PgVectorProperties properties) {
        properties.validate();
        return new PgVectorProfileGuard(
                properties.enabled(),
                properties.jdbcUrl(),
                properties.username(),
                properties.schema(),
                properties.initializeSchema(),
                properties.dimensions());
    }

    @Bean
    @ConditionalOnProperty(name = "agent.rag.vector-store.provider", havingValue = "pgvector")
    @ConditionalOnBean(NamedParameterJdbcOperations.class)
    public PolicyVectorRepository policyVectorRepository(
            NamedParameterJdbcOperations pgVectorJdbcOperations,
            PgVectorProperties properties) {
        return new JdbcPolicyVectorRepository(pgVectorJdbcOperations, properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "agent.rag.vector-store.provider", havingValue = "pgvector")
    static class PgVectorJdbcConfiguration {

        @Bean
        @ConditionalOnProperty(name = "agent.rag.vector-store.pgvector.enabled", havingValue = "true")
        DataSource pgVectorDataSource(PgVectorProperties properties) {
            properties.validate();
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(properties.jdbcUrl());
            dataSource.setUsername(properties.username());
            dataSource.setPassword(properties.password());
            return dataSource;
        }

        @Bean
        @ConditionalOnProperty(name = "agent.rag.vector-store.pgvector.enabled", havingValue = "true")
        NamedParameterJdbcOperations pgVectorJdbcOperations(DataSource pgVectorDataSource) {
            return new NamedParameterJdbcTemplate(pgVectorDataSource);
        }
    }
}
