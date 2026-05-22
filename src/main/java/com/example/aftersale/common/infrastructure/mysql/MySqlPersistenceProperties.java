package com.example.aftersale.common.infrastructure.mysql;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 mysql profile 使用的持久化配置。
 *
 * <p>边界：配置值来自外部环境或 application 配置，代码注释和测试不得写入真实密码或访问令牌。
 */
@ConfigurationProperties(prefix = "aftersale.persistence.mysql")
public record MySqlPersistenceProperties(
        String url,
        String username,
        String password,
        String schemaLocation,
        String dataLocation) {
}
