package io.github.tatame.aftersale.policy.rag.domain;

import java.util.List;
import java.util.Optional;

public interface PolicyVectorRepository {

    PolicyDocument saveDocument(PolicyDocument document);

    PolicyChunk saveChunk(PolicyChunk chunk);

    PolicyEmbedding saveEmbedding(PolicyEmbedding embedding);

    Optional<PolicyDocument> findDocumentById(String documentId);

    Optional<PolicyChunk> findChunkById(String chunkId);

    List<PolicyChunk> findChunksByDocumentId(String documentId);

    Optional<PolicyEmbedding> findEmbeddingByChunkIdAndModel(String chunkId, String embeddingModel);

    VectorSearchResult search(VectorSearchQuery query);
}
