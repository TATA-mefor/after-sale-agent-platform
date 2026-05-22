# V4.3.2 Vector Schema and Repository Contract

## Date

2026-05-22

## Status

Completed

## Goal

Define the PostgreSQL / PGvector schema file, pure RAG vector domain models, and repository contract needed for later
fake vector store, policy ingestion, and hybrid RAG policy retrieval work.

V4.3.2 is a schema and contract phase only. It does not connect to PostgreSQL, execute PGvector similarity search, call
Spring AI `VectorStore`, call `EmbeddingClient`, ingest policy documents, or change AgentRun behavior.

## Scope Completed

- Added an opt-in RAG PostgreSQL schema file for future PGvector use.
- Added pure domain models for policy documents, chunks, embeddings, vector search queries, matches, and results.
- Added `PolicyVectorRepository` as an interface-only contract.
- Added schema harness tests, domain model tests, repository contract tests, and architecture boundary checks.
- Updated V4 roadmap, RAG vector store decision, RAG policy retrieval contract, README, and quality score docs.

## What Changed

- `schema-rag-postgres.sql` defines:
  - `policy_documents`
  - `policy_chunks`
  - `policy_embeddings`
  - PGvector extension setup
  - primary keys, foreign keys, unique constraints, and retrieval indexes
- `policy.rag.domain` now contains:
  - `PolicyDocument`
  - `PolicyChunk`
  - `PolicyEmbedding`
  - `PolicyDocumentSourceType`
  - `VectorSearchQuery`
  - `VectorSearchMatch`
  - `VectorSearchResult`
  - `PolicyVectorRepository`
- Tests verify schema content, secret safety, default profile isolation, model validation, contract usability, and
  architecture boundaries.

## Vector Schema Boundary

The schema file is a resource for the future explicit `rag-postgres` path. It is not referenced by default
configuration and is not auto-loaded by default tests.

The schema uses `vector(1536)` as the current default OpenAI-compatible embedding dimension. Runtime configuration
continues to expose `AFTERSALE_EMBEDDING_DIMENSION`, and Java contracts do not hardcode a single supported dimension.

`policy_ingestion_runs` is intentionally not part of V4.3.2. Ingestion schema and runtime remain V4.4 scope.

## Domain Model Boundary

RAG vector domain models are plain Java records. They do not depend on Spring, Spring Web, JDBC, `DataSource`,
`JdbcTemplate`, PGvector, Spring AI, or database annotations.

The models validate basic invariants such as required identifiers, non-empty finite vectors, topK bounds, and evidence
score bounds. Search matches are evidence records only and do not represent completed refunds, exchanges, coupons,
payments, logistics changes, or dispute closure.

## Repository Contract Boundary

`PolicyVectorRepository` is a contract only. It exposes save, find, and search methods for later repository
implementations but does not provide a production fake, JDBC implementation, Spring AI `VectorStore` adapter, or
similarity search runtime in V4.3.2.

Agent, Handler, and Skill layers must not depend directly on this contract. Future RAG behavior must enter the Agent
execution path through ToolRegistry and policy application boundaries.

## Default Test Boundary

Default validation remains offline and deterministic. V4.3.2 tests do not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, or external network access.

## Architecture Boundary

Architecture rules now cover:

- `policy.rag.domain` stays free of Spring / JDBC / Spring AI dependencies.
- `PolicyVectorRepository` does not depend on infrastructure packages.
- Agent, Handler, and Skill packages do not depend directly on `PolicyVectorRepository`, PGvector infrastructure,
  `DataSource`, `JdbcTemplate`, or Spring AI `VectorStore`.
- ToolRegistry remains the only tool execution boundary for Agent-visible capabilities.

## Validation Commands

Required validation for this task:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No `FakePolicyVectorRepository` production implementation yet.
- No JDBC repository or PGvector similarity search.
- No Spring AI `VectorStore` integration.
- No Policy Ingestion, chunking service, or ingestion run schema.
- No HYBRID RAG retrieval and no `search_aftersale_policy` behavior change.
- No PostgreSQL Docker Compose service.

## Follow-ups

- V4.3.3: fake vector store / default offline vector tests.
- V4.3.4: Docker Compose / opt-in integration docs.
- V4.4: Policy Ingestion.
- V4.5: HYBRID RAG policy search tool integration through ToolRegistry.

## Completion Signal

TASK_COMPLETE
