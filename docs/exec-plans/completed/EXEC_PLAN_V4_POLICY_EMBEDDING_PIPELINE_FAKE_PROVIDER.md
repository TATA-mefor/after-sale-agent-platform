# V4.4.3 Policy Embedding Pipeline with Fake Provider

Date: 2026-05-27
Status: Completed

## Goal

Add an offline-testable policy embedding pipeline that converts ingestion chunks into vector repository records through
the project-owned `EmbeddingClient` and `PolicyVectorRepository` contracts.

## Scope Completed

- Added fake-provider embedding pipeline models.
- Added `PolicyEmbeddingPipelineService`.
- Added deterministic ingestion-to-vector model mapping.
- Added default offline tests with `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`.
- Updated V4 roadmap, RAG contract, vector-store decision, README, and quality notes.

## What Changed

- `PolicyEmbeddingPipelineOptions` defines embedding model, optional expected dimension, dimension mismatch behavior,
  duplicate embedding behavior, max chunks per run, and batch size.
- `PolicyEmbeddingPipelineResult` reports processed, embedded, skipped, failed, saved document, saved chunk, saved
  embedding, failure, and final status counts.
- `PolicyEmbeddingPipelineFailure` sanitizes failure messages and details.
- `PolicyEmbeddingPipelineService` reads ingestion runs/documents/chunks, invokes `EmbeddingClient`, writes
  `PolicyDocument`, `PolicyChunk`, and `PolicyEmbedding` through `PolicyVectorRepository`, and updates ingestion run
  status.

## Embedding Pipeline Boundary

The pipeline is an admin / ingestion foundation service. It is not an Agent runtime tool and is not registered in
`ToolRegistry`.

Allowed default path:

```text
PolicyIngestionRepository
→ PolicyEmbeddingPipelineService
→ EmbeddingClient abstraction
→ PolicyVectorRepository contract
```

Disallowed in V4.4.3:

```text
AgentRun / Handler / Skill
→ embedding pipeline
```

## Fake Provider Boundary

Default tests use `FakeEmbeddingClient`. V4.4.3 does not call real Spring AI `EmbeddingModel`, does not call
`SpringAiEmbeddingClient` in default tests, and does not require API keys or external network.

## Vector Repository Write Boundary

V4.4.3 writes through the `PolicyVectorRepository` contract and validates behavior with `InMemoryPolicyVectorRepository`.
It does not implement `JdbcPolicyVectorRepository`, connect PostgreSQL / PGvector, call Spring AI `VectorStore`, or
execute PGvector similarity search.

## Ingestion Status Boundary

Eligible runs are `CHUNKED` or `EMBEDDING`. A `CHUNKED` run transitions to `EMBEDDING` before work starts. Final status
is:

- `COMPLETED` when there are no failures;
- `PARTIALLY_FAILED` when some chunks are embedded or skipped and some fail;
- `FAILED` when all processed chunks fail.

## Default Offline Test Boundary

Default tests remain offline. They do not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, API keys,
real embedding providers, or external network.

## Architecture Boundary

Architecture tests allow ingestion application code to depend on `EmbeddingClient` abstraction and
`PolicyVectorRepository` contract. They continue to forbid Spring AI adapter/classes, Spring AI `VectorStore`, JDBC,
`DataSource`, PGvector infrastructure, vector memory infrastructure, business repositories, Tool, Handler, and Skill
dependencies from the embedding pipeline boundary.

## Validation Commands

Required validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No real embedding provider is called by default tests.
- No JDBC ingestion or vector repository exists.
- No PGvector live search is implemented.
- No Admin ingestion API or ingestion tool exists.
- `search_aftersale_policy` is not wired to vector or hybrid retrieval.
- The fake embeddings are deterministic test vectors and do not represent production embedding quality.

## Follow-ups

- V4.4.4: ingestion docs and final V4.4 completion record.
- V4.5: HYBRID RAG search integration for `search_aftersale_policy`.
- Future opt-in work: JDBC ingestion/vector repositories and PGvector-backed integration tests.

## Completion Signal

TASK_COMPLETE
