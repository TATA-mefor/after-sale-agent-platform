package com.example.aftersale.common.infrastructure.mysql;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aftersale.persistence.mysql")
public record MySqlPersistenceProperties(
        String url,
        String username,
        String password,
        String schemaLocation,
        String dataLocation) {
}
