package com.example.aftersale.policy.rag.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicyVectorRepositoryContractTest {

    @Test
    void repositoryContractCanBeImplementedWithoutPostgreSql() {
        TestPolicyVectorRepository repository = new TestPolicyVectorRepository();
        PolicyDocument document = RagVectorDomainModelTest.sampleDocument();
        PolicyChunk chunk = RagVectorDomainModelTest.sampleChunk();
        PolicyEmbedding embedding = RagVectorDomainModelTest.sampleEmbedding();

        repository.saveDocument(document);
        repository.saveChunk(chunk);
        repository.saveEmbedding(embedding);

        assertThat(repository.findDocumentById("doc-1")).contains(document);
        assertThat(repository.findChunkById("chunk-1")).contains(chunk);
        assertThat(repository.findChunksByDocumentId("doc-1")).containsExactly(chunk);
        assertThat(repository.findEmbeddingByChunkIdAndModel("chunk-1", "fake-embedding")).contains(embedding);

        VectorSearchResult result = repository.search(new VectorSearchQuery(
                "quality issue",
                List.of(0.1d, 0.2d, 0.3d),
                3,
                null,
                "RETURN",
                "electronics",
                null,
                "fake-embedding"));

        assertThat(result.hasMatches()).isTrue();
        assertThat(result.matches()).singleElement()
                .extracting(VectorSearchMatch::chunkId)
                .isEqualTo("chunk-1");
    }

    private static final class TestPolicyVectorRepository implements PolicyVectorRepository {

        private final Map<String, PolicyDocument> documents = new LinkedHashMap<>();
        private final Map<String, PolicyChunk> chunks = new LinkedHashMap<>();
        private final Map<String, PolicyEmbedding> embeddings = new LinkedHashMap<>();

        @Override
        public PolicyDocument saveDocument(PolicyDocument document) {
            documents.put(document.documentId(), document);
            return document;
        }

        @Override
        public PolicyChunk saveChunk(PolicyChunk chunk) {
            chunks.put(chunk.chunkId(), chunk);
            return chunk;
        }

        @Override
        public PolicyEmbedding saveEmbedding(PolicyEmbedding embedding) {
            embeddings.put(embedding.chunkId() + ":" + embedding.embeddingModel(), embedding);
            return embedding;
        }

        @Override
        public Optional<PolicyDocument> findDocumentById(String documentId) {
            return Optional.ofNullable(documents.get(documentId));
        }

        @Override
        public Optional<PolicyChunk> findChunkById(String chunkId) {
            return Optional.ofNullable(chunks.get(chunkId));
        }

        @Override
        public List<PolicyChunk> findChunksByDocumentId(String documentId) {
            return chunks.values().stream()
                    .filter(chunk -> chunk.documentId().equals(documentId))
                    .toList();
        }

        @Override
        public Optional<PolicyEmbedding> findEmbeddingByChunkIdAndModel(String chunkId, String embeddingModel) {
            return Optional.ofNullable(embeddings.get(chunkId + ":" + embeddingModel));
        }

        @Override
        public VectorSearchResult search(VectorSearchQuery query) {
            return chunks.values().stream()
                    .findFirst()
                    .map(chunk -> VectorSearchResult.matched(List.of(matchFor(chunk))))
                    .orElseGet(() -> VectorSearchResult.empty("No policy evidence matches.", false));
        }

        private VectorSearchMatch matchFor(PolicyChunk chunk) {
            PolicyDocument document = documents.get(chunk.documentId());
            return new VectorSearchMatch(
                    document.documentId(),
                    chunk.chunkId(),
                    document.title(),
                    document.category(),
                    document.productType(),
                    chunk.content(),
                    0.9d,
                    0.1d,
                    "fake-embedding",
                    chunk.metadataJson());
        }
    }
}
