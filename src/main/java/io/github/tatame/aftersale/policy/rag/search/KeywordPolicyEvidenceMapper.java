package io.github.tatame.aftersale.policy.rag.search;

import io.github.tatame.aftersale.policy.domain.PolicySearchResult;
import io.github.tatame.aftersale.policy.domain.PolicySnippet;
import java.util.List;
import java.util.Objects;

public class KeywordPolicyEvidenceMapper {

    public static final double KEYWORD_EVIDENCE_SCORE = 1.0d;

    public RagPolicySearchResult toRagResult(PolicySearchResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<RagPolicyEvidence> evidences = result.snippets().stream()
                .map(this::toEvidence)
                .toList();
        return new RagPolicySearchResult(
                result.query().queryText(),
                RetrievalMode.KEYWORD,
                evidences,
                result.message(),
                false,
                evidences.size(),
                0);
    }

    private RagPolicyEvidence toEvidence(PolicySnippet snippet) {
        return new RagPolicyEvidence(
                "keyword:" + snippet.policyId(),
                null,
                null,
                snippet.policyId(),
                null,
                snippet.category(),
                snippet.productType(),
                snippet.snippetText(),
                KEYWORD_EVIDENCE_SCORE,
                KEYWORD_EVIDENCE_SCORE,
                null,
                RetrievalMode.KEYWORD,
                RagPolicyEvidenceSource.KEYWORD_POLICY,
                null,
                null,
                "{\"matchReason\":\"" + sanitizeJsonValue(snippet.matchReason()) + "\"}");
    }

    private static String sanitizeJsonValue(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
