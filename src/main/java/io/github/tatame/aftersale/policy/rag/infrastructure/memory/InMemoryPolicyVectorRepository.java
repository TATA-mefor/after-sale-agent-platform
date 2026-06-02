package io.github.tatame.aftersale.policy.rag.infrastructure.memory;

import io.github.tatame.aftersale.policy.rag.domain.CosineSimilarityCalculator;
import io.github.tatame.aftersale.policy.rag.domain.PolicyChunk;
import io.github.tatame.aftersale.policy.rag.domain.PolicyDocument;
import io.github.tatame.aftersale.policy.rag.domain.PolicyEmbedding;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchMatch;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchQuery;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchResult;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Offline vector repository for deterministic tests and local fake-provider wiring.
 */
public class InMemoryPolicyVectorRepository implements PolicyVectorRepository {

    private static final int SNIPPET_LIMIT = 240;

    private final CosineSimilarityCalculator similarityCalculator;
    private final Map<String, PolicyDocument> documents = new LinkedHashMap<>();
    private final Map<String, PolicyChunk> chunks = new LinkedHashMap<>();
    private final Map<String, PolicyEmbedding> embeddingsByChunkAndModel = new LinkedHashMap<>();

    public InMemoryPolicyVectorRepository(CosineSimilarityCalculator similarityCalculator) {
        this.similarityCalculator = Objects.requireNonNull(
                similarityCalculator, "similarityCalculator must not be null");
    }

    public InMemoryPolicyVectorRepository() {
        this(new CosineSimilarityCalculator());
    }

    @Override
    public PolicyDocument saveDocument(PolicyDocument document) {
        PolicyDocument normalized = Objects.requireNonNull(document, "document must not be null");
        rejectDuplicate(documents.containsKey(normalized.documentId()), "document", normalized.documentId());
        documents.put(normalized.documentId(), normalized);
        return normalized;
    }

    @Override
    public PolicyChunk saveChunk(PolicyChunk chunk) {
        PolicyChunk normalized = Objects.requireNonNull(chunk, "chunk must not be null");
        requireDocument(normalized.documentId());
        rejectDuplicate(chunks.containsKey(normalized.chunkId()), "chunk", normalized.chunkId());
        chunks.put(normalized.chunkId(), normalized);
        return normalized;
    }

    @Override
    public PolicyEmbedding saveEmbedding(PolicyEmbedding embedding) {
        PolicyEmbedding normalized = Objects.requireNonNull(embedding, "embedding must not be null");
        requireChunk(normalized.chunkId());
        String key = embeddingKey(normalized.chunkId(), normalized.embeddingModel());
        rejectDuplicate(embeddingsByChunkAndModel.containsKey(key), "embedding", key);
        embeddingsByChunkAndModel.put(key, normalized);
        return normalized;
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
                .sorted(Comparator.comparingInt(PolicyChunk::chunkIndex)
                        .thenComparing(PolicyChunk::chunkId))
                .toList();
    }

    @Override
    public Optional<PolicyEmbedding> findEmbeddingByChunkIdAndModel(String chunkId, String embeddingModel) {
        return Optional.ofNullable(embeddingsByChunkAndModel.get(embeddingKey(chunkId, embeddingModel)));
    }

    @Override
    public VectorSearchResult search(VectorSearchQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<VectorSearchMatch> matches = embeddingsByChunkAndModel.values().stream()
                .filter(embedding -> matchesEmbeddingModel(embedding, query))
                .map(embedding -> toScoredMatch(embedding, query))
                .flatMap(Optional::stream)
                .filter(match -> query.minScore() == null || match.score() >= query.minScore())
                .sorted(Comparator.comparingDouble(VectorSearchMatch::score)
                        .reversed()
                        .thenComparing(VectorSearchMatch::documentId)
                        .thenComparing(VectorSearchMatch::chunkId))
                .limit(query.topK())
                .toList();

        if (matches.isEmpty()) {
            return VectorSearchResult.empty("No policy evidence matches found in fake vector repository.", false);
        }
        return VectorSearchResult.matched(matches);
    }

    private Optional<VectorSearchMatch> toScoredMatch(PolicyEmbedding embedding, VectorSearchQuery query) {
        PolicyChunk chunk = chunks.get(embedding.chunkId());
        if (chunk == null) {
            return Optional.empty();
        }
        PolicyDocument document = documents.get(chunk.documentId());
        if (document == null || !matchesDocumentFilters(document, query)) {
            return Optional.empty();
        }
        double score = similarityCalculator.similarity(query.queryVector(), embedding.vector());
        return Optional.of(new VectorSearchMatch(
                document.documentId(),
                chunk.chunkId(),
                document.title(),
                document.category(),
                document.productType(),
                snippet(chunk.content()),
                score,
                1.0d - score,
                embedding.embeddingModel(),
                chunk.metadataJson()));
    }

    private static boolean matchesEmbeddingModel(PolicyEmbedding embedding, VectorSearchQuery query) {
        return query.embeddingModel() == null || embedding.embeddingModel().equals(query.embeddingModel());
    }

    private static boolean matchesDocumentFilters(PolicyDocument document, VectorSearchQuery query) {
        if (query.category() != null && !document.category().equals(query.category())) {
            return false;
        }
        if (query.productType() != null && !document.productType().equals(query.productType())) {
            return false;
        }
        if (query.effectiveAt() == null) {
            return true;
        }
        boolean startsOnOrBefore = !document.effectiveFrom().isAfter(query.effectiveAt());
        boolean endsOnOrAfter = document.effectiveTo() == null || !document.effectiveTo().isBefore(query.effectiveAt());
        return startsOnOrBefore && endsOnOrAfter;
    }

    private static String snippet(String content) {
        if (content.length() <= SNIPPET_LIMIT) {
            return content;
        }
        return content.substring(0, SNIPPET_LIMIT).trim();
    }

    private void requireDocument(String documentId) {
        if (!documents.containsKey(documentId)) {
            throw new IllegalArgumentException("document must be saved before chunk: " + documentId);
        }
    }

    private void requireChunk(String chunkId) {
        if (!chunks.containsKey(chunkId)) {
            throw new IllegalArgumentException("chunk must be saved before embedding: " + chunkId);
        }
    }

    private static void rejectDuplicate(boolean duplicate, String entityType, String entityId) {
        if (duplicate) {
            throw new IllegalArgumentException("duplicate " + entityType + " is not allowed: " + entityId);
        }
    }

    private static String embeddingKey(String chunkId, String embeddingModel) {
        return chunkId + "::" + embeddingModel;
    }
}
