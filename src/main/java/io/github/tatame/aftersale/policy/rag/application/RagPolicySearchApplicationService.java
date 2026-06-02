package io.github.tatame.aftersale.policy.rag.application;

import io.github.tatame.aftersale.policy.application.PolicyApplicationService;
import io.github.tatame.aftersale.policy.domain.PolicySearchQuery;
import io.github.tatame.aftersale.policy.domain.PolicySearchResult;
import io.github.tatame.aftersale.policy.rag.domain.PolicyVectorRepository;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchQuery;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchResult;
import io.github.tatame.aftersale.policy.rag.search.KeywordPolicyEvidenceMapper;
import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidenceMergeOptions;
import io.github.tatame.aftersale.policy.rag.search.RagPolicyEvidenceMergeService;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchQuery;
import io.github.tatame.aftersale.policy.rag.search.RagPolicySearchResult;
import io.github.tatame.aftersale.policy.rag.search.RetrievalMode;
import io.github.tatame.aftersale.policy.rag.search.VectorPolicyEvidenceMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Runtime RAG policy search application boundary used by the search tool.
 *
 * <p>It dispatches keyword, vector, and hybrid retrieval through project-owned abstractions. It does not expose
 * repositories or embedding clients to Agent, Handler, or Skill code.
 */
@Service
public class RagPolicySearchApplicationService {

    public static final String DEFAULT_EMBEDDING_MODEL = "fake-policy-embedding";

    private static final String VECTOR_UNAVAILABLE_MESSAGE =
            "Vector policy search is unavailable because embedding or vector repository is not configured.";

    private final PolicyApplicationService policyApplicationService;
    private final List<EmbeddingClient> embeddingClients;
    private final List<PolicyVectorRepository> vectorRepositories;
    private final KeywordPolicyEvidenceMapper keywordMapper = new KeywordPolicyEvidenceMapper();
    private final VectorPolicyEvidenceMapper vectorMapper = new VectorPolicyEvidenceMapper();
    private final RagPolicyEvidenceMergeService mergeService = new RagPolicyEvidenceMergeService();

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring application service stores injected collaborators and provider handles.")
    public RagPolicySearchApplicationService(
            PolicyApplicationService policyApplicationService,
            List<EmbeddingClient> embeddingClients,
            List<PolicyVectorRepository> vectorRepositories) {
        this.policyApplicationService = policyApplicationService;
        this.embeddingClients = List.copyOf(embeddingClients);
        this.vectorRepositories = List.copyOf(vectorRepositories);
    }

    public RagPolicySearchResult search(RagPolicySearchQuery query) {
        return switch (query.retrievalMode()) {
            case KEYWORD -> searchKeyword(query);
            case VECTOR -> searchVector(query);
            case HYBRID -> searchHybrid(query);
        };
    }

    private RagPolicySearchResult searchKeyword(RagPolicySearchQuery query) {
        PolicySearchResult keywordResult = policyApplicationService.search(new PolicySearchQuery(
                query.query(),
                Math.min(query.topK(), 10)));
        return keywordMapper.toRagResult(keywordResult);
    }

    private RagPolicySearchResult searchVector(RagPolicySearchQuery query) {
        EmbeddingClient embeddingClient = embeddingClients.stream().findFirst().orElse(null);
        PolicyVectorRepository vectorRepository = vectorRepositories.stream().findFirst().orElse(null);
        if (embeddingClient == null || vectorRepository == null) {
            return emptyVectorResult(query, VECTOR_UNAVAILABLE_MESSAGE, true);
        }
        try {
            String embeddingModel = embeddingModel(query);
            EmbeddingResponse embedding = embeddingClient.embed(new EmbeddingRequest(embeddingModel, query.query()));
            VectorSearchResult vectorResult = vectorRepository.search(new VectorSearchQuery(
                    query.query(),
                    embedding.vector(),
                    query.topK(),
                    query.minScore(),
                    query.category(),
                    query.productType(),
                    query.effectiveAt(),
                    embedding.model()));
            return vectorMapper.toRagResult(query.query(), vectorResult);
        } catch (RuntimeException exception) {
            return emptyVectorResult(
                    query,
                    "Vector policy search failed: " + sanitizeFailure(exception),
                    true);
        }
    }

    private RagPolicySearchResult searchHybrid(RagPolicySearchQuery query) {
        RagPolicySearchResult keywordResult = searchKeyword(query);
        RagPolicySearchResult vectorResult = searchVector(query);
        return mergeService.merge(keywordResult, vectorResult, mergeOptions(query));
    }

    private static RagPolicySearchResult emptyVectorResult(
            RagPolicySearchQuery query,
            String message,
            boolean fallbackUsed) {
        return new RagPolicySearchResult(
                query.query(),
                RetrievalMode.VECTOR,
                List.of(),
                message,
                fallbackUsed,
                0,
                0);
    }

    private static RagPolicyEvidenceMergeOptions mergeOptions(RagPolicySearchQuery query) {
        RagPolicyEvidenceMergeOptions defaults = RagPolicyEvidenceMergeOptions.defaults();
        return new RagPolicyEvidenceMergeOptions(
                query.topK(),
                query.minScore() == null ? defaults.minScore() : query.minScore(),
                defaults.keywordWeight(),
                defaults.vectorWeight(),
                defaults.preferKeywordWhenTie(),
                defaults.dedupByChunkId(),
                defaults.dedupByPolicyId(),
                defaults.dedupBySnippet(),
                defaults.includeKeywordOnly(),
                defaults.includeVectorOnly());
    }

    private static String embeddingModel(RagPolicySearchQuery query) {
        return query.embeddingModel() == null ? DEFAULT_EMBEDDING_MODEL : query.embeddingModel();
    }

    private static String sanitizeFailure(RuntimeException exception) {
        String type = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return type;
        }
        String sanitized = message
                .replaceAll("sk-[A-Za-z0-9_-]+", "sk-***")
                .replaceAll("Bearer\\s+[A-Za-z0-9._~+/=-]+", "Bearer ***")
                .replaceAll("(?i)api[_-]?key\\s*[:=]\\s*[^\\s,;]+", "apiKey=***")
                .replaceAll("(?i)password\\s*[:=]\\s*[^\\s,;]+", "password=***")
                .replaceAll("(?i)token\\s*[:=]\\s*[^\\s,;]+", "token=***")
                .replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "[local-path]");
        if (sanitized.length() > 180) {
            sanitized = sanitized.substring(0, 180);
        }
        return type + ": " + sanitized;
    }
}
