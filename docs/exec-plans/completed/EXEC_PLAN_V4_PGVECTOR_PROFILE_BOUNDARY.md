# V4.3.1 PostgreSQL / PGvector Profile Boundary Completion Record

Date: 2026-05-22
Status: Completed

## Goal

Establish the PostgreSQL / PGvector dependency and profile boundary for later V4 vector store work without implementing
schema, repositories, vector search, policy ingestion, RAG, or AgentRun runtime changes.

## Scope Completed

- Added PostgreSQL JDBC as a runtime dependency.
- Added default-off RAG / PGvector properties in `application.yml`.
- Added explicit `rag-postgres` profile configuration.
- Added PGvector properties, profile guard, and sanitized configuration exception.
- Added tests for default-off behavior, property binding, missing configuration, complete opt-in configuration, and
  MySQL profile non-regression.
- Extended architecture checks for PGvector / VectorStore / JDBC boundaries.

## What Changed

V4.3.1 now has a project-owned configuration boundary for future PGvector work. The boundary can validate opt-in
configuration and expose a sanitized readiness marker, but it does not create database clients or retrieval runtime.

## PostgreSQL / PGvector Dependency Boundary

PostgreSQL JDBC is available for later opt-in integration. Spring AI PGvector VectorStore usage is intentionally not
introduced in V4.3.1 to avoid default auto-configuration risk and to keep schema / repository work in V4.3.2.

## Profile Boundary

`rag-postgres` is the only PGvector profile. Default configuration keeps:

```text
agent.rag.enabled=false
agent.rag.vector-store.provider=none
agent.rag.vector-store.pgvector.enabled=false
```

When `rag-postgres` is active, the profile expects:

```text
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
```

Optional settings include `AFTERSALE_PGVECTOR_SCHEMA` and `AFTERSALE_EMBEDDING_DIMENSION`.

## Default Test Boundary

Default tests do not create a PostgreSQL `DataSource`, `JdbcTemplate`, Spring AI `VectorStore`, PGvector schema,
repository, database connection, Docker service, or network dependency. The V3 MySQL profile remains separate.

## Architecture Boundary

Agent, Handler, and Skill layers must not depend directly on PGvector infrastructure, Spring AI VectorStore,
`DataSource`, or `JdbcTemplate`. PGvector infrastructure must not access business repositories or Agent execution
packages.

## Validation Commands

Required:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No `policy_documents`, `policy_chunks`, `policy_embeddings`, or `policy_ingestion_runs` schema exists yet.
- No VectorStore repository contract or fake vector store exists yet.
- No policy ingestion, chunking, embedding persistence, similarity search, hybrid retrieval, or RAG runtime exists yet.
- No PostgreSQL / PGvector Docker Compose service is added in V4.3.1.

## Follow-ups

- V4.3.2: vector schema / repository contract.
- V4.3.3: fake vector store / default offline vector tests.
- V4.3.4: Docker Compose / opt-in integration docs.
- V4.4: Policy Ingestion.
- V4.5: Hybrid RAG Policy Search Tool.

## Completion Signal

TASK_COMPLETE
