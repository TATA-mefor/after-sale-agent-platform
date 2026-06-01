# V5.A.1 JdbcPolicyVectorRepository

Date: 2026-06-01

Status: Completed

## Goal

Add an explicit opt-in JDBC implementation of the `PolicyVectorRepository` contract for the PostgreSQL / PGvector
profile while keeping the default validation path offline, deterministic, and fake / in-memory.

## Scope Completed

- Added `JdbcPolicyVectorRepository` under the PGvector infrastructure package.
- Wired the repository only behind the explicit `rag-postgres` profile and `pgvector` provider properties.
- Added sanitized PGvector repository exceptions that avoid leaking JDBC URLs, credentials, raw SQL, vectors, or local
  environment details.
- Added repository unit tests for SQL parameters, row mapping, vector literal validation, schema safety, and sanitized
  error behavior.
- Extended default offline/profile boundary tests so default application context does not create the JDBC repository or
  connect to PostgreSQL / PGvector.
- Updated current project documentation to distinguish V4 historical status from the V5.A.1 opt-in repository status.

## What Changed

- `JdbcPolicyVectorRepository` now implements document, chunk, embedding save/find operations and vector similarity
  search through `NamedParameterJdbcOperations`.
- `PgVectorConfiguration` creates the JDBC repository only when the `rag-postgres` profile is active and
  `agent.rag.vector-store.provider=pgvector` plus `agent.rag.vector-store.pgvector.enabled=true` are configured.
- SQL uses schema-qualified table names from a strict schema-name allowlist and PGvector cosine distance for retrieval
  score mapping.
- Documentation now records that V5.A.1 adds the opt-in JDBC repository, while live PGvector validation and Spring AI
  VectorStore production use remain future / opt-in work.

## JdbcPolicyVectorRepository Boundary

`JdbcPolicyVectorRepository` is an infrastructure adapter for the `PolicyVectorRepository` port. It is not an Agent
tool, not an Admin ingestion API, not a public RAG HTTP endpoint, and not a replacement for `ToolRegistry`.

Agent, Handler, and Skill code must not depend on this repository directly. `search_aftersale_policy` remains a
LOW-risk read-only ToolRegistry tool, and RAG evidence remains policy evidence only.

## PGvector Profile / Property Boundary

The JDBC repository is available only through the explicit PGvector path:

```text
spring profile: rag-postgres
agent.rag.vector-store.provider=pgvector
agent.rag.vector-store.pgvector.enabled=true
```

The default profile does not create a `DataSource`, does not create `JdbcPolicyVectorRepository`, and does not open a
PostgreSQL / PGvector connection.

## SQL / Row Mapping Boundary

- SQL is limited to the existing policy vector schema tables.
- Schema names are validated before use in SQL identifiers.
- Vector values are passed as PGvector literals and cast through SQL parameters.
- Row mapping reads document, chunk, embedding, metadata, and retrieval score fields into existing RAG domain models.
- Retrieval score is derived from vector distance; it is not a business decision confidence score.

## Sanitized Error Boundary

Repository failures are wrapped in a sanitized PGvector repository exception. Error messages must not expose database
passwords, API keys, tokens, JDBC URLs with credentials, local absolute paths, raw prompt text, raw dataset paths, raw
SQL payloads, or raw vector values.

## Runtime Non-change Boundary

V5.A.1 does not change `search_aftersale_policy` retrieval algorithms, does not change ToolRegistry execution
semantics, does not change ToolCallTrace schema, does not change Workspace evidence logic, does not change Execution
Tree runtime, does not change RAG evaluation runner, and does not add a public RAG HTTP endpoint.

## Default Offline Boundary

Default validation remains offline and does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL,
Redis, real embedding providers, Spring AI `VectorStore`, or external network access.

## Validation Commands

```bash
mvn test -Dtest=JdbcPolicyVectorRepositoryTest
mvn test -Dtest=PgVectorProfileBoundaryTest,DefaultOfflineValidationTest,ArchitectureTest
mvn test -Dtest=JdbcPolicyVectorRepositoryDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No live PGvector smoke validation is added in this phase.
- No database migration baseline, Flyway, or Liquibase integration is added.
- No Spring AI `VectorStore` production path is enabled.
- No Admin ingestion API is added.
- No public RAG HTTP endpoint is added.
- No reranking, query rewriting, RRF, or chunk window expansion runtime is added.

## Follow-ups

- Add explicit live PGvector validation as a separate opt-in task.
- Add database migration baseline in a separate deployment-hardening task.
- Evaluate Spring AI `VectorStore` only if it can preserve the existing ToolRegistry, approval, trace, and default
  offline boundaries.

## Completion Signal

TASK_COMPLETE
