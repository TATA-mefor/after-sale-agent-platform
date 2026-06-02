package io.github.tatame.aftersale.policy.rag.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.tatame.aftersale.policy.domain.PolicySearchQuery;
import io.github.tatame.aftersale.policy.domain.PolicySearchResult;
import io.github.tatame.aftersale.policy.domain.PolicySnippet;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchMatch;
import io.github.tatame.aftersale.policy.rag.domain.VectorSearchResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagPolicySearchContractTest {

    @Test
    void retrievalModeParsesKnownModesAndDefaultsToKeyword() {
        assertThat(RetrievalMode.parse("keyword")).isEqualTo(RetrievalMode.KEYWORD);
        assertThat(RetrievalMode.parse("VECTOR")).isEqualTo(RetrievalMode.VECTOR);
        assertThat(RetrievalMode.parse(" hybrid ")).isEqualTo(RetrievalMode.HYBRID);
        assertThat(RetrievalMode.parse(null)).isEqualTo(RetrievalMode.KEYWORD);
        assertThat(RetrievalMode.parse(" ")).isEqualTo(RetrievalMode.KEYWORD);

        assertThatThrownBy(() -> RetrievalMode.parse("semantic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown retrievalMode");
    }

    @Test
    void ragPolicySearchQueryValidatesInputAndDefaultsMode() {
        RagPolicySearchQuery query = new RagPolicySearchQuery(
                " quality return ",
                null,
                5,
                0.5d,
                " RETURN ",
                " electronics ",
                LocalDate.parse("2026-05-27"),
                " fake-model ",
                true,
                true);

        assertThat(query.query()).isEqualTo("quality return");
        assertThat(query.retrievalMode()).isEqualTo(RetrievalMode.KEYWORD);
        assertThat(query.category()).isEqualTo("RETURN");
        assertThat(query.productType()).isEqualTo("electronics");
        assertThat(query.embeddingModel()).isEqualTo("fake-model");

        assertThatThrownBy(() -> RagPolicySearchQuery.keyword(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
        assertThatThrownBy(() -> new RagPolicySearchQuery("q", RetrievalMode.KEYWORD, 0, null, null, null,
                null, null, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");
        assertThatThrownBy(() -> new RagPolicySearchQuery("q", RetrievalMode.KEYWORD, 21, null, null, null,
                null, null, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");
        assertThatThrownBy(() -> new RagPolicySearchQuery("q", RetrievalMode.KEYWORD, 1, 1.1d, null, null,
                null, null, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minScore");
    }

    @Test
    void ragPolicyEvidenceValidatesEvidenceOnlyAndSecretSafety() {
        RagPolicyEvidence evidence = keywordEvidence("Quality issues may qualify for return policy evidence.", "{}");

        assertThat(evidence.score()).isEqualTo(0.8d);
        assertThat(evidence.retrievalMode()).isEqualTo(RetrievalMode.KEYWORD);
        assertThat(evidence.source()).isEqualTo(RagPolicyEvidenceSource.KEYWORD_POLICY);

        assertThatThrownBy(() -> keywordEvidence("", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("snippet");
        assertThatThrownBy(() -> keywordEvidence("Quality issue evidence.", "{\"password\":\"secret\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadataJson");
        assertThatThrownBy(() -> keywordEvidence("系统显示已退款。", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completed business actions");
        assertThatThrownBy(() -> new RagPolicyEvidence("e-2", null, null, "p-1", null, "RETURN",
                "electronics", "Neutral evidence.", 1.2d, null, null, RetrievalMode.KEYWORD,
                RagPolicyEvidenceSource.KEYWORD_POLICY, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("score");
    }

    @Test
    void keywordMapperConvertsPolicySearchResultWithoutInventingVectorIds() {
        PolicySearchQuery query = PolicySearchQuery.of("return policy");
        PolicySnippet snippet = new PolicySnippet(
                "policy-1",
                "RETURN",
                "electronics",
                "Quality issues can use return evidence.",
                "keyword matched return");
        PolicySearchResult keywordResult = PolicySearchResult.matched(query, List.of(snippet));

        RagPolicySearchResult result = new KeywordPolicyEvidenceMapper().toRagResult(keywordResult);

        assertThat(result.query()).isEqualTo("return policy");
        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.KEYWORD);
        assertThat(result.totalKeywordMatches()).isEqualTo(1);
        assertThat(result.totalVectorMatches()).isZero();
        assertThat(result.evidences()).hasSize(1);
        RagPolicyEvidence evidence = result.evidences().get(0);
        assertThat(evidence.source()).isEqualTo(RagPolicyEvidenceSource.KEYWORD_POLICY);
        assertThat(evidence.policyId()).isEqualTo("policy-1");
        assertThat(evidence.documentId()).isNull();
        assertThat(evidence.chunkId()).isNull();
        assertThat(evidence.keywordScore()).isEqualTo(KeywordPolicyEvidenceMapper.KEYWORD_EVIDENCE_SCORE);

        RagPolicySearchResult empty = new KeywordPolicyEvidenceMapper().toRagResult(PolicySearchResult.empty(query));
        assertThat(empty.evidences()).isEmpty();
        assertThat(empty.message()).contains("No after-sale policy");
    }

    @Test
    void vectorMapperConvertsGivenVectorSearchResultWithoutRepositoryCalls() {
        VectorSearchMatch match = new VectorSearchMatch(
                "doc-1",
                "chunk-1",
                "Return Policy",
                "RETURN",
                "electronics",
                "Vector evidence for quality return policy.",
                0.76d,
                0.24d,
                "fake-embedding",
                "{\"chunkIndex\":0}");
        VectorSearchResult vectorResult = VectorSearchResult.matched(List.of(match));

        RagPolicySearchResult result = new VectorPolicyEvidenceMapper().toRagResult("quality return", vectorResult);

        assertThat(result.query()).isEqualTo("quality return");
        assertThat(result.retrievalMode()).isEqualTo(RetrievalMode.VECTOR);
        assertThat(result.totalKeywordMatches()).isZero();
        assertThat(result.totalVectorMatches()).isEqualTo(1);
        RagPolicyEvidence evidence = result.evidences().get(0);
        assertThat(evidence.source()).isEqualTo(RagPolicyEvidenceSource.VECTOR_CHUNK);
        assertThat(evidence.documentId()).isEqualTo("doc-1");
        assertThat(evidence.chunkId()).isEqualTo("chunk-1");
        assertThat(evidence.policyId()).isNull();
        assertThat(evidence.score()).isEqualTo(0.76d);
        assertThat(evidence.vectorScore()).isEqualTo(0.76d);

        RagPolicySearchResult empty = new VectorPolicyEvidenceMapper().toRagResult(
                "quality return",
                VectorSearchResult.empty("No vector evidence.", true));
        assertThat(empty.evidences()).isEmpty();
        assertThat(empty.fallbackUsed()).isTrue();
        assertThat(empty.message()).contains("No vector evidence");
    }

    private static RagPolicyEvidence keywordEvidence(String snippet, String metadataJson) {
        return new RagPolicyEvidence(
                "evidence-1",
                null,
                null,
                "policy-1",
                null,
                "RETURN",
                "electronics",
                snippet,
                0.8d,
                0.8d,
                null,
                RetrievalMode.KEYWORD,
                RagPolicyEvidenceSource.KEYWORD_POLICY,
                null,
                null,
                metadataJson);
    }
}
