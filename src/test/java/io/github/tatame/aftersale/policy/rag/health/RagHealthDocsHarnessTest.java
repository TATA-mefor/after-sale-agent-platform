package io.github.tatame.aftersale.policy.rag.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RagHealthDocsHarnessTest {

    @Test
    void docsMentionRagHealthBoundariesAndNoSecrets() throws IOException {
        String readme = fileText("README.md");
        String v4Roadmap = fileText("version-updates/V4_ROADMAP.md");
        String quality = fileText("docs/quality/QUALITY_SCORE.md");
        String activePlan = fileText("version-updates/EXEC_PLAN_V4_RAG_SPRING_AI.md");
        String completed = fileText("version-updates/EXEC_PLAN_V4_RAG_ACTUATOR_HEALTH.md");
        String decision = fileText("docs/decisions/DECISION_V4_SPRING_BOOT_COMPLETENESS.md");

        assertThat(v4Roadmap).contains("V4 RAG health indicators");
        assertThat(quality).contains("V4.6.3");
        assertThat(activePlan).contains("V4.6.3", "Completed");
        assertThat(decision).contains("offline readiness");
        assertThat(completed).contains(
                "Status: Completed",
                "Health Indicator Boundary",
                "offline readiness",
                "does not connect to PostgreSQL",
                "does not call real embedding providers",
                "does not expose secrets",
                "TASK_COMPLETE");

        String combined = readme + quality + activePlan + completed + decision + v4Roadmap;
        assertThat(combined).doesNotContain(
                "sk-",
                "database password:",
                "api key:",
                "D:/",
                "D:\\",
                "/Users/");
    }

    private static String fileText(String path) throws IOException {
        return Files.readString(Path.of("").toAbsolutePath().resolve(path), StandardCharsets.UTF_8);
    }
}
