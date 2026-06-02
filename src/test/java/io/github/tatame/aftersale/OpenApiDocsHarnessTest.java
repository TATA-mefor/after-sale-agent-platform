package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiDocsHarnessTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void openApiDocsAndRoadmapDescribeV464Boundary() throws IOException {
        String openApi = projectText("docs/api/OPENAPI.md");
        String readme = projectText("README.md");
        String activePlan = projectText("docs/exec-plans/active/EXEC_PLAN_V4_RAG_SPRING_AI.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String completed = projectText("docs/exec-plans/completed/EXEC_PLAN_V4_OPENAPI_API_DOCS.md");
        String activePlanV464 = sectionFrom(activePlan, "### 10.4 V4.6.4");
        String qualityV464 = sectionFrom(quality, "### V4.6.4");

        assertThat(openApi).contains(
                "OpenAPI / Swagger UI",
                "http://localhost:8080/swagger-ui/index.html",
                "http://localhost:8080/v3/api-docs",
                "http://localhost:8080/actuator/health",
                "default profile",
                "evidence-only",
                "not a production deployment guide",
                "does not add runtime behavior");
        assertThat(readme).contains("docs/api/OPENAPI.md", "/swagger-ui/index.html", "/v3/api-docs");
        assertThat(activePlan).contains("V4.6.4", "completed", "OpenAPI / API docs polish");
        assertThat(quality).contains("V4.6.4 OpenAPI / API Docs Polish (completed)");
        assertThat(completed).contains(
                "Status: Completed",
                "OpenAPI Boundary",
                "Swagger UI Boundary",
                "Evidence-only Documentation Boundary",
                "Security / Secret Safety Boundary",
                "TASK_COMPLETE");

        assertSecretSafe(openApi + activePlanV464 + qualityV464 + completed);
        assertReadmeOpenApiSectionSecretSafe(readme);
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static void assertSecretSafe(String text) {
        String lower = text.toLowerCase();
        assertThat(lower).doesNotContain("sk-");
        assertThat(lower).doesNotContain("api_key=");
        assertThat(lower).doesNotContain("password=prod");
        assertThat(lower).doesNotContain("jdbc:postgresql://prod");
        assertThat(lower).doesNotContain("jdbc:postgresql://production");
        assertThat(lower).doesNotContain("d:\\");
        assertThat(lower).doesNotContain("c:\\");
        assertThat(lower).doesNotContain("/users/");
        assertThat(lower).doesNotContain("data/raw/");
        assertThat(lower).doesNotContain("full prompt");
        assertThat(lower).doesNotContain("已退款成功");
        assertThat(lower).doesNotContain("已换货完成");
        assertThat(lower).doesNotContain("已补偿到账");
    }

    private static void assertReadmeOpenApiSectionSecretSafe(String readme) {
        String section = sectionFrom(readme, "### V4.6.4 OpenAPI / API Docs Polish");
        assertSecretSafe(section);
    }

    private static String sectionFrom(String text, String header) {
        int sectionStart = text.indexOf(header);
        assertThat(sectionStart).as("document should contain " + header).isNotNegative();
        int sectionEnd = text.indexOf("\n## ", sectionStart + 1);
        return sectionEnd < 0 ? text.substring(sectionStart) : text.substring(sectionStart, sectionEnd);
    }
}
