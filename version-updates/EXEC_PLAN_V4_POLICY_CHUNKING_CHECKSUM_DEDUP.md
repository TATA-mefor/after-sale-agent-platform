# EXEC_PLAN_V4_POLICY_CHUNKING_CHECKSUM_DEDUP

## Date

2026-05-27

## Status

Completed

## Goal

Implement the V4.4.2 policy ingestion chunking, token estimate, checksum, and deduplication service boundary on top of
the V4.4.1 ingestion domain and repository foundation.

## Scope Completed

- Added deterministic chunking options, strategy, result, and service classes.
- Added SHA-256 checksum calculation for policy ingestion documents and chunks.
- Added checksum-based document and chunk deduplication decisions.
- Extended `PolicyIngestionRepository` with checksum query methods.
- Updated `InMemoryPolicyIngestionRepository` to support offline checksum lookup.
- Added deterministic unit tests, repository regression tests, and architecture rules.
- Updated V4 roadmap, active plan, RAG vector decision, RAG retrieval contract, README, and quality documentation.

## What Changed

V4.4.2 introduces pure Java ingestion processing helpers. `PolicyChunkingService` can split a
`PolicyIngestionDocument` into `PolicyIngestionChunk` records with deterministic chunk indexes, token estimates,
overlap handling, paragraph-boundary preference, and chunk checksums.

`PolicyContentChecksumService` computes normalized SHA-256 checksums. `PolicyIngestionDedupService` uses repository
checksum queries to classify content as `NEW_CONTENT`, `DUPLICATE_DOCUMENT`, or `DUPLICATE_CHUNK`.

## Chunking Boundary

Chunking is deterministic and does not use an external tokenizer, LLM, embedding provider, Spring AI, database, or
vector store. Chunk indexes start at `0`. Token estimate is `ceil(chars / tokenEstimateDivisor)`. Max chunk overflow
fails clearly without echoing complete raw text.

## Checksum Boundary

Checksum calculation uses Java standard-library SHA-256 only. Content is normalized by line-ending normalization and
trim before hashing. Checksums do not store API keys, database passwords, tokens, local paths, prompts, or raw datasets.

## Dedup Boundary

Deduplication reads only the ingestion repository contract. It does not call `PolicyVectorRepository`, Spring AI,
`EmbeddingClient`, PostgreSQL, PGvector, MySQL, Docker, Redis, or external services.

## Ingestion Pipeline Boundary

V4.4.2 does not implement an end-to-end ingestion pipeline. It does not call `EmbeddingClient`, write
`PolicyVectorRepository`, create a JDBC repository, expose an Admin Controller, register an ingestion tool, or modify
AgentRun / Skill / ToolRegistry / ToolCallTrace / Execution Tree behavior.

## Default Offline Test Boundary

Default tests cover chunking, checksum, dedup, repository checksum queries, and architecture boundaries without
PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, API keys, embedding providers, or external network.

## Architecture Boundary

Architecture rules verify that ingestion application services do not depend on Spring Web, JDBC, DataSource, Spring AI,
VectorStore, PGvector infrastructure, vector infrastructure, business repositories, Tool, Handler, or Skill packages.
Agent, Handler, and Skill layers remain isolated from ingestion application and repository helpers.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No V4.4.3 embedding pipeline.
- No `EmbeddingClient` call.
- No Spring AI call.
- No `PolicyVectorRepository` write.
- No `JdbcPolicyIngestionRepository`.
- No `JdbcPolicyVectorRepository`.
- No Admin Controller.
- No ingestion tool registration.
- No RAG / HYBRID retrieval runtime.
- No `search_aftersale_policy` behavior change.

## Follow-ups

- V4.4.3: implement embedding pipeline with fake provider.
- V4.4.4: complete ingestion docs and final ingestion phase records.
- V4.5: wire HYBRID RAG into `search_aftersale_policy` through ToolRegistry and evidence boundaries.

## Completion Signal

TASK_COMPLETE
