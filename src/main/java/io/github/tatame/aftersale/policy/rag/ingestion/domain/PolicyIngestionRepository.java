package io.github.tatame.aftersale.policy.rag.ingestion.domain;

import java.util.List;
import java.util.Optional;

public interface PolicyIngestionRepository {

    PolicyIngestionRun saveRun(PolicyIngestionRun run);

    Optional<PolicyIngestionRun> findRunById(String runId);

    PolicyIngestionRun updateRun(PolicyIngestionRun run);

    PolicyIngestionDocument saveDocument(PolicyIngestionDocument document);

    List<PolicyIngestionDocument> findDocumentsByRunId(String runId);

    Optional<PolicyIngestionDocument> findDocumentByChecksum(String checksum);

    PolicyIngestionChunk saveChunk(PolicyIngestionChunk chunk);

    List<PolicyIngestionChunk> findChunksByRunId(String runId);

    List<PolicyIngestionChunk> findChunksByDocumentId(String ingestionDocumentId);

    List<PolicyIngestionChunk> findChunksByChecksum(String checksum);

    List<PolicyIngestionChunk> findChunksByDocumentIdAndChecksum(String ingestionDocumentId, String checksum);

    PolicyIngestionError saveError(PolicyIngestionError error);

    List<PolicyIngestionError> findErrorsByRunId(String runId);
}
