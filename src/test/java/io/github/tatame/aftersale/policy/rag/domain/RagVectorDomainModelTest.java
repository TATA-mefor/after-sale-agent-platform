package io.github.tatame.aftersale.policy.rag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagVectorDomainModelTest {

    private static final Instant NOW = Instant.parse("2026-05-22T00:00:00Z");

    @Test
    void policyDocumentValidatesRequiredFieldsAndEffectiveDates() {
        PolicyDocument document = sampleDocument();

        assertThat(document.documentId()).isEqualTo("doc-1");
        assertThat(document.sourceType()).isEqualTo(PolicyDocumentSourceType.MARKDOWN);
        assertThat(document.effectiveTo()).isNull();

        assertThatThrownBy(() -> new PolicyDocument(
                "doc-2",
                "Return Policy",
                "RETURN",
                "electronics",
                "v1",
                PolicyDocumentSourceType.MARKDOWN,
                null,
                "checksum-2",
                LocalDate.parse("2026-05-02"),
                LocalDate.parse("2026-05-01"),
                NOW,
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveTo");
    }

    @Test
    void policyChunkValidatesIndexContentAndMetadata() {
        PolicyChunk chunk = sampleChunk();

        assertThat(chunk.chunkIndex()).isZero();
        assertThat(chunk.metadataJson()).isEqualTo("{}");

        assertThatThrownBy(() -> new PolicyChunk(
                "chunk-2",
                "doc-1",
                -1,
                "quality return",
                10,
                "{}",
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkIndex");
    }

    @Test
    void policyEmbeddingValidatesVectorDimensionAndFiniteValues() {
        PolicyEmbedding embedding = sampleEmbedding();

        assertThat(embedding.embeddingDimension()).isEqualTo(3);
        assertThat(embedding.vector()).containsExactly(0.1d, 0.2d, 0.3d);

        assertThatThrownBy(() -> new PolicyEmbedding(
                "embedding-2",
                "chunk-1",
                "fake-embedding",
                2,
                List.of(0.1d, 0.2d, 0.3d),
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("embeddingDimension");

        assertThatThrownBy(() -> new PolicyEmbedding(
                "embedding-3",
                "chunk-1",
                "fake-embedding",
                1,
                List.of(Double.NaN),
                NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    void vectorSearchQueryBoundsTopKAndMinScore() {
        VectorSearchQuery query = new VectorSearchQuery(
                "quality issue",
                List.of(0.1d, 0.2d),
                5,
                0.65d,
                "RETURN",
                "electronics",
                LocalDate.parse("2026-05-22"),
                "fake-embedding");

        assertThat(query.topK()).isEqualTo(5);
        assertThat(query.queryVector()).containsExactly(0.1d, 0.2d);

        assertThatThrownBy(() -> new VectorSearchQuery(null, List.of(0.1d), 0, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");

        assertThatThrownBy(() -> new VectorSearchQuery(null, List.of(0.1d), 1, 1.1d, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minScore");
    }

    @Test
    void vectorSearchMatchRepresentsEvidenceOnly() {
        VectorSearchMatch match = new VectorSearchMatch(
                "doc-1",
                "chunk-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "Quality issues can be used as policy evidence.",
                0.82d,
                0.18d,
                "fake-embedding",
                "{\"chunkIndex\":0}");
        VectorSearchResult result = VectorSearchResult.matched(List.of(match));

        assertThat(result.hasMatches()).isTrue();
        assertThat(result.message()).contains("policy evidence");
        assertThat(match.snippet()).contains("policy evidence");
        assertThat(match.snippet()).doesNotContain("refunded");
        assertThat(match.snippet()).doesNotContain("exchanged");
        assertThat(match.snippet()).doesNotContain("completed");
    }

    static PolicyDocument sampleDocument() {
        return new PolicyDocument(
                "doc-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "v1",
                PolicyDocumentSourceType.MARKDOWN,
                "policy://return",
                "checksum-1",
                LocalDate.parse("2026-01-01"),
                null,
                NOW,
                NOW);
    }

    static PolicyChunk sampleChunk() {
        return new PolicyChunk(
                "chunk-1",
                "doc-1",
                0,
                "Quality issue return evidence.",
                12,
                null,
                NOW);
    }

    static PolicyEmbedding sampleEmbedding() {
        return new PolicyEmbedding(
                "embedding-1",
                "chunk-1",
                "fake-embedding",
                3,
                List.of(0.1d, 0.2d, 0.3d),
                NOW);
    }
}
