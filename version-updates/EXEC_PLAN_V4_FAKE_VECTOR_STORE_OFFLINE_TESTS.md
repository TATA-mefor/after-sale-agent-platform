# V4.3.3 Fake Vector Store / Default Offline Vector Tests

Date: 2026-05-22
Status: Completed

## Goal

Add a deterministic fake vector repository and default offline tests for the V4 RAG repository contract without
connecting to PostgreSQL, PGvector, Spring AI VectorStore, real embedding providers, or AgentRun runtime paths.

## Scope Completed

- Added a pure Java cosine similarity calculator for retrieval evidence scores.
- Added an in-memory `PolicyVectorRepository` implementation for save / find / search contract behavior.
- Added opt-in fake provider bean wiring through `agent.rag.vector-store.provider=fake`.
- Added offline tests for similarity, repository behavior, ranking, filters, empty results, duplicates, and bean
  boundary.
- Extended architecture rules for fake vector infrastructure and Agent / Handler / Skill vector boundaries.
- Updated V4 roadmap, RAG decision, RAG contract, README, and quality score documentation.

## What Changed

- `CosineSimilarityCalculator` computes deterministic cosine similarity and returns zero for zero-vector comparisons.
- `InMemoryPolicyVectorRepository` stores documents, chunks, and embeddings in memory and searches saved embeddings
  with deterministic ranking.
- Search supports `topK`, `minScore`, category, productType, effectiveAt, and embeddingModel filters.
- Empty searches return a structured empty result with no fabricated evidence.
- Duplicate document / chunk / embedding writes are rejected with clear errors.

## Fake Vector Store Boundary

The fake repository is default-off infrastructure for tests and local fake-provider wiring. It does not connect to
PostgreSQL, PGvector, Docker, MySQL, Redis, external network, Spring AI VectorStore, or real embedding providers. It
does not change `search_aftersale_policy`, ToolRegistry, ToolCallTrace, Workspace, Execution Tree, Skill runtime, or
AgentRun behavior.

## Similarity Calculation Boundary

Cosine similarity is used only as a retrieval evidence score. It is not a business decision confidence and does not
authorize refunds, exchanges, coupon compensation, payment changes, logistics changes, or dispute closure.

## Repository Contract Behavior

The repository requires documents before chunks and chunks before embeddings. It rejects duplicate document IDs, chunk
IDs, and chunk/model embedding pairs. Search never fabricates missing documents or chunks.

## Default Offline Test Boundary

Default tests use deterministic in-memory vectors and do not require real LLMs, API keys, PostgreSQL, PGvector, Docker,
MySQL, Redis, or external network. Fake provider tests assert no `DataSource`, `JdbcTemplate`, Spring AI VectorStore,
real LLM, or real embedding provider bean is created.

## Architecture Boundary

Architecture tests keep `policy.rag.domain` pure, keep fake vector infrastructure away from JDBC, Spring AI, business
repositories, Tool, Handler, and Skill packages, and keep Agent / Handler / Skill code from depending directly on
`PolicyVectorRepository` or fake vector repository implementations.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No JDBC repository or PGvector live similarity search is implemented.
- No Spring AI VectorStore integration is implemented.
- No EmbeddingClient call is made by the fake vector repository.
- No Policy Ingestion, chunking runtime, RAG runtime, or HYBRID retrieval is implemented.
- `search_aftersale_policy` still uses the existing behavior and does not call the fake vector repository.

## Follow-ups

- V4.3.4: Docker Compose / opt-in integration documentation.
- V4.4: Policy Ingestion and chunking.
- V4.5: HYBRID RAG integration for `search_aftersale_policy`.
- V4.7: Execution Tree policy evidence nodes.

## Completion Signal

TASK_COMPLETE
