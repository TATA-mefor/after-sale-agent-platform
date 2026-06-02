package io.github.tatame.aftersale.policy.rag.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RagEvaluationDatasetLoaderTest {

    private static final Path DATASET_PATH = Path.of("docs/evaluation/rag_policy_cases.jsonl");

    private final RagEvaluationDatasetLoader loader = new RagEvaluationDatasetLoader(new ObjectMapper());

    @Test
    void loadsVersionedRagPolicyCases() {
        List<RagEvaluationCase> cases = loader.load(DATASET_PATH);

        assertThat(cases).hasSize(15);
        assertThat(cases)
                .extracting(RagEvaluationCase::retrievalMode)
                .contains(RetrievalMode.KEYWORD, RetrievalMode.VECTOR, RetrievalMode.HYBRID);
        assertThat(cases.get(0).expected().requiredRetrievalMode()).isEqualTo(RetrievalMode.HYBRID);
    }

    @Test
    void caseIdsAreUniqueAndExpectedFieldsAreComplete() {
        List<RagEvaluationCase> cases = loader.load(DATASET_PATH);
        Set<String> caseIds = new HashSet<>();

        assertThat(cases).allSatisfy(evaluationCase -> {
            assertThat(caseIds.add(evaluationCase.caseId())).isTrue();
            assertThat(evaluationCase.expected().forbiddenSnippetContains()).isNotEmpty();
            assertThat(evaluationCase.expected().maxEvidenceCount())
                    .isGreaterThanOrEqualTo(evaluationCase.expected().minEvidenceCount());
        });
    }

    @Test
    void malformedLineFailsWithClearLineNumber(@TempDir Path tempDir) throws IOException {
        Path invalidDataset = tempDir.resolve("invalid-rag.jsonl");
        Files.writeString(invalidDataset, "{\"caseId\":\"BROKEN\",\"query\":\"missing expected\"}");

        assertThatThrownBy(() -> loader.load(invalidDataset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid RAG evaluation case at line 1")
                .hasMessageContaining("expected");
    }

    @Test
    void datasetDoesNotContainSecretsLocalPathsOrRawDatasetPointers() throws IOException {
        String content = Files.readString(DATASET_PATH);

        assertThat(content)
                .doesNotContain("api_key")
                .doesNotContain("OPENAI_API_KEY")
                .doesNotContain("password")
                .doesNotContain("token")
                .doesNotContain("D:\\")
                .doesNotContain("C:\\")
                .doesNotContain("data/raw");
    }
}
