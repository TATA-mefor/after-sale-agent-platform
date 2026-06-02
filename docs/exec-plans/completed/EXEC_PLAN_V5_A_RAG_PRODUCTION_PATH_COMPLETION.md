# V5.A RAG Production Path Completion

Date: 2026-06-01

Status: Completed

## Goal

Close the V5.A RAG production path foundation after the opt-in JDBC PGvector adapter, schema baseline, and live
connectivity smoke were added.

V5.A completed means the repository now has a documented, explicit opt-in PGvector persistence path foundation. It does
not mean the whole platform is production-ready, and production deployment is not completed.

## Scope Completed

- Closed the V5.A documentation path across README, RAG contracts, vector-store decision notes, PGvector local setup,
  quality notes, validation commands, project remediation docs, deployment docs, and release summary.
- Added a concise V5.A release summary for reviewers.
- Added docs harness coverage for V5.A completion, dual-path RAG boundaries, optional smoke validation, and overclaim
  safety.
- Preserved default fake / in-memory validation as the default path.

## What Changed

- V5.A.1 completed the explicit opt-in `JdbcPolicyVectorRepository` infrastructure adapter behind the
  `PolicyVectorRepository` port.
- V5.A.2 completed schema version baseline `2026-06-01-001` for `schema-rag-postgres.sql`.
- V5.A.3 completed an explicit opt-in PGvector connectivity smoke for the JDBC adapter.
- V5.A.4 closes the documentation and completion record for the V5.A RAG production path foundation.

## V5.A.1 Summary

V5.A.1 added `JdbcPolicyVectorRepository` as a PostgreSQL / PGvector infrastructure adapter. It remains profile-gated,
behind the existing `PolicyVectorRepository` contract, and outside Agent, Tool, Skill, Controller, and public API
runtime paths.

## V5.A.2 Summary

V5.A.2 added schema version baseline `2026-06-01-001` to the PGvector schema SQL. The baseline documents current manual
initialization expectations and gives future migration work a stable starting point.

It is not Flyway / Liquibase migration management. Flyway / Liquibase migration management remains pending V5.B.2.

## V5.A.3 Summary

V5.A.3 added `JdbcPolicyVectorRepositorySmokeTest` as a live-only connectivity smoke. It requires `-Dlive.rag=true`
and the existing `AFTERSALE_PGVECTOR_URL`, `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`, and optional
`AFTERSALE_PGVECTOR_SCHEMA` variables.

The smoke uses fake / fixed vectors. It validates SQL connectivity, persistence, lookup, vector ranking, cleanup, and
sanitized failure handling only.

## Default Fake / In-memory Path Boundary

Default validation keeps using fake / in-memory dependencies:

- `InMemoryPolicyVectorRepository`;
- fake embedding paths;
- deterministic RAG evaluation cases;
- no live PGvector database;
- no real embedding provider;
- no Spring AI `VectorStore`.

Default `mvn test` does not connect PostgreSQL / PGvector and does not run the live smoke.
default `mvn test` does not run live PGvector smoke.

## Opt-in Jdbc / PGvector Path Boundary

The JDBC PGvector path is explicit opt-in. It is an infrastructure path for policy evidence persistence and search, not
a public RAG HTTP endpoint and not an Agent runtime tool.

`JdbcPolicyVectorRepository` remains behind `PolicyVectorRepository`. It does not replace fake / in-memory defaults,
does not change `search_aftersale_policy` retrieval algorithms, and does not alter ToolRegistry execution semantics.

## Schema Baseline Boundary

The schema baseline documents current table/index initialization for local PGvector development and opt-in smoke tests.

No Flyway or Liquibase framework is enabled. No automatic production migration workflow is added.

## PGvector Connectivity Smoke Boundary

The smoke validates opt-in connectivity only. It does not prove production readiness, production-scale operation, or
retrieval relevance quality.

If a configured PGvector user cannot run `CREATE EXTENSION IF NOT EXISTS vector`, the smoke may skip with a sanitized
setup reason. Fresh `docker-compose-rag.yml` initialization and already prepared PGvector instances remain the intended
local setup paths.

## RAG Quality Boundary

V5.A does not complete RAG quality enhancement. Reranking, query rewriting, RRF, chunk window expansion, production
relevance benchmarks, and real embedding quality validation remain future work.

Evidence score is a retrieval score, not business decision confidence.

## ToolRegistry / Evidence-only Boundary

`search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool. RAG evidence remains policy evidence only and
does not execute refunds, exchanges, coupon compensation, payment changes, logistics changes, inventory changes, or
dispute closure.

LLMs must not directly execute tools. High-risk actions still require Approval.

## Default Offline Boundary

Default validation remains offline and deterministic. It does not require:

- real LLMs;
- API keys;
- PostgreSQL;
- PGvector;
- Docker;
- MySQL;
- Redis;
- real embedding providers;
- Spring AI `VectorStore`;
- external network.

## Production Hardening Boundary

V5.A is not full production hardening. The following remain future work:

- production auth / RBAC;
- production monitoring;
- secret manager integration;
- Dockerfile hardening;
- CI/CD pipeline;
- Kubernetes / Helm;
- Flyway / Liquibase migration management;
- production ingestion API or admin UI;
- Spring AI `VectorStore` production path;
- RAG quality enhancements;
- real refund, exchange, coupon compensation, payment, logistics, or inventory integrations.

## Runtime Non-change Boundary

V5.A.4 is documentation closure only. It does not modify runtime business code, controllers, services, tool executors,
RAG search, ingestion pipeline, health indicators, OpenAPI config, ToolCallTrace, Workspace, Execution Tree, or
AgentApplicationService.

## Validation Commands

Default validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Targeted docs validation:

```bash
mvn test -Dtest=RagProductionPathCompletionDocsTest,PgVectorConnectivitySmokeDocsTest,SchemaVersionBaselineDocsTest,JdbcPolicyVectorRepositoryDocsTest
```

Optional live smoke, not part of the default gate:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

PowerShell users may quote system-property arguments:

```powershell
mvn test "-Dtest=JdbcPolicyVectorRepositorySmokeTest" "-Dlive.rag=true"
```

## Known Limitations

- V5.A is a RAG production path foundation, not a complete production platform.
- Production deployment is not completed.
- Production monitoring is not completed.
- Production auth / RBAC is not completed.
- production auth / RBAC is not completed.
- production monitoring is not completed.
- Flyway / Liquibase migration management is not completed.
- Spring AI `VectorStore` production path is not enabled.
- RAG quality enhancement is not completed.
- Real embedding quality validation is not completed.
- real embedding quality validation is not completed.
- Real external refund, exchange, payment, logistics, coupon compensation, and inventory integrations are not
  connected.

## Follow-ups

- V5.B: production hardening implementation planning.
- V5.B.2: Flyway / Liquibase migration management.
- Future: production auth / RBAC and deployment hardening.
- Future: RAG quality enhancements such as reranking, query rewriting, RRF, chunk window expansion, and real embedding
  evaluation.
- Future: optional Spring AI `VectorStore` production path decision.

## Completion Signal

TASK_COMPLETE
