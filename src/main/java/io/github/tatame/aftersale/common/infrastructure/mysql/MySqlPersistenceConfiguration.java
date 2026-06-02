package io.github.tatame.aftersale.common.infrastructure.mysql;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * 配置显式 mysql profile 下的 JDBC 数据源和启动 SQL 初始化。
 *
 * <p>边界：MySQL 持久化必须显式启用；默认 profile 继续使用内存仓储，确保默认 mvn test 不依赖本地
 * MySQL、Docker 或外部服务。
 */
@Configuration
@Profile("mysql")
@EnableConfigurationProperties(MySqlPersistenceProperties.class)
public class MySqlPersistenceConfiguration {

    @Bean
    public DataSource dataSource(MySqlPersistenceProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(properties.url());
        dataSource.setUsername(properties.username());
        dataSource.setPassword(properties.password());
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(
            DataSource dataSource,
            MySqlPersistenceProperties properties,
            ResourceLoader resourceLoader) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        // 当前 V3 Demo 使用启动 SQL；频繁变更 schema 时应迁移到正式 migration 工具。
        populator.addScript(resourceLoader.getResource(properties.schemaLocation()));
        populator.addScript(resourceLoader.getResource(properties.dataLocation()));
        populator.setContinueOnError(false);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
