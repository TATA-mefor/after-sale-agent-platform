# V5.A RAG Production Path Summary

Date: 2026-06-01

Status: Completed

## Project Summary

V5.A closes the RAG production path foundation for AfterSale-Agent by adding an explicit opt-in JDBC PGvector adapter,
a schema version baseline, and a live-only PGvector connectivity smoke while preserving the default fake / in-memory
validation path.

## What V5.A Delivered

- V5.A.1: opt-in `JdbcPolicyVectorRepository` behind the `PolicyVectorRepository` port.
- V5.A.2: schema version baseline `2026-06-01-001` in `schema-rag-postgres.sql`.
- V5.A.3: opt-in `JdbcPolicyVectorRepositorySmokeTest` gated by `-Dlive.rag=true`.
- V5.A.4: completion record, release summary, and docs harness closure.

## Technical Highlights

- Default tests remain offline and deterministic.
- The PGvector path is profile-gated and explicit opt-in.
- The smoke uses fake / fixed vectors and the existing `AFTERSALE_PGVECTOR_*` variables.
- Missing live smoke configuration skips through assumptions instead of failing the default gate.
- Schema extension setup failures from insufficient PGvector permissions are treated as setup skips with sanitized
  reasons.

## RAG / Tool Boundary

`search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool. RAG evidence remains policy evidence only.
Evidence scores are retrieval scores, not business decision confidence.

V5.A does not change retrieval algorithms, ToolRegistry semantics, ToolCallTrace schema, Workspace evidence logic,
Execution Tree runtime, RAG evaluation, Actuator health, or OpenAPI behavior.

## How To Validate

Default validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional PGvector smoke:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

PowerShell:

```powershell
mvn test "-Dtest=JdbcPolicyVectorRepositorySmokeTest" "-Dlive.rag=true"
```

## What Is Intentionally Not Production / Live

V5.A does not complete production deployment, production auth / RBAC, production monitoring, Flyway / Liquibase
migration management, Spring AI `VectorStore` production use, real embedding quality validation, reranking, query
rewriting, RRF, chunk window expansion, production ingestion admin APIs, or real refund / exchange / payment /
logistics integrations.

## Future Roadmap

- V5.B.1 Container + CI foundation is now completed through Dockerfile hardening, `.dockerignore`, CI Maven quality
  gate, and Docker build validation.
- V5.B.2.1 Config + Secret Boundary / Profile Matrix Plan is now completed through docs and docs harness only.
- V5.B.2.2 Flyway migration foundation is now completed; Liquibase is not introduced.
- V5.B.2.3 Profile Matrix Validation is now completed through file-based harness coverage; V5.B.2 current scope
  completed.
- V5.B.3.1 Readiness / Liveness Boundary is now completed; readiness / liveness actuator probe boundary completed and
  Actuator web exposure remains health-only.
- V5.B.3.2 Micrometer metrics foundation is now completed. `/actuator/metrics` and `/actuator/prometheus` remain
  unavailable by default.
- V5.B.3.3 Prometheus opt-in exposure is now completed. `/actuator/prometheus` is available only with the explicit
  `observability-prometheus` profile. V5.B.3.4 tracing / correlation boundary completed for local HTTP log
  correlation. V5.B.3.5 observability docs + completion record completed. Production monitoring backend and
  V5.B.4 planned auth / Kubernetes / release hardening remain future work.
- Optional Spring AI `VectorStore` production path decision.
- RAG quality enhancements and real embedding evaluation.
- Production auth / RBAC, deployment, monitoring, and external business integrations.
