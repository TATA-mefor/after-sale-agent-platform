# V4.3.4 PGvector Local Setup

## Purpose

This guide describes the opt-in local PostgreSQL + PGvector infrastructure path introduced by V4.3.4.

Read this page as local infrastructure documentation, not as proof that the default application path uses live
PGvector. Later V4.4 and V4.5 stages added offline policy ingestion foundation and KEYWORD / VECTOR / HYBRID
`search_aftersale_policy` runtime through ToolRegistry, but the default path still uses fake / in-memory dependencies.

## Current Boundary

Available as V4 foundation:

- `docker-compose-rag.yml` starts a local PGvector-capable PostgreSQL container.
- `schema-rag-postgres.sql` defines the future policy vector tables.
- `application-rag-postgres.yml` defines explicit `rag-postgres` profile properties.
- `PolicyVectorRepository` defines the application contract.
- `InMemoryPolicyVectorRepository` and fake vector tests remain the default vector validation path.
- V4.4 policy ingestion foundation can write through the repository contract in offline tests.
- V4.5 `search_aftersale_policy` supports KEYWORD / VECTOR / HYBRID retrieval through ToolRegistry using the current
  default fake / in-memory vector path.
- Fake vector store tests remain the default offline vector validation path.
- V5.A.1 adds an explicit opt-in `JdbcPolicyVectorRepository` for the `rag-postgres` / `pgvector` profile.
- V5.A.2 records schema version baseline `2026-06-01-001` in `schema-rag-postgres.sql`.
- V5.A.3 adds an explicit opt-in live smoke test for `JdbcPolicyVectorRepository` connectivity and fixed-vector
  persistence/search behavior.

Still not completed:

- Default live PGvector write/search.
- Spring AI `VectorStore` production path.
- Default live PGvector integration validation.
- A production app + PGvector deployment compose file.

## Schema Version Baseline

Current schema version baseline: `2026-06-01-001`.

The baseline is intended for `JdbcPolicyVectorRepository` / PGvector policy evidence search. It documents the current
shape of `policy_documents`, `policy_chunks`, and `policy_embeddings` without changing table, index, constraint, or
extension semantics.

Supported initialization paths:

- Fresh `docker-compose-rag.yml` volume initialization through the init mount.
- Manual SQL import of `src/main/resources/schema-rag-postgres.sql`.
- Test setup that explicitly runs the schema SQL for a future opt-in live PGvector test.

Existing PostgreSQL volumes do not rerun `/docker-entrypoint-initdb.d` scripts. Recreate the local volume or manually
import the SQL when the baseline needs to be applied to an existing local database.

V5.A.3 validates live PGvector connectivity only when explicitly requested. Flyway / Liquibase migration management is
pending V5.B.2.

## Start PGvector

```bash
docker compose -f docker-compose-rag.yml up -d
```

The compose file is opt-in only and local development only. The default `docker-compose.yml` app + MySQL path does not
depend on PGvector. `docker-compose-rag.yml` provides PGvector infrastructure only; it is not a production deployment
guide and not a complete app + PGvector production compose.

## Stop And Clean Up

```bash
docker compose -f docker-compose-rag.yml down
docker compose -f docker-compose-rag.yml down -v
```

Use `down -v` only when you want to remove the `aftersale_pgvector_data` volume and rerun the init script on the next
startup.

## Schema Initialization

`docker-compose-rag.yml` mounts:

```text
./src/main/resources/schema-rag-postgres.sql
→ /docker-entrypoint-initdb.d/01-schema-rag-postgres.sql
```

PostgreSQL init scripts run only when the data directory is first created. If the volume already exists, use `down -v`
for a fresh local database, or import the schema manually:

```bash
docker compose -f docker-compose-rag.yml exec -T pgvector psql -U aftersale_rag -d after_sale_agent_rag < src/main/resources/schema-rag-postgres.sql
```

## Environment Example

`.env.rag.example` contains placeholder local development values:

