# EXEC_PLAN_V5_A3_PGVECTOR_CONNECTIVITY_SMOKE_TEST

## Date

2026-06-01

## Status

Completed

Status: Completed

## Goal

Add an explicit opt-in live PGvector connectivity smoke test for the `JdbcPolicyVectorRepository` path while keeping
default validation offline, deterministic, and independent from PostgreSQL / PGvector.

## Scope Completed

- Added `JdbcPolicyVectorRepositorySmokeTest` as a live-only smoke test.
- Added docs harness coverage for the V5.A.3 smoke boundary.
- Updated README, validation docs, PGvector setup docs, RAG retrieval contract, vector-store decision, quality notes,
  release summary, and the active correction plan status.
- Recorded V5.A.3 completed without changing runtime business behavior.

## What Changed

- The smoke test runs only with:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

- It uses the existing project environment variable names:

```text
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
AFTERSALE_PGVECTOR_SCHEMA
```

- `AFTERSALE_PGVECTOR_SCHEMA` is optional and defaults to `public`.
- Missing required configuration skips the smoke test through JUnit assumptions.
- default `mvn test` does not run live PGvector smoke.

## Smoke Test Boundary

The smoke test validates live SQL connectivity and repository plumbing only. It writes temporary `v5a3-smoke-` rows,
checks document/chunk/embedding lookup, checks vector search ordering, checks duplicate and invalid-vector failure
sanitization, and cleans up the temporary rows.

The smoke does not call ToolRegistry, does not create AgentRun records, does not write ToolCallTrace, does not write
Workspace, does not read Execution Tree, and does not execute `search_aftersale_policy`.

## PGvector Connectivity Boundary

The smoke validates the explicit `JdbcPolicyVectorRepository` / PGvector infrastructure path when a live database is
provided. It is not a default test path and it is not a production readiness gate.

Live PGvector remains opt-in. Spring AI `VectorStore` production path is not enabled.

## Fake / Fixed Vector Boundary

The smoke uses fake / fixed vectors. It does not call real LLMs, real embedding providers, Spring AI embedding models,
Spring AI `VectorStore`, or external embedding services.

The fixed vectors only prove deterministic persistence and ranking behavior for the JDBC adapter.

## Schema Initialization Boundary

The smoke executes `schema-rag-postgres.sql` against the configured database before inserting temporary rows.

`schema-rag-postgres.sql` starts with `CREATE EXTENSION IF NOT EXISTS vector`. Fresh `docker-compose-rag.yml`
initialization runs this through the PostgreSQL init mount. Existing PGvector instances commonly have the extension
preinstalled. If the configured database user cannot create the extension, the smoke skips with a sanitized setup
reason instead of exposing connection details.

Flyway / Liquibase migration management remains pending V5.B.2.

## Default Offline Boundary

Default validation remains offline:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Default validation does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
providers, Spring AI `VectorStore`, or external network.

## RAG Quality Boundary

V5.A.3 does not validate RAG quality. It does not add reranking, query rewriting, RRF, chunk window expansion, semantic
evaluation, or production-scale relevance benchmarking.

RAG evidence remains policy evidence only. Evidence score is a retrieval score, not business decision confidence.

## Secret Safety Boundary

The smoke and docs use environment variable names and placeholders only. They do not commit API keys, database
password values, tokens, local absolute paths, raw prompts, raw vectors from private datasets, or raw dataset paths.

Failure assertions require sanitized messages and do not expose JDBC URLs, credentials, raw SQL details, full vectors,
or policy chunk content.

## Runtime Non-change Boundary

V5.A.3 does not modify `src/main/java`, does not change `search_aftersale_policy` retrieval algorithms, does not change
ToolRegistry execution semantics, does not change ToolCallTrace schema, does not change Workspace evidence logic, does
not change Execution Tree runtime, does not change RAG evaluation runner, does not change Actuator health behavior,
does not change OpenAPI runtime behavior, and does not change ingestion runtime.

`search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool.

## Validation Commands

Targeted docs and smoke-harness validation:

```bash
mvn test -Dtest=PgVectorConnectivitySmokeDocsTest,SchemaVersionBaselineDocsTest,JdbcPolicyVectorRepositoryDocsTest
```

Default validation gate:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional live smoke:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

The optional live smoke requires the PGvector environment variables listed above. If they are missing, the test skips
through assumptions.

## Known Limitations

- V5.A.3 completed connectivity smoke only.
- It does not validate RAG quality.
- It does not enable default live PGvector search.
- It does not enable Spring AI `VectorStore` production path.
- It does not add Flyway / Liquibase migration management.
- It does not add Admin ingestion API or public RAG HTTP endpoints.
- It does not implement reranking, query rewriting, RRF, or chunk window expansion.
- It does not prove production deployment, production monitoring, production auth, or production secret management.

## Follow-ups

- V5.A.4: PGvector profile / docs closure planned.
- V5.B.2: Flyway / Liquibase migration management remains pending.
- Future RAG quality work may evaluate reranking, query rewriting, RRF, and chunk window expansion without bypassing
  ToolRegistry, RiskPolicy, Approval, ToolCallTrace, Workspace, or Execution Tree.

## Completion Signal

TASK_COMPLETE
