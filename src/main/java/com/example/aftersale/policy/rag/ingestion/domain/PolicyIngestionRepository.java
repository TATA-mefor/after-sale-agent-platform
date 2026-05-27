package com.example.aftersale.policy.rag.ingestion.domain;

import java.util.List;
import java.util.Optional;

public interface PolicyIngestionRepository {

    PolicyIngestionRun saveRun(PolicyIngestionRun run);

    Optional<PolicyIngestionRun> findRunById(String runId);

    PolicyIngestionRun updateRun(PolicyIngestionRun run);

    PolicyIngestionDocument saveDocument(PolicyIngestionDocument document);

    List<PolicyIngestionDocument> findDocumentsByRunId(String runId);

    PolicyIngestionChunk saveChunk(PolicyIngestionChunk chunk);

    List<PolicyIngestionChunk> findChunksByRunId(String runId);

    List<PolicyIngestionChunk> findChunksByDocumentId(String ingestionDocumentId);

    PolicyIngestionError saveError(PolicyIngestionError error);

    List<PolicyIngestionError> findErrorsByRunId(String runId);
}
