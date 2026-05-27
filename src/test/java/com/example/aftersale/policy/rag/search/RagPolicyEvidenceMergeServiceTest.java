package com.example.aftersale.policy.rag.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aftersale.policy.domain.PolicySearchQuery;
import com.example.aftersale.policy.domain.PolicySearchResult;
import com.example.aftersale.policy.domain.PolicySnippet;
import com.example.aftersale.policy.rag.domain.VectorSearchMatch;
import com.example.aftersale.policy.rag.domain.VectorSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagPolicyEvidenceMergeServiceTest {

    private final RagPolicyEvidenceMergeService service = new RagPolicyEvidenceMergeService();

    @Test
    void mergeOptionsValidateBoundsAndDefaults() {
        RagPolicyEvidenceMergeOptions defaults = RagPolicyEvidenceMergeOptions.defaults();

        assertThat(defaults.topK()).isEqualTo(5);
        assertThat(defaults.keywordWeight()).isEqualTo(0.45d);
        assertThat(defaults.vectorWeight()).isEqualTo(0.55d);

        assertThatThrownBy(() -> options(0, 0.0d, 0.45d, 0.55d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");
        assertThatThrownBy(() -> options(21, 0.0d, 0.45d, 0.55d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");
        assertThatThrownBy(() -> options(5, -0.1d, 0.45d, 0.55d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minScore");
        assertThatThrownBy(() -> options(5, 0.0d, -0.1d, 0.55d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keywordWeight");
        assertThatThrownBy(() -> options(5, 0.0d, 0.0d, 0.0d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not both be zero");
    }

    @Test
    void mergesScoreDeterministicallyAndSortsDescending() {
        RagPolicyEvidence keyword = keywordEvidence("policy-1", "Shared policy evidence.", 0.8d);
        RagPolicyEvidence vector = vectorEvidence("doc-1", "chunk-1", "Shared policy evidence.", 0.6d);
        RagPolicyEvidence vectorOnly = vectorEvidence("doc-2", "chunk-2", "Higher vector-only evidence.", 0.9d);
        RagPolicyEvidenceMergeOptions options = options(5, 0.0d, 0.25d, 0.75d);

        RagPolicySearchResult result = service.merge(
                keywordResult("quality return", keyword),
                vectorResult("quality return", vector, vectorOnly),
                options);

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        assertThat(result.evidences()).hasSize(2);
        assertThat(result.evidences().get(0).snippet()).isEqualTo("Higher vector-only evidence.");
        assertThat(result.evidences().get(0).score()).isEqualTo(0.9d);
        RagPolicyEvidence merged = result.evidences().get(1);
        assertThat(merged.score()).isEqualTo(0.65d);
        assertThat(merged.keywordScore()).isEqualTo(0.8d);
        assertThat(merged.vectorScore()).isEqualTo(0.6d);
        assertThat(merged.retrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        assertThat(merged.source()).isEqualTo(RagPolicyEvidenceSource.MERGED_HYBRID);
    }

    @Test
    void scoreIsClampedAndTieOrderingPrefersKeywordWhenConfigured() {
        RagPolicyEvidence keyword = keywordEvidence("policy-a", "Keyword tied evidence.", 1.0d);
        RagPolicyEvidence vector = vectorEvidence("doc-b", "chunk-b", "Vector tied evidence.", 1.0d);

        RagPolicySearchResult result = service.merge(
                keywordResult("query", keyword),
                vectorResult("query", vector),
                options(5, 0.0d, 1.0d, 1.0d));

        assertThat(result.evidences()).extracting(RagPolicyEvidence::score).containsExactly(1.0d, 1.0d);
        assertThat(result.evidences().get(0).keywordScore()).isNotNull();
    }

    @Test
    void dedupsByChunkIdPolicyIdAndNormalizedSnippet() {
        RagPolicyEvidence keywordByPolicy = keywordEvidence("policy-1", "Keyword policy evidence.", 0.7d);
        RagPolicyEvidence vectorByPolicy = vectorEvidenceWithPolicy(
                "doc-1",
                "chunk-1",
                "policy-1",
                "Vector policy evidence.",
                0.9d);
        RagPolicyEvidence keywordBySnippet = keywordEvidence("policy-2", " Same text evidence ", 0.5d);
        RagPolicyEvidence vectorBySnippet = vectorEvidence("doc-2", "chunk-2", "same   text EVIDENCE", 0.8d);
        RagPolicyEvidence nonDuplicate = vectorEvidence("doc-3", "chunk-3", "Different evidence.", 0.6d);

        RagPolicySearchResult result = service.merge(
                keywordResult("query", keywordByPolicy, keywordBySnippet),
                vectorResult("query", vectorByPolicy, vectorBySnippet, nonDuplicate),
                RagPolicyEvidenceMergeOptions.defaults());

        assertThat(result.evidences()).hasSize(3);
        assertThat(result.evidences()).allSatisfy(evidence -> {
            assertThat(evidence.retrievalMode()).isEqualTo(RetrievalMode.HYBRID);
            assertThat(evidence.source()).isEqualTo(RagPolicyEvidenceSource.MERGED_HYBRID);
        });
        RagPolicyEvidence policyMerged = result.evidences().stream()
                .filter(evidence -> "policy-1".equals(evidence.policyId()))
                .findFirst()
                .orElseThrow();
        assertThat(policyMerged.documentId()).isEqualTo("doc-1");
        assertThat(policyMerged.chunkId()).isEqualTo("chunk-1");
        assertThat(policyMerged.keywordScore()).isEqualTo(0.7d);
        assertThat(policyMerged.vectorScore()).isEqualTo(0.9d);
    }

    @Test
    void topKAndMinScoreApplyAfterMergeAndSorting() {
        RagPolicyEvidence keywordLow = keywordEvidence("policy-low", "Low keyword evidence.", 0.2d);
        RagPolicyEvidence vectorHigh = vectorEvidence("doc-high", "chunk-high", "High vector evidence.", 0.9d);
        RagPolicyEvidence vectorMid = vectorEvidence("doc-mid", "chunk-mid", "Mid vector evidence.", 0.7d);
        RagPolicyEvidenceMergeOptions options = options(1, 0.6d, 0.45d, 0.55d);

        RagPolicySearchResult result = service.merge(
                keywordResult("query", keywordLow),
                vectorResult("query", vectorHigh, vectorMid),
                options);

        assertThat(result.evidences()).hasSize(1);
        assertThat(result.evidences().get(0).chunkId()).isEqualTo("chunk-high");
    }

    @Test
    void includeFlagsFilterOnlySingleSidedEvidence() {
        RagPolicyEvidence keyword = keywordEvidence("policy-1", "Shared evidence.", 0.9d);
        RagPolicyEvidence vectorDuplicate = vectorEvidence("doc-1", "chunk-1", "Shared evidence.", 0.8d);
        RagPolicyEvidence vectorOnly = vectorEvidence("doc-2", "chunk-2", "Vector only evidence.", 0.95d);
        RagPolicyEvidenceMergeOptions options = new RagPolicyEvidenceMergeOptions(
                5,
                0.0d,
                0.45d,
                0.55d,
                true,
                true,
                true,
                true,
                false,
                false);

        RagPolicySearchResult result = service.merge(
                keywordResult("query", keyword),
                vectorResult("query", vectorDuplicate, vectorOnly),
                options);

        assertThat(result.evidences()).hasSize(1);
        assertThat(result.evidences().get(0).keywordScore()).isEqualTo(0.9d);
        assertThat(result.evidences().get(0).vectorScore()).isEqualTo(0.8d);
    }

    @Test
    void fallbackMessagesCoverKeywordOnlyVectorOnlyBothEmptyAndNullInput() {
        RagPolicySearchResult keywordOnly = service.merge(
                keywordResult("query", keywordEvidence("policy-1", "Keyword only evidence.", 0.8d)),
                vectorResult("query"),
                RagPolicyEvidenceMergeOptions.defaults());
        assertThat(keywordOnly.fallbackUsed()).isTrue();
        assertThat(keywordOnly.message()).contains("vector side was empty");

        RagPolicySearchResult vectorOnly = service.merge(
                keywordResult("query"),
                vectorResult("query", vectorEvidence("doc-1", "chunk-1", "Vector only evidence.", 0.8d)),
                RagPolicyEvidenceMergeOptions.defaults());
        assertThat(vectorOnly.fallbackUsed()).isTrue();
        assertThat(vectorOnly.message()).contains("keyword side was empty");

        RagPolicySearchResult bothEmpty = service.merge(null, null, null);
        assertThat(bothEmpty.fallbackUsed()).isTrue();
        assertThat(bothEmpty.evidences()).isEmpty();
        assertThat(bothEmpty.message()).contains("No keyword or vector policy evidence");
    }

    @Test
    void rejectsUnexpectedInputModesAndKeepsMessagesSafe() {
        RagPolicySearchResult wrongMode = new RagPolicySearchResult(
                "query",
                RetrievalMode.HYBRID,
                List.of(),
                "already hybrid",
                false,
                0,
                0);

        assertThatThrownBy(() -> service.merge(
                wrongMode,
                vectorResult("query"),
                RagPolicyEvidenceMergeOptions.defaults()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected KEYWORD");

        RagPolicySearchResult result = service.merge(keywordResult("query"), vectorResult("query"),
                RagPolicyEvidenceMergeOptions.defaults());
        String lower = result.message().toLowerCase();
        assertThat(lower).doesNotContain("api_key");
        assertThat(lower).doesNotContain("password");
        assertThat(lower).doesNotContain("token");
        assertThat(lower).doesNotContain("d:\\");
        assertThat(lower).doesNotContain("prompt");
    }

    @Test
    void mapperOutputsCanBeMergedWithoutRepositoryOrEmbeddingCalls() {
        PolicySearchQuery query = PolicySearchQuery.of("quality return");
        PolicySearchResult keyword = PolicySearchResult.matched(query, List.of(new PolicySnippet(
                "policy-1",
                "RETURN",
                "electronics",
                "Quality return evidence.",
                "keyword matched")));
        VectorSearchResult vector = VectorSearchResult.matched(List.of(new VectorSearchMatch(
                "doc-1",
                "chunk-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "Quality return evidence.",
                0.82d,
                0.18d,
                "fake-embedding",
                "{\"chunkIndex\":0}")));

        RagPolicySearchResult result = service.merge(
                new KeywordPolicyEvidenceMapper().toRagResult(keyword),
                new VectorPolicyEvidenceMapper().toRagResult("quality return", vector),
                RagPolicyEvidenceMergeOptions.defaults());

        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.HYBRID);
        assertThat(result.evidences()).hasSize(1);
        assertThat(result.evidences().get(0).policyId()).isEqualTo("policy-1");
        assertThat(result.evidences().get(0).chunkId()).isEqualTo("chunk-1");
    }

    @Test
    void mergedEvidenceStillRejectsCompletedBusinessActionClaimsAndUnsafeMetadata() {
        assertThatThrownBy(() -> keywordEvidence("系统显示已退款。", 0.8d))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completed business actions");
        assertThatThrownBy(() -> new RagPolicyEvidence(
                "keyword:unsafe",
                null,
                null,
                "policy-unsafe",
                null,
                "RETURN",
                "electronics",
                "Safe evidence text.",
                0.8d,
                0.8d,
                null,
                RetrievalMode.KEYWORD,
                RagPolicyEvidenceSource.KEYWORD_POLICY,
                null,
                null,
                "{\"api_key\":\"secret\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadataJson");
    }

    private static RagPolicyEvidenceMergeOptions options(
            int topK,
            double minScore,
            double keywordWeight,
            double vectorWeight) {
        return new RagPolicyEvidenceMergeOptions(
                topK,
                minScore,
                keywordWeight,
                vectorWeight,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    private static RagPolicySearchResult keywordResult(String query, RagPolicyEvidence... evidences) {
        return new RagPolicySearchResult(
                query,
                RetrievalMode.KEYWORD,
                List.of(evidences),
                evidences.length == 0 ? "No keyword evidence." : "Keyword evidence.",
                false,
                evidences.length,
                0);
    }

    private static RagPolicySearchResult vectorResult(String query, RagPolicyEvidence... evidences) {
        return new RagPolicySearchResult(
                query,
                RetrievalMode.VECTOR,
                List.of(evidences),
                evidences.length == 0 ? "No vector evidence." : "Vector evidence.",
                evidences.length == 0,
                0,
                evidences.length);
    }

    private static RagPolicyEvidence keywordEvidence(String policyId, String snippet, double score) {
        return new RagPolicyEvidence(
                "keyword:" + policyId,
                null,
                null,
                policyId,
                null,
                "RETURN",
                "electronics",
                snippet,
                score,
                score,
                null,
                RetrievalMode.KEYWORD,
                RagPolicyEvidenceSource.KEYWORD_POLICY,
                null,
                null,
                "{}");
    }

    private static RagPolicyEvidence keywordEvidence(String snippet, double score) {
        return keywordEvidence("policy-action", snippet, score);
    }

    private static RagPolicyEvidence vectorEvidence(String documentId, String chunkId, String snippet, double score) {
        return vectorEvidenceWithPolicy(documentId, chunkId, null, snippet, score);
    }

    private static RagPolicyEvidence vectorEvidenceWithPolicy(
            String documentId,
            String chunkId,
            String policyId,
            String snippet,
            double score) {
        return new RagPolicyEvidence(
                "vector:" + chunkId,
                documentId,
                chunkId,
                policyId,
                "Policy Document",
                "RETURN",
                "electronics",
                snippet,
                score,
                null,
                score,
                RetrievalMode.VECTOR,
                RagPolicyEvidenceSource.VECTOR_CHUNK,
                null,
                null,
                "{}");
    }
}
