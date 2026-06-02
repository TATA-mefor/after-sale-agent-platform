# EXEC_PLAN_V4_POLICY_INGESTION_DOMAIN_MODEL

## Date

2026-05-27

## Status

Completed

## Goal

Establish the V4.4.1 Policy Ingestion domain model, status model, repository contract, and default offline in-memory
persistence foundation for later chunking, checksum deduplication, embedding pipeline, and HYBRID RAG work.

## Scope Completed

- Added pure Java ingestion domain records for runs, sources, documents, chunks, errors, statuses, and chunk statuses.
- Added a status transition boundary for ingestion runs.
- Added the `PolicyIngestionRepository` contract.
- Added an in-memory ingestion repository for deterministic offline tests.
- Added domain, state transition, repository contract, duplicate behavior, sanitization, and architecture tests.
- Updated V4 roadmap, decision, RAG contract, README, and quality documentation.

## What Changed

V4.4.1 introduces the ingestion tracking foundation without executing ingestion work. Policy Ingestion now has a
separate admin/pipeline model for run status, source metadata, raw ingestion documents, generated ingestion chunks, and
sanitized ingestion errors.

The in-memory repository supports default tests and future service-level development without PostgreSQL, PGvector,
Docker, MySQL, Redis, real LLMs, API keys, embedding providers, or external network.

## Ingestion Domain Boundary

The ingestion domain package is pure Java and does not depend on Spring, JDBC, DataSource, PGvector, Spring AI,
VectorStore, or infrastructure implementations.

`PolicyIngestionDocument` may hold raw text for pipeline processing, but raw text must not be written into logs,
errors, README, Review Packets, or long diagnostic details.

## Ingestion State Boundary

The V4.4.1 state machine allows:

```text
CREATED -> RUNNING / CANCELLED
RUNNING -> CHUNKED / FAILED / CANCELLED
CHUNKED -> EMBEDDING / FAILED / CANCELLED
EMBEDDING -> COMPLETED / PARTIALLY_FAILED / FAILED / CANCELLED
```

`COMPLETED`, `FAILED`, `PARTIALLY_FAILED`, and `CANCELLED` are terminal and cannot transition again.

## Ingestion Repository Boundary

`PolicyIngestionRepository` is a contract only. It stores ingestion run, document, chunk, and error state. It is
separate from `PolicyVectorRepository` and does not write policy vector documents, chunks, or embeddings.

`InMemoryPolicyIngestionRepository` is default offline infrastructure. `saveRun` rejects duplicate run IDs,
`updateRun` updates an existing run, and duplicate document, chunk, and error IDs are rejected.

## Default Offline Test Boundary

Default tests do not connect to PostgreSQL, PGvector, MySQL, Redis, Docker, real LLMs, real embedding providers, or
external network. V4.4.1 adds no Spring bean that triggers ingestion, vector store access, database connections, or
provider calls by default.

## Architecture Boundary

Architecture rules verify:

- ingestion domain does not depend on Spring, JDBC, Spring AI, vector infrastructure, or ingestion infrastructure;
- ingestion memory infrastructure does not depend on JDBC, DataSource, Spring AI, business repositories, Tool, Handler,
  or Skill packages;
- Agent, Handler, and Skill layers do not depend on `PolicyIngestionRepository` or `InMemoryPolicyIngestionRepository`.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No chunking service.
- No checksum deduplication service.
- No embedding pipeline.
- No `EmbeddingClient` call.
- No `PolicyVectorRepository` write.
- No `JdbcPolicyIngestionRepository`.
- No `JdbcPolicyVectorRepository`.
- No Admin ingestion API or Agent ingestion tool.
- No RAG / HYBRID retrieval runtime.
- No `search_aftersale_policy` behavior change.

## Follow-ups

- V4.4.2: implement chunking and checksum dedup boundaries.
- V4.4.3: implement embedding pipeline with fake provider.
- V4.4.4: complete ingestion docs and final ingestion phase records.
- V4.5: wire HYBRID RAG into `search_aftersale_policy` through ToolRegistry and policy evidence boundaries.

## Completion Signal

TASK_COMPLETE
