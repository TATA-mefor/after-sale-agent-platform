package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductionConfigTemplateDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final List<String> STAGE_ONE_DOCS = List.of(
            "README.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE1_PROD_CONFIG_TEMPLATE.md");

    @Test
    void productionTemplateAndDocsExistAndAreLinked() throws IOException {
        String template = projectText("src/main/resources/application-prod.example.yml");
        String templateDoc = projectText("docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md");
        String readme = projectText("README.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String plan = projectText(
                "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE1_PROD_CONFIG_TEMPLATE.md");

        assertThat(template).contains("on-profile: prod", "SPRING_DATASOURCE_PASSWORD");
        assertThat(templateDoc).contains(
                "src/main/resources/application-prod.example.yml",
                "not loaded by default",
                "not a production deployment manifest");
        assertThat(readme).contains("docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md");
        assertThat(validation).contains("ProductionConfigTemplateDocsTest");
        assertThat(plan).contains("Status: Completed", "TASK_COMPLETE");
    }

    @Test
    void productionTemplateUsesPlaceholdersAndKeepsSensitiveDefaultsEmpty() throws IOException {
        String template = projectText("src/main/resources/application-prod.example.yml");

        assertThat(template).contains(
                "${SPRING_DATASOURCE_URL:}",
                "${SPRING_DATASOURCE_USERNAME:}",
                "${SPRING_DATASOURCE_PASSWORD:}",
                "${OPENAI_API_KEY:}",
                "${DASHSCOPE_API_KEY:}",
                "${SPRING_AI_OPENAI_API_KEY:}",
                "${AFTERSALE_PGVECTOR_PASSWORD:}");
        assertThat(template).contains(
                "include: health",
                "show-actuator: false",
                "enabled: ${AFTERSALE_SWAGGER_UI_ENABLED:false}");
    }

    @Test
    void docsStateDefaultOfflineAndTemplateNotLoadedByDefault() throws IOException {
        String docs = combinedStageOneDocs();

        assertThat(docs).contains(
                "not loaded by default",
                "不会被默认测试 profile 加载",
                "默认验证",
                "does not require real LLMs",
                "API keys",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network");
    }

    @Test
    void docsKeepProductionHardeningOutsideStageOneCompletion() throws IOException {
        String docs = combinedStageOneDocs();

        assertThat(docs).contains(
                "production authentication",
                "secret manager",
                "production monitoring",
                "CI/CD",
                "Kubernetes",
                "Helm",
                "live PGvector validation",
                "后续阶段",
                "not a production deployment manifest");
        assertThat(docs.toLowerCase()).doesNotContain(
                "production auth completed",
                "production monitoring completed",
                "production deployment completed",
                "secret manager integration completed",
                "live pgvector validation completed",
                "jdbcpolicyvectorrepository implementation completed",
                "real refund integration completed",
                "real exchange integration completed",
                "real payment integration completed",
                "real logistics integration completed");
    }

    @Test
    void templateAndDocsDoNotContainSecretsLocalPathsOrRawDatasets() throws IOException {
        String template = projectText("src/main/resources/application-prod.example.yml");
        assertSafeText("src/main/resources/application-prod.example.yml", template);

        for (String path : STAGE_ONE_DOCS) {
            assertSafeText(path, projectText(path));
        }
    }

    private static void assertSafeText(String path, String text) {
        String lower = text.toLowerCase();

        assertThat(lower).as(path).doesNotContain(
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "sk-");
    }

    private static String combinedStageOneDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_ONE_DOCS) {
            builder.append(projectText(path)).append('\n');
        }
        return builder.toString();
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
