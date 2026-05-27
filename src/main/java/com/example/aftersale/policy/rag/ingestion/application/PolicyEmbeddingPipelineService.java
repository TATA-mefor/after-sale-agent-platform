package com.example.aftersale.policy.rag.ingestion.application;

import com.example.aftersale.policy.rag.application.EmbeddingClient;
import com.example.aftersale.policy.rag.application.EmbeddingProviderException;
import com.example.aftersale.policy.rag.application.EmbeddingRequest;
import com.example.aftersale.policy.rag.application.EmbeddingResponse;
import com.example.aftersale.policy.rag.domain.PolicyChunk;
import com.example.aftersale.policy.rag.domain.PolicyDocument;
import com.example.aftersale.policy.rag.domain.PolicyDocumentSourceType;
import com.example.aftersale.policy.rag.domain.PolicyEmbedding;
import com.example.aftersale.policy.rag.domain.PolicyVectorRepository;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionError;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRepository;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionRun;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionSourceType;
import com.example.aftersale.policy.rag.ingestion.domain.PolicyIngestionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Offline embedding pipeline foundation for fake-provider ingestion tests.
 */
public class PolicyEmbeddingPipelineService {

    private static final LocalDate DEFAULT_EFFECTIVE_FROM = LocalDate.parse("1970-01-01");

    private final PolicyIngestionRepository ingestionRepository;
    private final PolicyVectorRepository vectorRepository;
    private final EmbeddingClient embeddingClient;
    private final Clock clock;

