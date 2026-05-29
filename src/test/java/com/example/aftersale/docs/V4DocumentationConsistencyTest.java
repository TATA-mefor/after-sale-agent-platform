package com.example.aftersale.docs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class V4DocumentationConsistencyTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    private static final List<String> V4_COMPLETED_PLANS = List.of(
            "docs/exec-plans/completed/EXEC_PLAN_V4_PRE_FLIGHT_FIXES.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_TOOL_SKILL_LAYER_FOUNDATION.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_SPRING_AI_ADAPTER.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_PGVECTOR_PROFILE_BOUNDARY.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_VECTOR_SCHEMA_REPOSITORY_CONTRACT.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_FAKE_VECTOR_STORE_OFFLINE_TESTS.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_PGVECTOR_COMPOSE_DOCS.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_DOMAIN_MODEL.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_CHUNKING_CHECKSUM_DEDUP.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_EMBEDDING_PIPELINE_FAKE_PROVIDER.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_RAG_SEARCH_CONTRACT.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVIDENCE_MERGE_SERVICE.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_SEARCH_AFTERSALE_POLICY_HYBRID_RUNTIME.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_RAG_TRACE_WORKSPACE_EVIDENCE.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_RAG_EVALUATION_CASES_METRICS.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_RAG_DEMO_SCRIPT.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_RAG_ACTUATOR_HEALTH.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_OPENAPI_API_DOCS.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_DOCUMENTATION_CONSISTENCY_AUDIT.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_ARCHITECTURE_OFFLINE_VALIDATION_CLOSURE.md",
            "docs/exec-plans/completed/EXEC_PLAN_V4_INTERVIEW_DEMO_README_POLISH.md");

    private static final List<String> SELECTED_V4_DOCS = List.of(
            "README.md",
            "EXEC_PLAN_V4.md",
            "docs/exec-plans/active/EXEC_PLAN_V4_RAG_SPRING_AI.md",
            "docs/quality/QUALITY_SCORE.md",
            "docs/agent/TOOL_CONTRACTS.md",
            "docs/agent/SKILL_CONTRACTS.md",
            "docs/agent/RAG_POLICY_RETRIEVAL_CONTRACT.md",
            "docs/agent/RISK_POLICY.md",
            "docs/agent/LLM_PLANNER_CONTRACT.md",
            "docs/decisions/DECISION_V4_TOOL_SKILL_LAYER.md",
            "docs/decisions/DECISION_V4_SPRING_AI_ADAPTER.md",
            "docs/decisions/DECISION_V4_RAG_VECTOR_STORE.md",
            "docs/decisions/DECISION_V4_SPRING_BOOT_COMPLETENESS.md",
            "docs/quality/VALIDATION_COMMANDS.md",
            "docs/evaluation/EVALUATION.md",
            "docs/demo/V4_RAG_DEMO_SCRIPT.md",
            "docs/demo/V4_POLICY_INGESTION_PIPELINE.md",
            "docs/demo/V4_PGVECTOR_LOCAL_SETUP.md",
            "docs/api/OPENAPI.md");

    @Test
    void v4CompletedPlansExistAndContainCompletionSignals() throws IOException {
        for (String path : V4_COMPLETED_PLANS) {
            String text = projectText(path);

            assertThat(text).as(path).contains("TASK_COMPLETE");
            assertThat(text).as(path).containsIgnoringCase("completed");
            assertSecretSafe(path, text);
        }
    }

    @Test
    void v4RoadmapStatusIsConsistentAcrossPrimaryDocs() throws IOException {
        String readme = projectText("README.md");
        String execPlan = projectText("EXEC_PLAN_V4.md");
        String activePlan = projectText("docs/exec-plans/active/EXEC_PLAN_V4_RAG_SPRING_AI.md");
        String quality = projectText("docs/quality/QUALITY_SCORE.md");
        String combined = readme + "\n" + execPlan + "\n" + activePlan + "\n" + quality;

        assertThat(combined).contains(
                "V4.0",
                "V4.1",
                "V4.2",
                "V4.3.1",
                "V4.3.2",
                "V4.3.3",
                "V4.3.4",
                "V4.4.1",
                "V4.4.2",
                "V4.4.3",
                "V4.4.4",
                "V4.5.1",
                "V4.5.2",
                "V4.5.3",
                "V4.5.4",
                "V4.6.1",
                "V4.6.2",
                "V4.6.3",
                "V4.6.4");
        assertThat(combined).contains("V4.7.1", "completed");
        assertThat(combined).contains("V4.7.2", "completed");
        assertThat(combined).contains("V4.7.3", "completed");
        assertThat(combined).contains("V4.7.4", "planned");

        assertThat(execPlan).contains("V4.7 Documentation / Architecture / Final Closure", "Status: active");
        assertThat(activePlan).contains("V4.7 Documentation / Architecture / Final Closure", "Status: active");
        assertThat(quality).contains("V4.7.1 Documentation Consistency / Secret Safety Audit (completed)");
        assertThat(quality).contains("V4.7.2 Architecture Boundary / Offline Validation Closure (completed)");
        assertThat(quality).contains("V4.7.3 Interview Demo / README Polish (completed)");
    }

    @Test
    void selectedV4DocsStateCurrentSafetyBoundaries() throws IOException {
        String docs = selectedDocsText();

        assertThat(docs).contains("RAG evidence");
        assertThat(docs).contains("evidence-only");
        assertThat(docs).contains("LOW-risk read-only");
        assertThat(docs).contains("search_aftersale_policy");
        assertThat(docs).contains("ToolRegistry");
        assertThat(docs).contains("Skill");
        assertThat(docs).contains("does not replace ToolRegistry");
        assertThat(docs).contains("offline pipeline");
        assertThat(docs).contains("not an Agent runtime tool");
        assertThat(docs).contains("outside the Agent runtime");
        assertThat(docs).contains("opt-in");
        assertThat(docs).contains("real embedding provider");
        assertThat(docs).contains("PGvector");
        assertThat(docs).contains("default tests");
        assertThat(docs).contains("external network");
    }

    @Test
    void selectedV4DocsDoNotContainSecretsLocalPathsOrOverclaims() throws IOException {
        for (String path : SELECTED_V4_DOCS) {
            assertSecretSafe(path, projectText(path));
        }
        for (String path : V4_COMPLETED_PLANS) {
            assertSecretSafe(path, projectText(path));
        }
    }

    @Test
    void v471CompletionRecordDefinesNonRuntimeAuditBoundary() throws IOException {
        String completed = projectText(
                "docs/exec-plans/completed/EXEC_PLAN_V4_DOCUMENTATION_CONSISTENCY_AUDIT.md");

        assertThat(completed).contains(
                "Status: Completed",
                "Documentation Consistency Boundary",
                "Completion Records Boundary",
                "Secret / Path Safety Boundary",
                "Runtime Non-change Boundary",
                "Default Offline Test Boundary",
                "does not modify runtime business code",
                "V4.7.2",
                "V4.7.3",
                "V4.7.4",
                "TASK_COMPLETE");
    }

    @Test
    void v472CompletionRecordDefinesArchitectureAndOfflineValidationBoundary() throws IOException {
        String completed = projectText(
                "docs/exec-plans/completed/EXEC_PLAN_V4_ARCHITECTURE_OFFLINE_VALIDATION_CLOSURE.md");

        assertThat(completed).contains(
                "Status: Completed",
                "Architecture Boundary Closure",
                "Default Offline Validation Boundary",
                "Live Test Skip Boundary",
                "Validation Command Documentation",
                "Runtime Non-change Boundary",
                "does not add runtime business behavior",
                "V4.7.3",
                "V4.7.4",
                "TASK_COMPLETE");
    }

    @Test
    void validationCommandsDocumentDefaultOfflineAndLiveOptInBoundaries() throws IOException {
        String validation = projectText("docs/quality/VALIDATION_COMMANDS.md");

        assertThat(validation).contains(
                "mvn test",
                "mvn checkstyle:check",
                "mvn spotbugs:check",
                "mvn test -Dtest=ArchitectureTest",
                "real LLM",
                "API Key",
                "PostgreSQL",
                "PGvector",
                "Docker",
                "MySQL",
                "Redis",
                "external network",
                "-Dlive.llm=true",
                "-Dlive.spring-ai=true",
                "-Dlive.embedding=true",
                "-Dlive.mysql=true",
                "If any default command requires one of those dependencies, treat it as a regression");
    }

    private static String selectedDocsText() throws IOException {
        StringBuilder builder = new StringBuilder();
        for (String path : SELECTED_V4_DOCS) {
            builder.append(projectText(path)).append('\n');
        }
        return builder.toString();
    }

    private static String projectText(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private static void assertSecretSafe(String path, String text) {
        String lower = text.toLowerCase();

        assertThat(lower).as(path).doesNotContain(
                "sk-",
                "api_key=",
                "openai_api_key=",
                "dashscope_api_key=",
                "spring_ai_openai_api_key=",
                "password=prod",
                "password=production",
                "token=",
                "secret=",
                "jdbc:postgresql://prod",
                "jdbc:postgresql://production",
                "d:/",
                "d:\\",
                "c:/",
                "c:\\",
                "/users/",
                "data/raw/",
                "已退款成功",
                "已换货完成",
                "已补偿到账");
    }
}
