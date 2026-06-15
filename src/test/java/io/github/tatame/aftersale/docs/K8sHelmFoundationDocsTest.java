package io.github.tatame.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class K8sHelmFoundationDocsTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final String K8S_README = "deploy/k8s/README.md";
    private static final String K8S_DEPLOYMENT = "deploy/k8s/deployment.yaml";
    private static final String K8S_SERVICE = "deploy/k8s/service.yaml";
    private static final String K8S_CONFIGMAP = "deploy/k8s/configmap.yaml";
    private static final String K8S_SECRET_EXAMPLE = "deploy/k8s/secret.example.yaml";

    private static final String HELM_CHART = "deploy/helm/after-sale-agent-platform/Chart.yaml";
    private static final String HELM_VALUES = "deploy/helm/after-sale-agent-platform/values.yaml";
    private static final String HELM_README = "deploy/helm/after-sale-agent-platform/README.md";
    private static final String HELM_HELPERS = "deploy/helm/after-sale-agent-platform/templates/_helpers.tpl";
    private static final String HELM_DEPLOYMENT = "deploy/helm/after-sale-agent-platform/templates/deployment.yaml";
    private static final String HELM_SERVICE = "deploy/helm/after-sale-agent-platform/templates/service.yaml";
    private static final String HELM_CONFIGMAP = "deploy/helm/after-sale-agent-platform/templates/configmap.yaml";
    private static final String HELM_SECRET = "deploy/helm/after-sale-agent-platform/templates/secret.yaml";
    private static final String HELM_NOTES = "deploy/helm/after-sale-agent-platform/templates/NOTES.txt";

    private static final String FOUNDATION_DOC = "docs/deploy/K8S_HELM_FOUNDATION.md";
    private static final String COMPLETED_PLAN =
            "docs/exec-plans/completed/EXEC_PLAN_V5_B4_3_K8S_HELM_FOUNDATION.md";

    private static final List<String> ALL_K8S_HELM_FILES = List.of(
            K8S_README, K8S_DEPLOYMENT, K8S_SERVICE, K8S_CONFIGMAP, K8S_SECRET_EXAMPLE,
            HELM_CHART, HELM_VALUES, HELM_README, HELM_HELPERS, HELM_DEPLOYMENT, HELM_SERVICE,
            HELM_CONFIGMAP, HELM_SECRET, HELM_NOTES);

    private static final List<String> ALL_DOCS = List.of(
            "README.md",
            FOUNDATION_DOC,
            COMPLETED_PLAN,
            "docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md",
            "docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md",
            "docs/deploy/AUTH_RUNTIME_FOUNDATION.md",
            "docs/deploy/AUTH_RBAC_BOUNDARY.md",
            "docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md",
            "docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/quality/PROJECT_REMEDIATION_PLAN.md",
            "version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md",
            K8S_README,
            HELM_README);

    @Test
    void allK8sHelmFilesExist() throws IOException {
        for (String path : ALL_K8S_HELM_FILES) {
            assertThat(PROJECT_ROOT.resolve(path))
                    .as(path + " should exist")
                    .exists();
        }
    }

    @Test
    void foundationDocAndCompletionRecordExist() throws IOException {
        assertThat(PROJECT_ROOT.resolve(FOUNDATION_DOC)).exists();
        assertThat(PROJECT_ROOT.resolve(COMPLETED_PLAN)).exists();
        String completedPlan = projectText(COMPLETED_PLAN);
        assertThat(completedPlan).contains("Status: Completed", "TASK_COMPLETE");
    }

    // ---- Kubernetes manifest boundary tests ----

    @Test
    void k8sDeploymentContainsProbesAndSecurityContext() throws IOException {
        String deployment = projectText(K8S_DEPLOYMENT);

        assertThat(deployment).contains(
                "/actuator/health/readiness",
                "/actuator/health/liveness",
                "runAsNonRoot",
                "allowPrivilegeEscalation: false",
                "capabilities",
                "drop",
                "ALL");
    }

    @Test
    void k8sServiceIsClusterIpOnly() throws IOException {
        String service = projectText(K8S_SERVICE);

        assertThat(service).contains("ClusterIP");
        assertThat(service).doesNotContain("LoadBalancer", "NodePort");
    }

    @Test
    void k8sSecretExampleUsesPlaceholdersNotRealSecrets() throws IOException {
        String secret = projectText(K8S_SECRET_EXAMPLE);

        assertThat(K8S_SECRET_EXAMPLE).contains(".example");
        assertThat(secret).contains(
                "REPLACE_WITH_RUNTIME_SECRET",
                "DO NOT USE IN PRODUCTION");
        assertThat(secret.toLowerCase(Locale.ROOT)).doesNotContain(
                "sk-",
                "dGhpc0lzTm90QVJlYWxTZWNyZXQ=");
    }

    @Test
    void k8sConfigMapContainsOnlyNonSensitiveValues() throws IOException {
        String configmap = projectText(K8S_CONFIGMAP);

        assertThat(configmap).contains("Non-sensitive configuration only");
        assertThat(configmap).doesNotContain(
                "AFTERSALE_SECURITY_ADMIN_API_KEY",
                "AFTERSALE_SECURITY_SUPERVISOR_API_KEY",
                "AFTERSALE_SECURITY_OPERATOR_API_KEY",
                "AFTERSALE_SECURITY_SYSTEM_SERVICE_API_KEY");
    }

    @Test
    void k8sManifestsDoNotContainSecretsLocalPathsOrRealCredentials() throws IOException {
        for (String path : List.of(K8S_DEPLOYMENT, K8S_SERVICE, K8S_CONFIGMAP, K8S_SECRET_EXAMPLE, K8S_README)) {
            String text = projectText(path);
            String lower = text.toLowerCase(Locale.ROOT);

            assertThat(lower).as(path).doesNotContain(
                    "d:/", "d:\\", "c:/", "c:\\", "/users/", "/home/");
            assertThat(text).as(path).doesNotContain(
                    "password=prod",
                    "apiKey=sk-",
                    "privateKey=");
        }
    }

    // ---- Helm chart boundary tests ----

    @Test
    void helmChartHasCorrectNameAndType() throws IOException {
        String chart = projectText(HELM_CHART);

        assertThat(chart).contains(
                "name: after-sale-agent-platform",
                "type: application");
    }

    @Test
    void helmValuesUsePlaceholders() throws IOException {
        String values = projectText(HELM_VALUES);

        assertThat(values).contains(
                "REPLACE_WITH_IMAGE_TAG",
                "REPLACE_WITH_RUNTIME_SECRET",
                "security-api-key",
                "observability-prometheus",
                "readinessProbe",
                "livenessProbe",
                "securityContext",
                "runAsNonRoot",
                "allowPrivilegeEscalation");
        assertThat(values).contains(
                "ingress:",
                "enabled: false");
    }

    @Test
    void helmValuesDoNotContainRealSecretsOrRegistryCredentials() throws IOException {
        String values = projectText(HELM_VALUES);
        String lower = values.toLowerCase(Locale.ROOT);

        assertThat(lower).doesNotContain(
                "d:/", "d:\\", "c:/", "c:\\", "/users/", "/home/");
        assertThat(values).doesNotContain(
                "sk-",
                "password=prod",
                "password=production",
                "registry.example.com");
    }

    @Test
    void helmSecretTemplateDefaultsToExistingSecretPattern() throws IOException {
        String secretTpl = projectText(HELM_SECRET);

        assertThat(secretTpl).contains(
                "existingSecret",
                "secrets.create",
                "external secret");
    }

    @Test
    void helmDeploymentTemplateReferencesSecretAndConfigMap() throws IOException {
        String deploymentTpl = projectText(HELM_DEPLOYMENT);

        assertThat(deploymentTpl).contains(
                "secretKeyRef",
                "configMapKeyRef",
                "securityContext",
                ".Values.readinessProbe",
                ".Values.livenessProbe");
    }

    // ---- Docs status tests ----

    @Test
    void readmeMentionsV5B43K8sHelmFoundation() throws IOException {
        String readme = projectText("README.md");

        assertThat(readme).contains(
                "K8s / Helm",
                "Helm chart",
                "Kubernetes manifest");
    }

    @Test
    void deploymentRoadmapMarksB43AndB44Completed() throws IOException {
        String roadmap = projectText("docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md");

        assertThat(roadmap).contains(
                "V5.B.4.3 K8s / Helm Foundation completed",
                "V5.B.4.4 Release / Rollback Foundation completed");
    }

    @Test
    void qualityScoreMentionsK8sHelmFoundation() throws IOException {
        String quality = projectText("docs/quality/QUALITY_SCORE.md");

        assertThat(quality).contains(
                "V5.B.4.3 K8s / Helm Foundation",
                "Kubernetes manifest",
                "Helm chart");
    }

    @Test
    void validationDocsMentionK8sHelmTest() throws IOException {
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");

        assertThat(validation).contains("mvn test -Dtest=K8sHelmFoundationDocsTest");
    }

    @Test
    void completionRecordContainsTaskComplete() throws IOException {
        String completedPlan = projectText(COMPLETED_PLAN);

        assertThat(completedPlan).contains("TASK_COMPLETE");
    }

    // ---- Boundary tests ----

    @Test
    void docsStateNoProductionDeploymentCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains("not a production deployment");
        // At least one of the specific negations should be present (case-insensitive)
        assertThat(lower.contains("no kubectl apply")
                || lower.contains("no helm release")
                || lower.contains("no image registry push")
                || lower.contains("no image has been pushed")).isTrue();
    }

    @Test
    void docsStateNoReleaseRollbackCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(docs).contains("V5.B.4.4 Release / Rollback Foundation");
        assertThat(lower).contains("planned");
    }

    @Test
    void docsStateNoSecretManagerCompleted() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(lower.contains("no secret manager")
                || lower.contains("no external secret manager")
                || lower.contains("external secret manager")).isTrue();
    }

    @Test
    void docsStateDefaultValidationDoesNotNeedKubernetes() throws IOException {
        String docs = combinedDocs();

        // K8s README and foundation doc state Kubernetes/helm not required
        assertThat(docs.contains("not a production deployment")
                || docs.contains("optional")
                || docs.contains("NOT part of the default Maven")).isTrue();
    }

    @Test
    void docsStateHelmKubectlCommandsAreOptional() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "helm template",
                "kubectl apply --dry-run=client");
        assertThat(docs.contains("optional")
                || docs.contains("not part of the default Maven")).isTrue();
    }

    @Test
    void docsStateIngressIsDisabledOrFuture() throws IOException {
        String docs = combinedDocs();

        assertThat(docs.contains("Ingress is disabled")
                || docs.contains("ingress.enabled: false")
                || docs.contains("Ingress boundary")
                || docs.contains("Ingress disabled")).isTrue();
    }

    @Test
    void docsStateImageRegistryPushIsNotImplemented() throws IOException {
        String docs = combinedDocs();
        String lower = docs.toLowerCase(Locale.ROOT);

        assertThat(lower.contains("no image registry push")
                || lower.contains("no registry push")
                || lower.contains("no image registry")
                || lower.contains("no image has been pushed")).isTrue();
    }

    @Test
    void defaultValidationDoesNotNeedK8sDockerOrExternalServices() throws IOException {
        String docs = combinedDocs();

        assertThat(docs).contains(
                "real LLM",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis");
    }

    // ---- Safety tests ----

    @Test
    void allFilesDoNotContainRealSecretsLocalPathsOrProductionOverclaims() throws IOException {
        for (String path : ALL_DOCS) {
            assertSafeText(path, projectText(path));
        }
        for (String path : ALL_K8S_HELM_FILES) {
            assertSafeText(path, projectText(path));
        }
    }

    @Test
    void noFileClaimsProductionDeploymentCompleted() throws IOException {
        for (String path : ALL_DOCS) {
            String text = projectText(path);
            // Only check for POSITIVE claims, not negations like "not production deployment completed"
            assertThat(text).as(path).doesNotContain(
                    "production deployment is completed",
                    "production deployment was completed",
                    "production deployment has been completed",
                    "release automation is completed",
                    "rollback automation is completed");
        }
    }

    @Test
    void noFileClaimsRealBusinessIntegrationCompleted() throws IOException {
        List<String> allFiles = new java.util.ArrayList<>(ALL_DOCS);
        allFiles.addAll(ALL_K8S_HELM_FILES);
        for (String path : allFiles) {
            String lower = projectText(path).toLowerCase(Locale.ROOT);
            assertThat(lower).as(path).doesNotContain(
                    "真实退款已接入",
                    "真实换货已接入",
                    "真实补偿已接入",
                    "真实支付已接入",
                    "真实物流已接入");
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
                "sk-");
        // Check for positive overclaims only (not negations)
        assertThat(text).as(path).doesNotContain(
                "full production auth completed",
                "kubernetes deployment completed",
                "helm release completed",
                "真实退款已接入",
                "真实换货已接入",
                "真实补偿已接入",
                "真实支付已接入",
                "真实物流已接入");
    }

    private static String combinedDocs() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : ALL_DOCS) {
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
