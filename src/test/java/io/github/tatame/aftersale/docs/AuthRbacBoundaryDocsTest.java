package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class AuthRbacBoundaryDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String DECISION_DOC = "docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md";

    private static final String DEPLOY_DOC = "docs/deploy/AUTH_RBAC_BOUNDARY.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_1_AUTH_RBAC_BOUNDARY_DECISION.md";

    private static final List<String> V5_B4_1_DOCS = List.of(
            "README.md",
            DECISION_DOC,
            DEPLOY_DOC,
            COMPLETED_PLAN,
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            "docs/api/OPENAPI.md");

    @Test
    void authRbacDecisionDocsExistAndMarkCompleted() throws IOException {
        String decision = projectText(DECISION_DOC);
        String deploy = projectText(DEPLOY_DOC);
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(decision).contains(
                "Status: Completed",
                "Current API Surface",
                "Current Auth Gap",
                "RBAC Role Model",
                "API Access Matrix",
                "Actuator Access Boundary",
                "OpenAPI / Swagger UI Boundary",
                "Approval Boundary",
                "ToolRegistry / High-risk Action Boundary",
                "RAG Evidence-only Boundary",
                "K8s Exposure Precondition",
                "Release / Rollback Security Precondition",
                "TASK_COMPLETE");
        assertThat(deploy).contains(
                "Status: Completed",
                "full production IAM",
                "Current Auth Gap",
                "Role Model",
                "API Access Matrix Boundary",
                "ToolRegistry direct access",
                "K8s Exposure Boundary",
                "Release / Rollback Boundary",
                "TASK_COMPLETE");
        assertThat(completedPlan).contains(
                "Status: Completed",
                "Current Auth Gap",
                "Role Model Boundary",
                "API Access Matrix Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Boundary",
                "TASK_COMPLETE");
    }

    @Test
    void roleModelAndApiAccessMatrixAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "CUSTOMER",
                "AGENT_OPERATOR",
                "SUPERVISOR",
                "ADMIN",
                "SYSTEM_SERVICE",
                "Ticket create",
                "AgentRun create",
                "Approval approve / reject",
                "Trace read",
                "ExecutionTree read",
                "OpenAPI / Swagger UI",
                "Prometheus opt-in endpoint",
                "Admin ingestion future API",
                "ToolRegistry direct access");
    }

    @Test
    void readmeAndStatusDocsLinkAuthBoundaryAndKeepFutureRuntimePlanned() throws IOException {
        String readme = projectText("README.md");
        String docs = combinedDocs();

        assertThat(readme).contains(
                "[Auth / RBAC Boundary](" + DEPLOY_DOC + ")",
                "API Key",
                "security-api-key");
        assertThat(docs).contains(
                "V5.B.4.1 Production Auth / RBAC Boundary Decision completed",
                "V5.B.4.2 Spring Security / API Key Auth Foundation completed",
                "V5.B.4.3 K8s / Helm Foundation completed",
                "V5.B.4.4 Release / Rollback Foundation planned",
                "full production auth / RBAC runtime",
                "full production IAM remains");
    }

    @Test
    void securityBoundariesAreExplicitWithoutRuntimeAuthOverclaim() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains(
                "ToolRegistry is not a public API",
                "ToolRegistry direct access is never public",
                "High-risk actions require Approval",
                "`search_aftersale_policy` remains LOW-risk read-only",
                "RAG evidence is policy evidence only",
                "Evidence score is a retrieval score",
                "Swagger UI is local or internal API documentation",
                "K8s / Helm / Ingress exposure remains planned",
                "Release / rollback hardening remains planned");
        assertThat(lower).doesNotContain(
                "full production auth completed",
                "production iam completed",
                "oauth2 completed",
                "oidc completed",
                "jwt completed",
                "kubernetes completed",
                "helm completed",
                "production deployment completed");
    }

    @Test
    void defaultOfflineAndRuntimeNonChangeBoundariesAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "documentation-only",
                "does not modify `src/main/java`",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "real embedding provider",
                "Spring AI `VectorStore`",
                "external network");
    }

    @Test
    void validationCommandsAreDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "mvn test -Dtest=AuthRbacBoundaryDocsTest",
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : V5_B4_1_DOCS) {
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
                "production auth completed",
                "spring security completed",
                "kubernetes completed",
                "helm completed",
                "production deployment completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : V5_B4_1_DOCS) {
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
