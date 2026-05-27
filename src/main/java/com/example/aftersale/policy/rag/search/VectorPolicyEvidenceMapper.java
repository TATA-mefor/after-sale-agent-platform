package com.example.aftersale.policy.rag.search;

import com.example.aftersale.policy.rag.domain.VectorSearchMatch;
import com.example.aftersale.policy.rag.domain.VectorSearchResult;
import java.util.List;
import java.util.Objects;

public class VectorPolicyEvidenceMapper {

    public RagPolicySearchResult toRagResult(String query, VectorSearchResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<RagPolicyEvidence> evidences = result.matches().stream()
                .map(this::toEvidence)
                .toList();
        return new RagPolicySearchResult(
                query,
                RetrievalMode.VECTOR,
                evidences,
                result.message(),
                result.fallbackUsed(),
                0,
                evidences.size());
    }

    private RagPolicyEvidence toEvidence(VectorSearchMatch match) {
        return new RagPolicyEvidence(
                "vector:" + match.chunkId(),
                match.documentId(),
                match.chunkId(),
                null,
                match.documentTitle(),
                match.category(),
                match.productType(),
                match.snippet(),
                match.score(),
                null,
                match.score(),
                RetrievalMode.VECTOR,
                RagPolicyEvidenceSource.VECTOR_CHUNK,
                null,
                null,
                match.metadataJson());
    }
}
