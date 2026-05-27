package com.example.aftersale.policy.rag.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicyIngestionRepositoryContractTest {

    @Test
    void contractCanBeImplementedWithoutDatabaseOrVectorStore() {
        PolicyIngestionRepository repository = new TestPolicyIngestionRepository();
        PolicyIngestionRun run = PolicyIngestionDomainModelTest.sampleRun(
                PolicyIngestionDomainModelTest.sampleSource());

        repository.saveRun(run);

        assertThat(repository.findRunById("run-1")).contains(run);
        assertThat(repository.findDocumentsByRunId("run-1")).isEmpty();
        assertThat(repository.findDocumentByChecksum("checksum-doc")).isEmpty();
        assertThat(repository.findChunksByRunId("run-1")).isEmpty();
        assertThat(repository.findChunksByChecksum("checksum-chunk")).isEmpty();
        assertThat(repository.findChunksByDocumentIdAndChecksum("doc-1", "checksum-chunk")).isEmpty();
        assertThat(repository.findErrorsByRunId("run-1")).isEmpty();
    }

    private static final class TestPolicyIngestionRepository implements PolicyIngestionRepository {

        private final List<PolicyIngestionRun> runs = new ArrayList<>();

        @Override
        public PolicyIngestionRun saveRun(PolicyIngestionRun run) {
            runs.add(run);
            return run;
        }

        @Override
        public Optional<PolicyIngestionRun> findRunById(String runId) {
            return runs.stream().filter(run -> run.runId().equals(runId)).findFirst();
        }

        @Override
        public PolicyIngestionRun updateRun(PolicyIngestionRun run) {
            return run;
        }

        @Override
        public PolicyIngestionDocument saveDocument(PolicyIngestionDocument document) {
            return document;
        }

        @Override
        public List<PolicyIngestionDocument> findDocumentsByRunId(String runId) {
            return List.of();
        }

        @Override
        public Optional<PolicyIngestionDocument> findDocumentByChecksum(String checksum) {
            return Optional.empty();
        }

        @Override
        public PolicyIngestionChunk saveChunk(PolicyIngestionChunk chunk) {
            return chunk;
        }

        @Override
        public List<PolicyIngestionChunk> findChunksByRunId(String runId) {
            return List.of();
        }

        @Override
        public List<PolicyIngestionChunk> findChunksByDocumentId(String ingestionDocumentId) {
            return List.of();
        }

        @Override
        public List<PolicyIngestionChunk> findChunksByChecksum(String checksum) {
            return List.of();
        }

        @Override
        public List<PolicyIngestionChunk> findChunksByDocumentIdAndChecksum(
                String ingestionDocumentId,
                String checksum) {
            return List.of();
        }

        @Override
        public PolicyIngestionError saveError(PolicyIngestionError error) {
            return error;
        }

        @Override
        public List<PolicyIngestionError> findErrorsByRunId(String runId) {
            return List.of();
        }
    }
}
