package com.example.aftersale.policy.rag.ingestion.application;

import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import java.util.List;
import java.util.Objects;

public record PolicyChunkingResult(
        PolicyIngestionDocument document,
        List<PolicyIngestionChunk> chunks,
        PolicyChunkingOptions options) {

    public PolicyChunkingResult {
        document = Objects.requireNonNull(document, "document must not be null");
        chunks = List.copyOf(Objects.requireNonNull(chunks, "chunks must not be null"));
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("chunks must not be empty");
        }
        options = Objects.requireNonNull(options, "options must not be null");
    }
}
