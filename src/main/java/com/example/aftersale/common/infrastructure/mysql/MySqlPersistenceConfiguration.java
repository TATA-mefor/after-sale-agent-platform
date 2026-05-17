package com.example.aftersale.common.infrastructure.mysql;

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
        populator.addScript(resourceLoader.getResource(properties.schemaLocation()));
        populator.addScript(resourceLoader.getResource(properties.dataLocation()));
        populator.setContinueOnError(false);

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