```text
AFTERSALE_RAG_ENABLED=true
AFTERSALE_VECTOR_STORE_PROVIDER=pgvector
AFTERSALE_PGVECTOR_ENABLED=true
AFTERSALE_PGVECTOR_URL=jdbc:postgresql://localhost:5433/after_sale_agent_rag
AFTERSALE_PGVECTOR_USERNAME=aftersale_rag
AFTERSALE_PGVECTOR_PASSWORD=aftersale_rag
AFTERSALE_PGVECTOR_SCHEMA=public
AFTERSALE_EMBEDDING_DIMENSION=1536
```

Inside a Docker Compose network, use:

```text
AFTERSALE_PGVECTOR_URL=jdbc:postgresql://pgvector:5432/after_sale_agent_rag
```

Do not commit local `.env` files, API keys, real database passwords, tokens, or local machine paths.

## Verify Locally

```bash
docker compose -f docker-compose-rag.yml ps
docker compose -f docker-compose-rag.yml exec pgvector pg_isready -U aftersale_rag -d after_sale_agent_rag
docker compose -f docker-compose-rag.yml exec pgvector psql -U aftersale_rag -d after_sale_agent_rag -c "\\dt"
```

Expected tables after schema initialization:

```text
policy_documents
policy_chunks
policy_embeddings
```

Optional live JDBC repository smoke:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

The smoke uses the existing project environment variable names:

```text
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
AFTERSALE_PGVECTOR_SCHEMA
```

`AFTERSALE_PGVECTOR_SCHEMA` is optional and defaults to `public`. Missing required configuration skips the smoke. The
test uses fake / fixed vectors and validates SQL connectivity, schema availability, persistence, lookup, vector search
ranking, cleanup, and sanitized failures only. It does not call real LLMs, real embedding providers, Spring AI
`VectorStore`, ToolRegistry, AgentRun, or `search_aftersale_policy`, and it does not validate RAG quality.

`schema-rag-postgres.sql` includes `CREATE EXTENSION IF NOT EXISTS vector`. Fresh `docker-compose-rag.yml` volumes run
the SQL through the init mount, where extension setup is expected to be available. If you point the smoke at an
existing PGvector instance, the extension is usually preinstalled. If the configured database user cannot create the
extension, the smoke skips with a sanitized setup reason instead of exposing credentials.

## Default Test Isolation

Default validation does not start Docker and does not connect to PostgreSQL, PGvector, MySQL, Redis, a real LLM, a real
embedding provider, or external network:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## FAQ

Port conflict on `5433`: set `AFTERSALE_PGVECTOR_PORT` in a local `.env` file and adjust `AFTERSALE_PGVECTOR_URL`.

Schema not loaded: init scripts only run for a new volume. Use `docker compose -f docker-compose-rag.yml down -v` and
start again, or import `schema-rag-postgres.sql` manually.

Password authentication failed: verify `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`, and whether an old
volume was initialized with different credentials.

Vector extension unavailable: use the PGvector image from `docker-compose-rag.yml`; a plain PostgreSQL image may not
include the `vector` extension.

App cannot execute real vector search through PGvector by default: expected. HYBRID `search_aftersale_policy` wiring is
available through ToolRegistry, and V5.A.1 adds an explicit opt-in JDBC repository adapter for `rag-postgres` /
`pgvector`. V5.A.3 adds an explicit opt-in smoke test for that adapter. Default validation still uses fake / in-memory
dependencies; default live PGvector persistence/search and any Spring AI `VectorStore` production path require separate
future approval.

V5.A completion: V5.A completed the RAG production path foundation by closing V5.A.1 opt-in JDBC adapter, V5.A.2 schema
baseline, V5.A.3 opt-in connectivity smoke, and V5.A.4 docs / completion record. See
`docs/exec-plans/completed/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md` and
`docs/release/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md`.

This completion does not make PGvector part of default validation, does not validate RAG retrieval quality, does not
validate real embedding quality, does not enable Spring AI `VectorStore` production use, and does not add Flyway /
Liquibase migration management.
