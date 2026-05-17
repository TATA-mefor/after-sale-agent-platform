package com.example.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DockerComposeHarnessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void dockerComposeDefinesLocalMysqlAndAppServices() throws IOException {
        String compose = projectText("docker-compose.yml");

        assertThat(compose).contains("mysql:");
        assertThat(compose).contains("app:");
        assertThat(compose).contains("mysql:8.0");
        assertThat(compose).contains("SPRING_PROFILES_ACTIVE: mysql");
        assertThat(compose).contains("AFTERSALE_MYSQL_URL: jdbc:mysql://mysql:3306/after_sale_agent");
        assertThat(compose).contains("8080:8080");
        assertThat(compose).contains("3306:3306");
    }

    @Test
    void dockerComposeUsesMysqlSchemaAndSeedInitialization() throws IOException {
        String compose = projectText("docker-compose.yml");

        assertThat(compose)
                .contains("./src/main/resources/schema-mysql.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro");
        assertThat(compose)
                .contains("./src/main/resources/data-mysql.sql:/docker-entrypoint-initdb.d/02-data.sql:ro");
        assertThat(compose).contains("mysql_data:/var/lib/mysql");
        assertThat(compose).contains("service_healthy");
    }

    @Test
    void dockerComposeDoesNotEnableRedisOrProductionSecrets() throws IOException {
        String compose = projectText("docker-compose.yml");

        assertThat(compose).doesNotContain("redis:");
        assertThat(compose).doesNotContain("OPENAI_API_KEY");
        assertThat(compose).doesNotContain("<local-password>");
        assertThat(compose).doesNotContain("prod");
        assertThat(compose).doesNotContain("production");
    }

    @Test
    void dockerfileBuildsSpringBootJarWithoutChangingRuntimeProfile() throws IOException {
        String dockerfile = projectText("Dockerfile");

        assertThat(dockerfile).contains("maven:3.9.9-eclipse-temurin-17");
        assertThat(dockerfile).contains("eclipse-temurin:17-jre");
        assertThat(dockerfile).contains("mvn -DskipTests package");
        assertThat(dockerfile).contains("EXPOSE 8080");
        assertThat(dockerfile).contains("java", "-jar", "/app/app.jar");
        assertThat(dockerfile).doesNotContain("SPRING_PROFILES_ACTIVE");
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
