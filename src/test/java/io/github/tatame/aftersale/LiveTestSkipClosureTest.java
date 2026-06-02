package io.github.tatame.aftersale;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LiveTestSkipClosureTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void llmLiveSmokeTestRequiresExplicitLiveFlagAndCredentials() throws IOException {
        String source = source("src/test/java/io/github/tatame/aftersale/LlmPlannerLiveSmokeTest.java");

        assertThat(source).contains(
                "@Tag(\"live\")",
                "@EnabledIfSystemProperty(named = \"live.llm\", matches = \"true\")",
                "assumeTrue(liveProviderCredentialsPresent",
                "OPENAI_API_KEY",
                "DASHSCOPE_API_KEY");
    }

    @Test
    void springAiLiveSmokeTestsRequireExplicitLiveFlagsAndCredentialAssumptions() throws IOException {
        String chat = source("src/test/java/io/github/tatame/aftersale/SpringAiLlmClientLiveSmokeTest.java");
        String embedding = source("src/test/java/io/github/tatame/aftersale/SpringAiEmbeddingClientLiveSmokeTest.java");

        assertThat(chat).contains(
                "@Tag(\"live\")",
                "@EnabledIfSystemProperty(named = \"live.spring-ai\", matches = \"true\")",
                "@EnabledIfSystemProperty(named = \"live.llm\", matches = \"true\")",
                "assumeTrue",
                "SPRING_AI_ENABLED",
                "SPRING_AI_CHAT_ENABLED",
                "SPRING_AI_OPENAI_API_KEY");
        assertThat(embedding).contains(
                "@Tag(\"live\")",
                "@EnabledIfSystemProperty(named = \"live.spring-ai\", matches = \"true\")",
                "@EnabledIfSystemProperty(named = \"live.embedding\", matches = \"true\")",
                "assumeTrue",
                "SPRING_AI_ENABLED",
                "SPRING_AI_EMBEDDING_ENABLED",
                "SPRING_AI_OPENAI_API_KEY");
    }

    @Test
    void realAgentValidationRequiresExplicitLlmMysqlFlagsAndEnvironmentGates() throws IOException {
        String source = source("src/test/java/io/github/tatame/aftersale/RealAgentValidationLiveTest.java");

        assertThat(source).contains(
                "@Tag(\"live\")",
                "@ActiveProfiles(\"mysql\")",
                "@EnabledIfSystemProperty(named = \"live.llm\", matches = \"true\")",
                "@EnabledIfSystemProperty(named = \"live.mysql\", matches = \"true\")",
                "@EnabledIf(\"llmProviderCredentialsAvailable\")",
                "@EnabledIfEnvironmentVariable(named = \"AFTERSALE_MYSQL_URL\", matches = \".+\")",
                "@EnabledIfEnvironmentVariable(named = \"AFTERSALE_MYSQL_USERNAME\", matches = \".+\")",
                "@EnabledIfEnvironmentVariable(named = \"AFTERSALE_MYSQL_PASSWORD\", matches = \".+\")",
                "OPENAI_API_KEY",
                "DASHSCOPE_API_KEY",
                "SPRING_AI_OPENAI_API_KEY");
    }

    @Test
    void pgVectorSmokeTestRequiresExplicitRagFlagAndEnvironmentGates() throws IOException {
        String source = source(
                "src/test/java/io/github/tatame/aftersale/policy/rag/infrastructure/pgvector/"
                        + "JdbcPolicyVectorRepositorySmokeTest.java");

        assertThat(source).contains(
                "@Tag(\"live\")",
                "@EnabledIfSystemProperty(named = \"live.rag\", matches = \"true\")",
                "assumeTrue",
                "AFTERSALE_PGVECTOR_URL",
                "AFTERSALE_PGVECTOR_USERNAME",
                "AFTERSALE_PGVECTOR_PASSWORD",
                "AFTERSALE_PGVECTOR_SCHEMA");
    }

    private static String source(String path) throws IOException {
        Path file = PROJECT_ROOT.resolve(path);
        assertThat(Files.exists(file)).as(path + " should exist").isTrue();
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
