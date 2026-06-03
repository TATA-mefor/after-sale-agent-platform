# V5.B.2.3 Profile Matrix Validation

Date: 2026-06-03

Status: Completed

## Goal

V5.B.2.3 closes the current V5.B.2 configuration scope with a documentation and file-based profile matrix validation
harness. The task verifies the default, `mysql`, `rag-postgres`, and production-template boundaries without changing
runtime profile behavior.

## Scope Completed

- Added file-based profile matrix tests for default, MySQL, RAG PGvector, production template, Flyway, CI, and live
  smoke boundaries.
- Added docs harness coverage for V5.B.2.3 completion status, validation commands, profile boundaries, secret safety,
  and default offline guarantees.
- Updated status docs so V5.B.2.1, V5.B.2.2, and V5.B.2.3 are completed; V5.B.2 current scope completed.
- Preserved V5.B.3 observability runtime hardening and V5.B.4 auth / Kubernetes / release hardening as future work.

## What Changed

V5.B.2.3 profile matrix validation harness completed. The harness reads repository files and checks that:

- `application.yml` remains the default offline / local baseline.
- `application-mysql.yml` remains explicit opt-in and uses the existing `AFTERSALE_MYSQL_*` variables.
- `application-rag-postgres.yml` remains explicit opt-in and uses the existing `AFTERSALE_PGVECTOR_*` variables.
- `application-prod.example.yml` remains template only and environment-driven.
- Flyway remains disabled by default while profile-specific locations are documented.
- Live PGvector smoke remains explicit opt-in and uses sanitized skip behavior.
- CI default validation does not inject live secrets or run live service checks.

Runtime profile behavior was not changed.

## Default Profile Boundary

The default profile stays offline and deterministic. `application.yml` keeps Flyway disabled by default, keeps
`DataSourceAutoConfiguration` excluded, exposes actuator health only, and keeps RAG / PGvector / Spring AI live paths
disabled unless explicitly configured.

## MySQL Profile Boundary

The `mysql` profile remains explicit opt-in through `application-mysql.yml`. It uses:

```text
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
AFTERSALE_FLYWAY_ENABLED:false
```

Its Flyway location remains `classpath:db/migration/mysql`. Default validation does not connect to MySQL.

## RAG PGvector Profile Boundary

The `rag-postgres` profile remains explicit opt-in through `application-rag-postgres.yml`. It keeps the project
existing variable convention:

```text
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
AFTERSALE_PGVECTOR_SCHEMA
AFTERSALE_RAG_FLYWAY_ENABLED:false
```

The task intentionally keeps the existing PGvector variable convention and does not add alternate rag-prefixed
PGvector variable names. Default validation does not connect to PostgreSQL or PGvector.

## Production Template Boundary

`application-prod.example.yml` remains a template only. It is not loaded by default and is not a deployment manifest.
Production deployment is not completed. Production auth, production monitoring, secret manager, live provider
validation, and real external business integrations remain future work.

## Flyway Boundary

Flyway remains disabled by default. The profile-specific locations remain:

```text
classpath:db/migration/mysql
classpath:db/migration/pgvector
```

The PGvector schema includes `CREATE EXTENSION IF NOT EXISTS vector`, which can require elevated PostgreSQL privileges.
For the live PGvector smoke, docker-compose-rag init mount, a preinstalled extension, or sanitized skip behavior remain
the current boundary.

## Live Test Boundary

The live PGvector smoke remains opt-in:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

It uses `AFTERSALE_PGVECTOR_URL`, `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`, and optional
`AFTERSALE_PGVECTOR_SCHEMA`. Missing configuration or extension setup failures skip with sanitized reasons. The live
smoke does not run in default Maven validation.

## Secret Boundary

Secret manager is not implemented. Docs and tests do not add real API keys, database passwords, tokens, private
endpoints, local absolute paths, raw prompts, raw provider responses, or raw dataset paths. Example credentials remain
placeholder-only or local development placeholders.

## CI / Default Offline Boundary

The default validation gate remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Default validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Docker Compose, secret manager, or external network.

## Runtime Non-change Boundary

Runtime profile behavior was not changed. This task does not modify `src/main/java`, `pom.xml`, application
configuration files, Dockerfile, compose files, migration SQL, ToolRegistry, `search_aftersale_policy`, RAG runtime,
ingestion pipeline, health indicators, OpenAPI config, ToolCallTrace, Workspace, Execution Tree, or
AgentApplicationService.

## Validation Commands

```bash
mvn test -Dtest=ProfileMatrixValidationTest
mvn test -Dtest=ProfileMatrixValidationDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- This is a profile matrix validation harness, not a runtime profile behavior change.
- Secret manager is not implemented.
- Production deployment is not completed.
- Production auth / RBAC is not completed.
- Production monitoring is not completed.
- Real refund / exchange / payment / logistics integrations are not connected.
- Live PGvector smoke can skip when `CREATE EXTENSION IF NOT EXISTS vector` cannot run with the configured database
  user.

## Follow-ups

- V5.B.3: observability runtime hardening.
- V5.B.4: auth, Kubernetes / Helm, release / rollback hardening.
- Future work: secret manager selection, production readiness / liveness split, and broader opt-in live deployment
  validation.

## Completion Signal

TASK_COMPLETE
