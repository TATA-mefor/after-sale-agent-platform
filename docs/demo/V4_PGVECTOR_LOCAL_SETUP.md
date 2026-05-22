# V4.3.4 PGvector Local Setup

## Purpose

This guide describes the opt-in local PostgreSQL + PGvector development path for future V4 RAG work.

V4.3.4 only adds Docker Compose and integration documentation. It does not add a `JdbcPolicyVectorRepository`, policy
ingestion, HYBRID retrieval, Spring AI `VectorStore` usage, or any runtime change to `search_aftersale_policy`.

## Current Boundary

Available in this phase:

- `docker-compose-rag.yml` starts a local PGvector-capable PostgreSQL container.
- `schema-rag-postgres.sql` defines the future policy vector tables.
- `application-rag-postgres.yml` defines explicit `rag-postgres` profile properties.
- Fake vector store tests remain the default offline vector validation path.

Not available in this phase:

- No `JdbcPolicyVectorRepository` yet.
- No Policy Ingestion yet.
- No HYBRID retrieval yet.
- `search_aftersale_policy` is not wired to vector search yet.
- The app cannot execute real vector search through PGvector yet.

## Start PGvector

```bash
docker compose -f docker-compose-rag.yml up -d
```

The compose file is opt-in only and local development only. The default `docker-compose.yml` app + MySQL path does not
depend on PGvector.

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

App cannot execute real vector search: expected in V4.3.4. The JDBC repository and HYBRID RAG wiring are later phases.