    public PolicyEmbeddingPipelineService(
            PolicyIngestionRepository ingestionRepository,
            PolicyVectorRepository vectorRepository,
            EmbeddingClient embeddingClient,
            Clock clock) {
        this.ingestionRepository = Objects.requireNonNull(
                ingestionRepository, "ingestionRepository must not be null");
        this.vectorRepository = Objects.requireNonNull(vectorRepository, "vectorRepository must not be null");
        this.embeddingClient = Objects.requireNonNull(embeddingClient, "embeddingClient must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PolicyEmbeddingPipelineResult embedRun(String runId, PolicyEmbeddingPipelineOptions options) {
        String normalizedRunId = requireText(runId, "runId");
        PolicyEmbeddingPipelineOptions normalizedOptions = Objects.requireNonNull(
                options, "options must not be null");
        Instant startedAt = clock.instant();
        PolicyIngestionRun run = requireRunnableRun(normalizedRunId);
        if (run.status() == PolicyIngestionStatus.CHUNKED) {
            run = ingestionRepository.updateRun(run.transitionTo(
                    PolicyIngestionStatus.EMBEDDING,
                    startedAt,
                    null));
        }

        List<PolicyIngestionDocument> documents = ingestionRepository.findDocumentsByRunId(normalizedRunId);
        Map<String, PolicyIngestionDocument> documentsById = documentsById(documents);
        List<PolicyIngestionChunk> chunks = ingestionRepository.findChunksByRunId(normalizedRunId);
        if (chunks.size() > normalizedOptions.maxChunksPerRun()) {
            throw new IllegalArgumentException("run exceeds maxChunksPerRun for embedding pipeline");
        }

        PipelineCounters counters = new PipelineCounters();
        List<PolicyEmbeddingPipelineFailure> failures = new ArrayList<>();
        for (PolicyIngestionChunk chunk : chunks) {
            counters.processedChunks++;
            PolicyIngestionDocument document = documentsById.get(chunk.ingestionDocumentId());
            if (document == null) {
                recordFailure(normalizedRunId, chunk, "DOCUMENT_NOT_FOUND",
                        "Ingestion document is missing for chunk.", null, failures, counters);
                continue;
            }
            embedChunk(document, chunk, normalizedOptions, failures, counters);
        }

        Instant finishedAt = clock.instant();
        PolicyIngestionStatus finalStatus = finalStatus(counters);
        String errorMessage = failures.isEmpty() ? null : "Embedding pipeline completed with sanitized failures.";
        PolicyIngestionRun updated = run.withStatistics(
                documents.size(),
                chunks.size(),
                counters.embeddedChunks,
                counters.failedChunks,
                finishedAt);
        ingestionRepository.updateRun(updated.transitionTo(finalStatus, finishedAt, errorMessage));

        return new PolicyEmbeddingPipelineResult(
                normalizedRunId,
                counters.processedChunks,
                counters.embeddedChunks,
                counters.skippedChunks,
                counters.failedChunks,
                counters.savedDocuments,
                counters.savedVectorChunks,
                counters.savedEmbeddings,
                failures,
                finalStatus);
    }

    private PolicyIngestionRun requireRunnableRun(String runId) {
        PolicyIngestionRun run = ingestionRepository.findRunById(runId)
                .orElseThrow(() -> new IllegalArgumentException("policy ingestion run not found: " + runId));
        if (run.status() != PolicyIngestionStatus.CHUNKED && run.status() != PolicyIngestionStatus.EMBEDDING) {
            throw new IllegalStateException(
                    "policy ingestion run must be CHUNKED or EMBEDDING before embedding pipeline");
        }
        return run;
    }

    private void embedChunk(
            PolicyIngestionDocument document,
            PolicyIngestionChunk chunk,
            PolicyEmbeddingPipelineOptions options,
            List<PolicyEmbeddingPipelineFailure> failures,
            PipelineCounters counters) {
        if (vectorRepository.findEmbeddingByChunkIdAndModel(chunk.ingestionChunkId(), options.embeddingModel())
                .isPresent()) {
            if (options.skipDuplicateEmbeddings()) {
                counters.skippedChunks++;
                return;
            }
            recordFailure(document.runId(), chunk, "DUPLICATE_EMBEDDING",
                    "Embedding already exists for chunk and model.", null, failures, counters);
            return;
        }

        try {
            EmbeddingResponse response = embeddingClient.embed(new EmbeddingRequest(
                    options.embeddingModel(),
                    chunk.content()));
            if (hasDimensionMismatch(options, response)) {
                if (options.failOnDimensionMismatch()) {
                    recordFailure(document.runId(), chunk, "DIMENSION_MISMATCH",
                            "Embedding dimension does not match expected dimension.",
                            "expected=" + options.expectedDimension() + ", actual=" + response.dimension(),
                            failures,
                            counters);
                } else {
                    counters.skippedChunks++;
                }
                return;
            }
            saveVectorRecords(document, chunk, response, options, counters);
            counters.embeddedChunks++;
        } catch (IllegalArgumentException | EmbeddingProviderException ex) {
            recordFailure(document.runId(), chunk, "EMBEDDING_FAILED",
                    "Embedding provider failed for ingestion chunk.",
                    ex.getMessage(),
                    failures,
                    counters);
        }
    }

    private void saveVectorRecords(
            PolicyIngestionDocument document,
            PolicyIngestionChunk chunk,
            EmbeddingResponse response,
            PolicyEmbeddingPipelineOptions options,
            PipelineCounters counters) {
        if (vectorRepository.findDocumentById(document.ingestionDocumentId()).isEmpty()) {
            vectorRepository.saveDocument(toPolicyDocument(document));
            counters.savedDocuments++;
        }
        if (vectorRepository.findChunkById(chunk.ingestionChunkId()).isEmpty()) {
            vectorRepository.saveChunk(toPolicyChunk(chunk));
            counters.savedVectorChunks++;
        }
        vectorRepository.saveEmbedding(toPolicyEmbedding(chunk, response, options.embeddingModel()));
        counters.savedEmbeddings++;
    }

    private void recordFailure(
            String runId,
            PolicyIngestionChunk chunk,
            String errorCode,
            String message,
            String details,
            List<PolicyEmbeddingPipelineFailure> failures,
            PipelineCounters counters) {
        PolicyEmbeddingPipelineFailure failure = new PolicyEmbeddingPipelineFailure(
                chunk.ingestionChunkId(),
                errorCode,
                message,
                removeChunkContent(details, chunk));
        failures.add(failure);
        counters.failedChunks++;
        ingestionRepository.saveError(new PolicyIngestionError(
                "embedding-error-" + chunk.ingestionChunkId() + "-" + counters.failedChunks,
                runId,
                chunk.ingestionDocumentId(),
                chunk.ingestionChunkId(),
                errorCode,
                failure.message(),
                failure.sanitizedDetails(),
                clock.instant()));
    }

    private static String removeChunkContent(String details, PolicyIngestionChunk chunk) {
        if (details == null) {
            return null;
        }
        return details.replace(chunk.content(), "<chunk-content-redacted>");
    }

    private static PolicyDocument toPolicyDocument(PolicyIngestionDocument document) {
        Instant createdAt = document.createdAt();
        return new PolicyDocument(
                document.ingestionDocumentId(),
                document.title(),
                document.category(),
                document.productType(),
                document.version(),
                toSourceType(document.sourceType()),
                document.sourceUri(),
                checksumOrFallback(document),
                document.effectiveFrom() == null ? DEFAULT_EFFECTIVE_FROM : document.effectiveFrom(),
                document.effectiveTo(),
                createdAt,
                createdAt);
    }

    private static PolicyChunk toPolicyChunk(PolicyIngestionChunk chunk) {
        return new PolicyChunk(
                chunk.ingestionChunkId(),
                chunk.ingestionDocumentId(),
                chunk.chunkIndex(),
                chunk.content(),
                chunk.tokenEstimate(),
                chunk.metadataJson(),
                chunk.createdAt());
    }

    private static PolicyEmbedding toPolicyEmbedding(
            PolicyIngestionChunk chunk,
            EmbeddingResponse response,
            String fallbackModel) {
        String model = response.model() == null || response.model().isBlank() ? fallbackModel : response.model();
        return new PolicyEmbedding(
                chunk.ingestionChunkId() + "::" + Integer.toHexString(model.hashCode()),
                chunk.ingestionChunkId(),
                model,
                response.dimension(),
                response.vector(),
                chunk.createdAt());
    }

    private static PolicyDocumentSourceType toSourceType(PolicyIngestionSourceType sourceType) {
        return switch (sourceType) {
            case LOCAL_MARKDOWN -> PolicyDocumentSourceType.MARKDOWN;
            case LOCAL_JSON, INLINE_TEXT, SEED_POLICY, ADMIN_UPLOAD -> PolicyDocumentSourceType.MANUAL;
        };
    }

    private static String checksumOrFallback(PolicyIngestionDocument document) {
        if (document.checksum() != null) {
            return document.checksum();
        }
        return "missing-checksum-" + document.ingestionDocumentId();
    }

    private static boolean hasDimensionMismatch(
            PolicyEmbeddingPipelineOptions options,
            EmbeddingResponse response) {
        return options.expectedDimension() != null && response.dimension() != options.expectedDimension();
    }

    private static PolicyIngestionStatus finalStatus(PipelineCounters counters) {
        if (counters.failedChunks == 0) {
            return PolicyIngestionStatus.COMPLETED;
        }
        if (counters.embeddedChunks > 0 || counters.skippedChunks > 0) {
            return PolicyIngestionStatus.PARTIALLY_FAILED;
        }
        return PolicyIngestionStatus.FAILED;
    }

    private static Map<String, PolicyIngestionDocument> documentsById(List<PolicyIngestionDocument> documents) {
        Map<String, PolicyIngestionDocument> byId = new LinkedHashMap<>();
        for (PolicyIngestionDocument document : documents) {
            byId.put(document.ingestionDocumentId(), document);
        }
        return byId;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static final class PipelineCounters {
        private int processedChunks;
        private int embeddedChunks;
        private int skippedChunks;
        private int failedChunks;
        private int savedDocuments;
        private int savedVectorChunks;
        private int savedEmbeddings;
    }
}
