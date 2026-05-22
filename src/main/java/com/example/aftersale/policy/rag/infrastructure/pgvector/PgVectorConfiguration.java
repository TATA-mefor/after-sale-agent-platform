package com.example.aftersale.policy.rag.infrastructure.pgvector;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Holds the explicit PGvector profile boundary without creating database or VectorStore clients.
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
}
