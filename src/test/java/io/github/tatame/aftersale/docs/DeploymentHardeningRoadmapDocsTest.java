package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class DeploymentHardeningRoadmapDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC =
            "docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md";

    private static final String ROADMAP_DOC = "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md";

    private static final String COMPLETED_PLAN =
            "version-updates/"
                    + "EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE6_DEPLOYMENT_HARDENING_ROADMAP.md";

    private static final List<String> STAGE_SIX_DOCS = List.of(
            "README.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "version-updates/V4_RELEASE_SUMMARY.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            DECISION_DOC,
            ROADMAP_DOC,
            COMPLETED_PLAN);

    @Test
    void deploymentHardeningDocsAndCompletionRecordExist() throws IOException {
        String decision = projectText(DECISION_DOC);
        String roadmap = projectText(ROADMAP_DOC);
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(decision).contains(
                "Status: Completed",
                "Current Deployment Baseline",
                "Current Gaps",
                "Dockerfile Strategy",
                "CI / Quality Gate Strategy",
                "Default Offline Boundary",
                "TASK_COMPLETE");
        assertThat(roadmap).contains("Status: Completed", "default offline validation", "TASK_COMPLETE");
        assertThat(completedPlan).contains("Status: Completed", "TASK_COMPLETE");
    }

    @Test
    void currentDeploymentBaselineIsDocumented() throws IOException {
        String decision = projectText(DECISION_DOC);

        assertThat(decision).contains(
                "docker-compose.yml",
                "docker-compose-rag.yml",
                ".env.rag.example",
                "application-prod.example.yml",
                "application-mysql.yml",
                "application-rag-postgres.yml",
                "Actuator health",
                "OpenAPI docs",
                "默认测试离线");
    }

    @Test
    void productionHardeningCapabilitiesRemainFutureWork() throws IOException {
        String decision = projectText(DECISION_DOC);

        assertThat(decision).contains(
                "Dockerfile is not implemented",
                "CI/CD is not implemented",
                "Kubernetes / Helm is not implemented",
                "secret manager is not implemented",
                "production deployment is not completed",
                "live PGvector validation is not completed",
                "JdbcPolicyVectorRepository is not implemented",
                "production auth/RBAC is not completed",
                "production monitoring is not completed");
    }

    @Test
    void roadmapContainsDeploymentHardeningChecklists() throws IOException {
        String roadmap = projectText(ROADMAP_DOC);

        assertThat(roadmap).contains(
                "Dockerfile checklist",
                "CI quality gate checklist",
                "profile matrix checklist",
                "secret management checklist",
                "database migration checklist",
                "PGvector deployment checklist",
                "readiness/liveness checklist",
                "observability checklist",
                "security/auth checklist",
                "release/rollback checklist");
    }

    @Test
    void stageSixIsLinkedFromReviewDocs() throws IOException {
        String readme = projectText("README.md");
        String productionConfig = projectText("docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md");
        String remediation = projectText("docs/quality/PROJECT_REMEDIATION_PLAN.md");
        String activePlan = projectText("version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");
        String releaseSummary = projectText("version-updates/V4_RELEASE_SUMMARY.md");

        assertThat(readme).contains(ROADMAP_DOC);
        assertThat(productionConfig).contains(DECISION_DOC, ROADMAP_DOC);
        assertThat(remediation).contains("阶段 6：已完成", "阶段 0-6 current correction scope completed");
        assertThat(activePlan).contains("状态：阶段 0-6 已完成", "current correction scope completed");
        assertThat(quality).contains("Project Review Correction Stage 6 (completed)", "deployment hardening roadmap");
        assertThat(validation).contains(
                "Deployment Hardening Decision Validation",
                "DeploymentHardeningRoadmapDocsTest");
        assertThat(releaseSummary).contains("系统性补丁阶段 0-6 current correction scope completed");
    }

    @Test
    void defaultOfflineAndRuntimeNonChangeBoundariesAreDocumented() throws IOException {
        String docs = combinedStageSixDocs();

        assertThat(docs).contains(
                "default offline validation",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network",
                "secret manager",
                "CI runner",
                "Kubernetes / Helm",
                "Prometheus",
                "Grafana",
                "OpenTelemetry collector",
                "不修改 `src/main/java`",
                "不新增 Dockerfile",
                "不新增 CI/CD");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrDeploymentOverclaims() throws IOException {
        for (String path : STAGE_SIX_DOCS) {
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
                "dockerfile completed",
                "ci/cd completed",
                "kubernetes completed",
                "helm completed",
                "production deployment completed",
                "live pgvector validation completed",
                "production auth completed",
                "production monitoring completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedStageSixDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : STAGE_SIX_DOCS) {
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
