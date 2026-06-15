package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ApiKeyAuthFoundationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String RUNTIME_DOC = "docs/deploy/AUTH_RUNTIME_FOUNDATION.md";

    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_2_SPRING_SECURITY_API_KEY_AUTH_FOUNDATION.md";

    private static final List<String> V5_B4_2_DOCS = List.of(
            "README.md",
            "docs/api/OPENAPI.md",
            "docs/deploy/AUTH_RBAC_BOUNDARY.md",
            RUNTIME_DOC,
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            COMPLETED_PLAN);

    @Test
    void securityProfileAndRuntimeDocsExist() throws IOException {
        assertThat(PROJECT_ROOT.resolve("src/main/resources/application-security-api-key.yml"))
                .exists();
        assertThat(PROJECT_ROOT.resolve(RUNTIME_DOC)).exists();
        assertThat(projectText(COMPLETED_PLAN)).contains("Status: Completed", "TASK_COMPLETE");
    }

    @Test
    void readmeAndStatusDocsLinkApiKeyAuthFoundation() throws IOException {
        String readme = projectText("README.md");
        String docs = combinedDocs();

        assertThat(readme).contains(
                "[Auth Runtime Foundation](" + RUNTIME_DOC + ")",
                "API Key",
                "security-api-key");
        assertThat(docs).contains(
                "V5.B.4.2 Spring Security / API Key Auth Foundation completed",
                "V5.B.4.3 K8s / Helm Foundation completed",
                "V5.B.4.4 Release / Rollback Foundation planned",
                "API Key Auth Foundation",
                "mvn test -Dtest=ApiKeyAuthBoundaryTest");
    }

    @Test
    void apiKeyRuntimeBoundaryIsDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Spring Security",
                "security-api-key",
                "X-API-Key",
                "AFTERSALE_SECURITY_ADMIN_API_KEY",
                "AFTERSALE_SECURITY_SUPERVISOR_API_KEY",
                "AFTERSALE_SECURITY_OPERATOR_API_KEY",
                "AFTERSALE_SECURITY_SYSTEM_SERVICE_API_KEY");
        assertThat(docs).contains("default profile", "permit-all", "offline", "opt-in");
    }

    @Test
    void endpointProtectionBoundaryIsDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Health probes are public",
                "Ticket and AgentRun APIs require a valid application role",
                "Approval approve / reject requires `ADMIN` or `SUPERVISOR`",
                "Trace and ExecutionTree read APIs allow `ADMIN`, `SUPERVISOR` and `AGENT_OPERATOR`",
                "OpenAPI / Swagger UI requires `ADMIN` or `SUPERVISOR`",
                "Prometheus, when exposed by opt-in profile, requires `ADMIN` or `SYSTEM_SERVICE`",
                "Sensitive actuator endpoints remain unexposed");
    }

    @Test
    void productionIamAndDeploymentLimitsRemainExplicit() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "Full production auth / IAM is not completed",
                "OAuth2 / OIDC is not implemented",
                "JWT issuer / JWKS is not implemented",
                "Session login and password login are not implemented",
                "User database and tenant isolation are not implemented",
                "Secret manager integration is not implemented",
                "Kubernetes / Helm and release / rollback automation remain planned");
    }

    @Test
    void toolRegistryApprovalRagAndOfflineBoundariesRemainDocumented() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "ToolRegistry remains internal",
                "High-risk actions remain Approval-gated",
                "`search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool",
                "RAG evidence is policy evidence only",
                "does not execute refund, exchange, compensation, payment or logistics actions",
                "Default validation does not need real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network");
    }

    @Test
    void docsDoNotContainSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : V5_B4_2_DOCS) {
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
                "aftersale_security_admin_api_key=",
                "aftersale_security_supervisor_api_key=",
                "aftersale_security_operator_api_key=",
                "aftersale_security_system_service_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "sk-",
                "full production auth completed",
                "production iam completed",
                "oauth2 completed",
                "oidc completed",
                "jwt completed",
                "kubernetes completed",
                "helm completed",
                "release automation completed",
                "rollback automation completed",
                "production deployment completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : V5_B4_2_DOCS) {
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
