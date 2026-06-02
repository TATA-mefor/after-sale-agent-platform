# V4.3.4 Docker Compose / Opt-in PGvector Integration Docs

Date: 2026-05-22
Status: Completed

## Goal

Add a local development only PostgreSQL + PGvector Docker Compose entrypoint and integration documentation for later
V4.4 Policy Ingestion, V4.5 HYBRID RAG search, and future JDBC vector repository work without changing default runtime
or test behavior.

## Scope Completed

- Added independent opt-in PGvector compose file.
- Added a RAG environment example with placeholder local development values.
- Added PGvector local setup documentation.
- Added compose/docs harness tests for opt-in boundary, secret safety, default compose non-regression, and non-goals.
- Updated V4 roadmap, active plan, RAG vector decision, RAG retrieval contract, README, and quality score docs.

## What Changed

- `docker-compose-rag.yml` defines a `pgvector` service using a PGvector PostgreSQL image, dedicated volume, healthcheck,
  and `schema-rag-postgres.sql` initdb mount.
- `.env.rag.example` documents `rag-postgres` / PGvector environment variables and placeholder compose settings.
- `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md` explains local startup, shutdown, cleanup, schema initialization, verification,
  default test isolation, and common troubleshooting.
- The default `docker-compose.yml` app + MySQL path remains unchanged and does not depend on PGvector.

## Docker Compose Boundary

The PGvector compose file is opt-in and local development only. It does not add an app service dependency, does not
change the default Docker Compose path, and is not production deployment guidance.

## PGvector Local Setup Boundary

The local setup guide only prepares a PGvector database for future stages. V4.3.4 does not implement
`JdbcPolicyVectorRepository`, PGvector live search, Policy Ingestion, HYBRID retrieval, or app runtime vector wiring.

## Schema Initialization Boundary

`schema-rag-postgres.sql` is mounted into PostgreSQL initdb for new local volumes. Existing volumes will not rerun the
init script automatically; operators can recreate the volume or import the schema manually. The schema is still not
loaded by default tests or the default Spring profile.

## Default Test Boundary

Default tests do not start Docker and do not connect to PostgreSQL, PGvector, MySQL, Redis, real LLM providers, real
embedding providers, or external network.

## Documentation Boundary

Documentation states that PGvector compose is opt-in only, local development only, and does not make
`search_aftersale_policy` use vector search. Later V4.4 / V4.5 work remains explicitly planned.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No JDBC repository is implemented.
- No PGvector similarity search is executed.
- No Spring AI VectorStore integration is implemented.
- No EmbeddingClient call is made.
- No Policy Ingestion, chunking runtime, RAG runtime, or HYBRID retrieval is implemented.
- `search_aftersale_policy` still uses the existing behavior and is not wired to vector search.

## Follow-ups

- V4.4: Policy Ingestion and chunking.
- V4.5: HYBRID RAG integration for `search_aftersale_policy`.
- Future V4.3.x / V4.5 support: JDBC repository and explicit opt-in integration tests.
- V4.7: Execution Tree policy evidence nodes and evaluation.

## Completion Signal

TASK_COMPLETE
