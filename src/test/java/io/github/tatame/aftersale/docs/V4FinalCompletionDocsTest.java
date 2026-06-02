package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class V4FinalCompletionDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final List<String> FINAL_DOCS = List.of(
            "README.md",
            "version-updates/EXEC_PLAN_V4.md",
            "version-updates/EXEC_PLAN_V4_RAG_SPRING_AI.md",
            "version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md",
            "version-updates/V4_RELEASE_SUMMARY.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md",
            "docs/demo/V4_PROJECT_HIGHLIGHTS.md");

    @Test
    void finalCompletionRecordAndReleaseSummaryExist() throws IOException {
        String finalRecord = projectText("version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md");
        String releaseSummary = projectText("version-updates/V4_RELEASE_SUMMARY.md");

        assertThat(finalRecord).contains(
                "Status: Completed",
                "Final Scope Completed",
                "Architecture Boundaries Preserved",
                "Default Offline Validation Boundary",
                "Evidence-only / Safety Boundary",
                "Completion Signal",
                "TASK_COMPLETE");
        assertThat(releaseSummary).contains(
                "Status: Completed",
                "What V4 Delivered",
                "How To Validate",
                "How To Demo",
                "What Is Intentionally Not Production / Live",
                "Future Roadmap");
    }

    @Test
    void finalStatusAndLinksAreVisibleInPrimaryDocs() throws IOException {
        String readme = projectText("README.md");
        String execPlan = projectText("version-updates/EXEC_PLAN_V4.md");
        String activePlan = projectText("version-updates/EXEC_PLAN_V4_RAG_SPRING_AI.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String interview = projectText("docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md");
        String highlights = projectText("docs/demo/V4_PROJECT_HIGHLIGHTS.md");

        assertThat(execPlan).contains("Status: Completed", "V4 overall status is completed");
        assertThat(activePlan).contains(
                "Status: Completed / Closed",
                "historical active V4 plan",
                "V4 overall status is completed");
        assertThat(readme).contains(
                "V4 status: completed",
                "version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md",
                "version-updates/V4_RELEASE_SUMMARY.md");
        assertThat(quality).contains("V4 final quality closure", "V4.7.4 V4 Final Completion Record (completed)");
        assertThat(validation).contains(
                "V4 Final Validation",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest",
                "V4 final default validation gate");
        assertThat(interview).contains(
                "V4 status: completed",
                "version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md",
                "version-updates/V4_RELEASE_SUMMARY.md");
        assertThat(highlights).contains(
                "V4 status: completed",
                "version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md",
                "version-updates/V4_RELEASE_SUMMARY.md");
    }

    @Test
    void finalDocsStateOfflineEvidenceToolRegistryAndLiveOptInBoundaries() throws IOException {
        String docs = combinedFinalDocs();

        assertThat(docs).contains(
                "default offline",
                "does not require API keys",
                "does not require real LLMs",
                "RAG evidence is policy evidence",
                "evidence-only",
                "ToolRegistry remains",
                "PGvector is an opt-in",
                "real providers are opt-in");
        assertThat(docs).contains("not a production deployment guide");
    }

    @Test
    void finalDocsDoNotContainLocalPathsSecretsOrCompletionOverclaims() throws IOException {
        for (String path : FINAL_DOCS) {
            String lower = projectText(path).toLowerCase();

            assertThat(lower).as(path).doesNotContain(
                    "d:/",
                    "d:\\",
                    "c:/",
                    "c:\\",
                    "/users/",
                    "api_key=",
                    "openai_api_key=",
                    "dashscope_api_key=",
                    "spring_ai_openai_api_key=",
                    "token=",
                    "secret=",
                    "sk-",
                    "real refund integration completed",
                    "real exchange integration completed",
                    "real payment integration completed",
                    "real logistics integration completed",
                    "production deployment completed",
                    "production monitoring completed",
                    "production auth completed");
        }
    }

    @Test
    void finalDocsKeepFutureWorkOutsideCompletedV4Scope() throws IOException {
        String docs = combinedFinalDocs();

        assertThat(docs).contains(
                "Future Work",
                "V5 production hardening",
                "production auth",
                "JdbcPolicyVectorRepository",
                "live PGvector validation",
                "production ingestion API",
                "real payment",
                "observability and metrics hardening",
                "deployment hardening");
        assertThat(docs).contains("production hardening work is outside this V4 completion scope");
    }

    private static String combinedFinalDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : FINAL_DOCS) {
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
