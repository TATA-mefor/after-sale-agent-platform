package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class V4InterviewDemoDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final List<String> INTERVIEW_DOCS = List.of(
            "README.md",
            "docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md",
            "docs/demo/V4_PROJECT_HIGHLIGHTS.md",
            "docs/demo/V4_RAG_DEMO_SCRIPT.md",
            "docs/api/OPENAPI.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_V4_INTERVIEW_DEMO_README_POLISH.md");

    @Test
    void interviewDemoDocsExistAndAreLinkedFromReadme() throws IOException {
        assertThat(Files.exists(PROJECT_ROOT.resolve("docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md"))).isTrue();
        assertThat(Files.exists(PROJECT_ROOT.resolve("docs/demo/V4_PROJECT_HIGHLIGHTS.md"))).isTrue();

        String readme = projectText("README.md");

        assertThat(readme).contains(
                "docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md",
                "docs/demo/V4_PROJECT_HIGHLIGHTS.md",
                "docs/demo/V4_RAG_DEMO_SCRIPT.md",
                "docs/demo/V4_POLICY_INGESTION_PIPELINE.md",
                "docs/demo/V4_PGVECTOR_LOCAL_SETUP.md",
                "docs/evaluation/EVALUATION.md",
                "docs/api/OPENAPI.md",
                "docs/quality/VALIDATION_COMMANDS.md");
    }

    @Test
    void readmeContainsDefaultOfflineValidationCommandsAndBoundary() throws IOException {
        String readme = projectText("README.md");

        assertThat(readme).contains(
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest",
                "Default validation does not require real LLMs",
                "API keys",
                "PGvector",
                "Docker");
    }

    @Test
    void ragDemoOpenApiAndValidationDocsContainInterviewGuidance() throws IOException {
        String ragDemo = projectText("docs/demo/V4_RAG_DEMO_SCRIPT.md");
        String openApi = projectText("docs/api/OPENAPI.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");

        assertThat(ragDemo).contains(
                "How To Present This Demo In An Interview",
                "If You Only Show Tests",
                "docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md");
        assertThat(openApi).contains(
                "Interview Swagger UI Walkthrough",
                "`search_aftersale_policy`",
                "remains a ToolRegistry tool",
                "They do not add a new public RAG policy-search endpoint");
        assertThat(validation).contains(
                "Interview Safe Validation Commands",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest",
                "Live validation remains explicit opt-in only");
    }

    @Test
    void interviewDocsStateEvidenceOnlyAndOfflineBoundaries() throws IOException {
        String docs = combinedInterviewDocs();

        assertThat(docs).contains(
                "RAG evidence is policy evidence only",
                "evidence-only",
                "LOW-risk read-only",
                "ToolRegistry",
                "default offline",
                "does not require real LLMs",
                "does not require live PGvector",
                "production deployment");
        assertThat(docs).contains("not a production deployment guide");
    }

    @Test
    void interviewDocsDoNotContainLocalPathsSecretsOrOverclaims() throws IOException {
        for (String path : INTERVIEW_DOCS) {
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
                    "已退款成功",
                    "已换货完成",
                    "已补偿到账",
                    "production deployment completed",
                    "production monitoring completed",
                    "real refund integration completed",
                    "real exchange integration completed",
                    "real payment integration completed",
                    "real logistics integration completed");
        }
    }

    @Test
    void completedPlanContainsRequiredCompletionSignal() throws IOException {
        String completed = projectText(
                "version-updates/EXEC_PLAN_V4_INTERVIEW_DEMO_README_POLISH.md");

        assertThat(completed).contains(
                "Status: Completed",
                "README Polish Boundary",
                "Interview Demo Boundary",
                "Project Highlights Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Demo Boundary",
                "TASK_COMPLETE");
    }

    private static String combinedInterviewDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : INTERVIEW_DOCS) {
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
