# V5.A.2 Schema Init / Version Baseline

Date: 2026-06-01

Status: Completed

## Goal

Record a schema version baseline for the existing PGvector policy evidence schema after V5.A.1 introduced the opt-in
`JdbcPolicyVectorRepository` adapter.

## Scope Completed

- Added schema version comments to `src/main/resources/schema-rag-postgres.sql`.
- Documented version `2026-06-01-001` as the current initialization baseline for PGvector policy evidence search.
- Updated RAG / PGvector docs, deployment docs, validation docs, README, quality score, and the active correction plan.
- Added a docs harness test that reads files only and verifies the schema baseline, safety boundaries, and future-work
  wording.

## What Changed

- `schema-rag-postgres.sql` now declares:
  - schema version `2026-06-01-001`;
  - intended use by `JdbcPolicyVectorRepository` / PGvector policy evidence search;
  - migration framework pending V5.B.2;
  - manual SQL import or docker-compose-rag init mount as the current initialization paths;
  - default test boundary excluding this SQL from default `mvn test`.
- Documentation now distinguishes schema baseline documentation from production migration management.

## Schema Version Baseline Boundary

The baseline records the current schema shape and initialization contract only. It does not change table definitions,
indexes, constraints, extension setup, retrieval algorithms, evidence scoring, or runtime behavior.

## Manual Init Boundary

The current initialization paths are manual SQL import, a fresh `docker-compose-rag` volume init mount, or a future
explicit test setup that deliberately runs the schema SQL. Existing local database volumes do not rerun init scripts
automatically.

## Migration Framework Boundary

V5.A.2 does not add Flyway, Liquibase, migration directories, runtime schema initialization, rollback scripts, or
production database migration behavior. Migration framework work remains pending V5.B.2.

## PGvector Repository Boundary

V5.A.1 completed the explicit opt-in `JdbcPolicyVectorRepository` adapter. V5.A.2 only documents the schema baseline
used by that opt-in path. It does not add live PGvector smoke validation, does not enable Spring AI `VectorStore`
production use, and does not add a public RAG HTTP endpoint.

## Default Offline Boundary

Default validation remains offline and deterministic. It does not require real LLMs, API keys, PostgreSQL, PGvector,
Docker, MySQL, Redis, real embedding providers, Spring AI `VectorStore`, or external network access.

## Runtime Non-change Boundary

No runtime business code was changed. This task does not modify `search_aftersale_policy`, retrieval algorithms,
ToolRegistry semantics, RAG health indicators, OpenAPI behavior, ingestion runtime, ToolCallTrace, Workspace,
Execution Tree, AgentRun, or public API behavior.

## Validation Commands

```bash
mvn test -Dtest=SchemaVersionBaselineDocsTest,JdbcPolicyVectorRepositoryDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Live PGvector connectivity validation is not completed.
- Flyway / Liquibase migration management is not completed.
- Spring AI `VectorStore` production path is not enabled.
- Production deployment, production auth, production monitoring, and production database migration are not completed.
- RAG quality improvements such as reranking, query rewriting, RRF, and chunk window expansion are not completed.

## Follow-ups

- V5.A.3: add an explicit opt-in PGvector connectivity smoke test.
- V5.A.4: close PGvector profile and docs validation around the opt-in path.
- V5.B.2: choose and implement a database migration framework if approved.

## Completion Signal

TASK_COMPLETE
