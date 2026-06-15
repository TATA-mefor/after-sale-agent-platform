package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ContainerCiHardeningDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String CONTAINER_DOC = "docs/deploy/CONTAINER_CI_HARDENING.md";

    private static final String CANONICAL_COMPLETION =
            "version-updates/EXEC_PLAN_V5_B1_CONTAINER_CI.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B1_CONTAINER_CI.md";

    private static final List<String> V5_B1_DOCS = List.of(
            "README.md",
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "version-updates/V4_RELEASE_SUMMARY.md",
            "version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md",
            CONTAINER_DOC,
            CANONICAL_COMPLETION,
            COMPLETED_PLAN);

    @Test
    void containerCiFilesExist() {
        assertProjectFileExists("Dockerfile");
        assertProjectFileExists(".dockerignore");
        assertProjectFileExists(".github/workflows/ci.yml");
        assertProjectFileExists(CONTAINER_DOC);
        assertProjectFileExists(CANONICAL_COMPLETION);
        assertProjectFileExists(COMPLETED_PLAN);
    }

    @Test
    void dockerfileUsesMultiStageNonRootRuntimeWithoutLiveDefaults() throws IOException {
        String dockerfile = projectText("Dockerfile");
        String lower = dockerfile.toLowerCase(Locale.ROOT);

        assertThat(dockerfile).contains(
                " AS build",
                "FROM eclipse-temurin:17-jre",
                "USER aftersale",
                "java $JAVA_OPTS -jar /app/app.jar");
        assertThat(lower).doesNotContain(
                "api_key",
                "password",
                "token",
                "live.rag=true",
                "rag-postgres",
                ".env",
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "/home/");
    }

    @Test
    void dockerignoreExcludesSecretsLocalArtifactsAndBuildOutput() throws IOException {
        String dockerignore = projectText(".dockerignore");

        assertThat(dockerignore).contains(
                ".git",
                "target",
                ".env",
                ".env*",
                ".idea",
                ".vscode",
                "*.pem",
                "*.key",
                "*.p12",
                "*.jks",
                "secrets/",
                "credentials/",
                "logs/",
                "tmp/",
                "data/local/",
                "docker-build/");
    }

    @Test
    void ciRunsDefaultOfflineQualityGateAndDockerBuildOnly() throws IOException {
        String workflow = projectText(".github/workflows/ci.yml");
        String lower = workflow.toLowerCase(Locale.ROOT);

        assertThat(workflow).contains(
                "mvn -B --no-transfer-progress test",
                "mvn -B --no-transfer-progress checkstyle:check",
                "mvn -B --no-transfer-progress spotbugs:check",
                "mvn -B --no-transfer-progress test -Dtest=ArchitectureTest",
                "docker build -t after-sale-agent-platform:ci .");
        assertThat(lower).doesNotContain(
                "docker push",
                "docker login",
                "live.rag=true",
                "live.llm=true",
                "live.spring-ai=true",
                "live.embedding=true",
                "live.pgvector=true",
                "secrets.");
    }

    @Test
    void v5B1DocsRecordStatusBoundariesAndPackageNamespace() throws IOException {
        String readme = projectText("README.md");
        String roadmap = projectText("docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md");
        String productionConfig = projectText("docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String remediation = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String containerDoc = projectText(CONTAINER_DOC);
        String completion = projectText(CANONICAL_COMPLETION);
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(readme).contains(CONTAINER_DOC, "Container + CI");
        String lowerRoadmap = roadmap.toLowerCase(Locale.ROOT);
        assertThat(roadmap).contains("V5.B.1");
        assertThat(lowerRoadmap).contains("foundation completed");
        assertThat(lowerRoadmap).contains("v5.b.2.2", "v5.b.2.3", "v5.b.3", "v5.b.4");
        assertThat(productionConfig).contains("Container / CI Usage", "does not contain secrets");
        assertThat(validation).contains("V5.B.1 Container + CI Validation", "Docker build validation");
        assertThat(quality).contains("V5.B.1 Container + CI Foundation", "Dockerfile", "CI quality gate");
        assertThat(remediation).contains(
                "V5.B.1：已完成",
                "V5.B.2.1：已完成",
                "V5.B.2.2：已完成",
                "V5.B.2.3：已完成");
        assertThat(containerDoc).contains("io.github.tatame.aftersale", "Default Offline Boundary");
        assertThat(completion).contains("Status: Completed", "TASK_COMPLETE");
        assertThat(completedPlan).contains("Status: Completed", "TASK_COMPLETE");
    }

    @Test
    void v5B1DocsKeepOfflineLiveAndProductionBoundaries() throws IOException {
        String docs = combinedV5B1Docs();

        assertThat(docs).contains(
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network",
                "Live tests remain explicit opt-in",
                "not a production deployment",
                "not a CD pipeline",
                "not a registry");
    }

    @Test
    void v5B1DocsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : V5_B1_DOCS) {
            assertSafeText(path, projectText(path));
        }
    }

    private static void assertSafeText(String path, String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        assertThat(lower).as(path).doesNotContain(
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "/home/",
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "sk-",
                "production deployment completed",
                "kubernetes completed",
                "helm completed",
                "secret manager completed",
                "production auth completed",
                "production monitoring completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedV5B1Docs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : V5_B1_DOCS) {
            builder.append(projectText(path)).append('\n');
        }
        return builder.toString();
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static void assertProjectFileExists(String path) {
        assertThat(Files.exists(PROJECT_ROOT.resolve(path))).as(path + " should exist").isTrue();
    }
}
