package io.github.tatame.aftersale.policy.rag.infrastructure.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.policy.rag.domain.PolicyChunk;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocument;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import io.github.tatame.aftersale.policy.rag.domain.PolicyEmbedding;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchQuery;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryPolicyVectorRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-05-22T00:00:00Z");
    private static final String MODEL = "fake-embedding";

    @Test
    void savesAndFindsDocumentsChunksAndEmbeddings() {
        PolicyVectorRepository repository = seededRepository();

        assertThat(repository.findDocumentById("doc-return")).isPresent();
        assertThat(repository.findChunkById("chunk-return")).isPresent();
        assertThat(repository.findChunksByDocumentId("doc-return"))
                .extracting(PolicyChunk::chunkId)
                .containsExactly("chunk-return");
        assertThat(repository.findEmbeddingByChunkIdAndModel("chunk-return", MODEL)).isPresent();
    }

    @Test
    void searchReturnsHighestSimilarityFirstAndRespectsTopK() {
        PolicyVectorRepository repository = seededRepository();

        VectorSearchResult result = repository.search(query(List.of(1.0d, 0.0d, 0.0d), 2));

        assertThat(result.hasMatches()).isTrue();
        assertThat(result.matches()).hasSize(2);
        assertThat(result.matches()).extracting(match -> match.chunkId())
                .containsExactly("chunk-return", "chunk-special");
        assertThat(result.matches().get(0).score()).isGreaterThan(result.matches().get(1).score());
    }

    @Test
    void searchRespectsMinScoreAndFilters() {
        PolicyVectorRepository repository = seededRepository();

        VectorSearchResult categoryResult = repository.search(new VectorSearchQuery(
                "exchange issue",
                List.of(0.0d, 1.0d, 0.0d),
                5,
                0.99d,
                "EXCHANGE",
                "electronics",
                LocalDate.parse("2026-05-22"),
                MODEL));

        assertThat(categoryResult.matches()).singleElement()
                .extracting(match -> match.chunkId())
                .isEqualTo("chunk-exchange");

        VectorSearchResult productTypeResult = repository.search(new VectorSearchQuery(
                "special goods",
                List.of(0.7d, 0.2d, 0.0d),
                5,
                null,
                "RETURN",
                "special-goods",
                LocalDate.parse("2026-05-22"),
                MODEL));

        assertThat(productTypeResult.matches()).singleElement()
                .extracting(match -> match.chunkId())
                .isEqualTo("chunk-special");
    }

    @Test
    void searchRespectsEffectiveAtAndEmbeddingModelFilters() {
        PolicyVectorRepository repository = seededRepository();

        VectorSearchResult expiredResult = repository.search(new VectorSearchQuery(
                "logistics issue",
                List.of(0.0d, 0.0d, 1.0d),
                5,
                null,
                "LOGISTICS",
                "electronics",
                LocalDate.parse("2024-12-31"),
                MODEL));

        assertThat(expiredResult.matches()).isEmpty();
        assertThat(expiredResult.message()).contains("No policy evidence matches");

        VectorSearchResult wrongModelResult = repository.search(new VectorSearchQuery(
                "return issue",
                List.of(1.0d, 0.0d, 0.0d),
                5,
                null,
                null,
                null,
                null,
                "other-model"));

        assertThat(wrongModelResult.matches()).isEmpty();
    }

    @Test
    void emptyResultDoesNotFabricateEvidenceOrBusinessActionCompletion() {
        PolicyVectorRepository repository = seededRepository();

        VectorSearchResult result = repository.search(new VectorSearchQuery(
                "coupon issue",
                List.of(1.0d, 0.0d, 0.0d),
                5,
                null,
                "COUPON",
                null,
                null,
                MODEL));

        assertThat(result.matches()).isEmpty();
        assertThat(result.message()).contains("No policy evidence matches");
        assertThat(result.message()).doesNotContain("refunded");
        assertThat(result.message()).doesNotContain("exchanged");
        assertThat(result.message()).doesNotContain("compensated");
    }

    @Test
    void dimensionMismatchFailsClearly() {
        PolicyVectorRepository repository = seededRepository();

        VectorSearchQuery query = new VectorSearchQuery(
                "return issue",
                List.of(1.0d, 0.0d),
                5,
                null,
                null,
                null,
                null,
                MODEL);

        assertThatThrownBy(() -> repository.search(query))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions must match");
    }

    @Test
    void duplicateDocumentsChunksAndEmbeddingsAreRejected() {
        InMemoryPolicyVectorRepository repository = new InMemoryPolicyVectorRepository();
        PolicyDocument document = document("doc-return", "Return Policy", "RETURN", "electronics", null);
        PolicyChunk chunk = chunk("chunk-return", "doc-return", 0, "Return policy evidence.");
        PolicyEmbedding embedding = embedding("embedding-return", "chunk-return", List.of(1.0d, 0.0d, 0.0d));

        repository.saveDocument(document);
        assertThatThrownBy(() -> repository.saveDocument(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate document");

        repository.saveChunk(chunk);
        assertThatThrownBy(() -> repository.saveChunk(chunk))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate chunk");

        repository.saveEmbedding(embedding);
        assertThatThrownBy(() -> repository.saveEmbedding(embedding))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate embedding");
    }

    @Test
    void saveChunkAndEmbeddingRequireExistingParents() {
        InMemoryPolicyVectorRepository repository = new InMemoryPolicyVectorRepository();

        assertThatThrownBy(() -> repository.saveChunk(chunk("chunk-return", "doc-return", 0, "Return policy.")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("document must be saved before chunk");

        repository.saveDocument(document("doc-return", "Return Policy", "RETURN", "electronics", null));
        assertThatThrownBy(() -> repository.saveEmbedding(
                embedding("embedding-return", "chunk-return", List.of(1.0d, 0.0d, 0.0d))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunk must be saved before embedding");
    }

    @Test
    void searchMatchContainsEvidenceFieldsOnly() {
        PolicyVectorRepository repository = seededRepository();

        VectorSearchResult result = repository.search(query(List.of(0.0d, 0.0d, 1.0d), 1));

        assertThat(result.matches()).singleElement().satisfies(match -> {
            assertThat(match.documentId()).isEqualTo("doc-logistics");
            assertThat(match.chunkId()).isEqualTo("chunk-logistics");
            assertThat(match.documentTitle()).isEqualTo("Logistics Issue Policy");
            assertThat(match.category()).isEqualTo("LOGISTICS");
            assertThat(match.productType()).isEqualTo("electronics");
            assertThat(match.snippet()).contains("Logistics delay policy evidence");
            assertThat(match.score()).isBetween(0.0d, 1.0d);
            assertThat(match.embeddingModel()).isEqualTo(MODEL);
        });
    }

    private static PolicyVectorRepository seededRepository() {
        InMemoryPolicyVectorRepository repository = new InMemoryPolicyVectorRepository();
        save(repository, "doc-return", "Return Policy", "RETURN", "electronics", null,
                "chunk-return", "Return quality issue policy evidence.", List.of(1.0d, 0.0d, 0.0d));
        save(repository, "doc-exchange", "Exchange Policy", "EXCHANGE", "electronics", null,
                "chunk-exchange", "Exchange recommendation policy evidence.", List.of(0.0d, 1.0d, 0.0d));
        save(repository, "doc-logistics", "Logistics Issue Policy", "LOGISTICS", "electronics", null,
                "chunk-logistics", "Logistics delay policy evidence.", List.of(0.0d, 0.0d, 1.0d));
        save(repository, "doc-special", "Special Goods Restriction", "RETURN", "special-goods", null,
                "chunk-special", "Special goods return restriction evidence.", List.of(0.7d, 0.2d, 0.0d));
        return repository;
    }

    private static void save(
            InMemoryPolicyVectorRepository repository,
            String documentId,
            String title,
            String category,
            String productType,
            LocalDate effectiveTo,
            String chunkId,
            String content,
            List<Double> vector) {
        repository.saveDocument(document(documentId, title, category, productType, effectiveTo));
        repository.saveChunk(chunk(chunkId, documentId, 0, content));
        repository.saveEmbedding(embedding("embedding-" + chunkId, chunkId, vector));
    }

    private static PolicyDocument document(
            String documentId,
            String title,
            String category,
            String productType,
            LocalDate effectiveTo) {
        return new PolicyDocument(
                documentId,
                title,
                category,
                productType,
                "v1",
                PolicyDocumentSourceType.MARKDOWN,
                "policy://" + documentId,
                "checksum-" + documentId,
                LocalDate.parse("2026-01-01"),
                effectiveTo,
                NOW,
                NOW);
    }

    private static PolicyChunk chunk(String chunkId, String documentId, int chunkIndex, String content) {
        return new PolicyChunk(chunkId, documentId, chunkIndex, content, 12, "{\"fixture\":true}", NOW);
    }

    private static PolicyEmbedding embedding(String embeddingId, String chunkId, List<Double> vector) {
        return new PolicyEmbedding(embeddingId, chunkId, MODEL, vector.size(), vector, NOW);
    }

    private static VectorSearchQuery query(List<Double> vector, int topK) {
        return new VectorSearchQuery("policy issue", vector, topK, null, null, null, null, MODEL);
    }
}
