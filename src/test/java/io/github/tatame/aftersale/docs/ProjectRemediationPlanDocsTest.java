package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectRemediationPlanDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void remediationPlanExistsAndIsLinked() throws IOException {
        String plan = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String readme = projectText("README.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");

        assertThat(readme).contains("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        assertThat(validation).contains("ProjectRemediationPlanDocsTest");
        assertThat(plan).contains("Status: Completed", "TASK_COMPLETE");
    }

    @Test
    void remediationPlanCoversReviewDimensionsAndBoundaries() throws IOException {
        String plan = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");

        assertThat(plan).contains(
                "Spring Boot 工程",
                "Spring AI 使用",
                "RAG 与 Tool",
                "API 调用",
                "部署",
                "阶段化整改路线");
        assertThat(plan).contains(
                "RAG evidence 是政策证据",
                "ToolRegistry tool",
                "admin/offline pipeline",
                "future work",
                "默认离线");
    }

    @Test
    void remediationPlanDoesNotOverclaimRuntimeOrProductionCompletion() throws IOException {
        String lower = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md").toLowerCase();

        assertThat(lower).doesNotContain(
                "real refund integration completed",
                "real exchange integration completed",
                "real payment integration completed",
                "real logistics integration completed",
                "production deployment completed",
                "production monitoring completed",
                "production auth completed",
                "jdbcpolicyvectorrepository implementation completed",
                "spring ai vectorstore production path completed");
    }

    @Test
    void remediationPlanDoesNotContainSecretsOrLocalPaths() throws IOException {
        String lower = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md").toLowerCase();

        assertThat(lower).doesNotContain(
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "token=",
                "secret=",
                "sk-");
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
