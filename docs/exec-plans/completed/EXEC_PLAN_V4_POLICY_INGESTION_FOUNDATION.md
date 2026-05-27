# EXEC PLAN V4 Policy Ingestion Foundation

Date: 2026-05-27

Status: Completed

## Goal

Close the V4.4 Policy Ingestion phase with documentation, harness tests, and a total completion record that accurately
describes the completed ingestion foundation and its boundaries.

## Scope Completed

- Documented the V4.4 ingestion pipeline foundation.
- Recorded the V4.4.1, V4.4.2, V4.4.3, and V4.4.4 completion status.
- Updated V4 roadmap, RAG contract, vector store decision, quality summary, and README status.
- Added docs harness coverage for ingestion documentation boundaries and secret safety.

## V4.4.1 Summary

V4.4.1 completed the ingestion domain, status model, repository contract, and in-memory repository foundation. It added
ingestion run/source/document/chunk/error models, status transition validation, and offline repository tests.

## V4.4.2 Summary

V4.4.2 completed deterministic chunking, token estimate, SHA-256 checksum, and checksum dedup services. It added
repository checksum queries and offline tests for chunking, checksum, dedup, and architecture boundaries.

## V4.4.3 Summary

V4.4.3 completed the fake-provider embedding pipeline. It uses `FakeEmbeddingClient` in default tests and writes
`PolicyDocument`, `PolicyChunk`, and `PolicyEmbedding` through the `PolicyVectorRepository` contract with
`InMemoryPolicyVectorRepository`.

## V4.4.4 Summary

V4.4.4 completed ingestion documentation and the V4.4 total completion record. It did not change Java pipeline
runtime behavior.

## Ingestion Pipeline Boundary

Policy ingestion is an admin / offline pipeline capability. It is not an Agent runtime tool, is not registered in
ToolRegistry, and is not called by AgentRun, Specialist Handler, or Skill runtime code.

V4.4 does not add an Admin Controller, `ingest_policy_document` tool, ToolRegistry wiring, real Spring AI embedding
default path, JDBC ingestion repository, JDBC vector repository, PGvector live writes, RAG runtime, HYBRID retrieval,
or `search_aftersale_policy` vector wiring.

## Default Offline Test Boundary

Default validation uses in-memory repositories and fake providers only. It does not require PostgreSQL, PGvector,
Docker, MySQL, Redis, a real LLM, API keys, a real embedding provider, or external network access.

## Architecture Boundary

The ingestion domain remains free of Spring, JDBC, DataSource, PGvector, and Spring AI dependencies. The ingestion
application boundary may depend on `EmbeddingClient` and `PolicyVectorRepository` contracts for the fake-provider
pipeline, but it must not depend on concrete Spring AI adapters, JDBC repositories, PGvector infrastructure, Tool,
Handler, Skill, or business repositories.

Agent, Handler, and Skill layers must not directly access ingestion repositories, embedding pipeline services,
PGvector, VectorStore, JdbcTemplate, DataSource, `PolicyVectorRepository`, or fake vector repositories.

## Current Limitations

- No Admin ingestion API.
- No `ingest_policy_document` tool.
- No ToolRegistry wiring.
- No real Spring AI embedding default path.
- No `JdbcPolicyIngestionRepository`.
- No `JdbcPolicyVectorRepository`.
- No PGvector live writes.
- No RAG / HYBRID retrieval.
- `search_aftersale_policy` is not wired to vector search yet.

## Follow-ups

- V4.5 connects HYBRID policy retrieval to `search_aftersale_policy`.
- A later opt-in phase may add JDBC repositories and real Spring AI embedding validation.
- Any future Admin API must have explicit security and profile boundaries.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Completion Signal

TASK_COMPLETE
