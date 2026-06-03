## Production Config Template

Stage 1 of the project review correction adds a safe production configuration example:

```text
src/main/resources/application-prod.example.yml
```

See [Production Config Template](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md) for the environment variable groups,
secret placeholder boundary, and default offline validation boundary.

This template is not loaded by default, is not a production deployment manifest, and does not add production auth,
production monitoring, secret-manager integration, live PGvector validation, or real payment / logistics / refund
integrations. Do not commit real API keys, database passwords, tokens, private endpoints, local absolute paths, raw
prompts, or raw datasets. Default validation still does not require real LLMs, API keys, PostgreSQL, PGvector, Docker,
MySQL, Redis, real embedding providers, Spring AI live calls, or external network.

Stage 6 of the project review correction records the deployment hardening route in
[Deployment Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md) and
[Deployment Hardening Roadmap](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md). This stage is documentation-only:
Dockerfile, CI/CD, Kubernetes / Helm, secret manager, production auth/RBAC, production monitoring, live PGvector
validation, and production deployment remain future work.

## Container / CI Usage

V5.B.1 adds a Dockerfile, `.dockerignore`, CI Maven quality gate, and Docker build validation. See
`docs/deploy/CONTAINER_CI_HARDENING.md` and `version-updates/EXEC_PLAN_V5_B1_CONTAINER_CI.md`.

The image does not contain secrets. Runtime configuration must still be supplied through environment variables,
Spring profiles, or the deployment platform. Do not bake API keys, database passwords, tokens, private keys, or
provider configuration into the image.

The CI default gate runs Maven tests, Checkstyle, SpotBugs, ArchitectureTest, and Docker image build validation. It
does not run live LLM, live Spring AI, live PGvector, live MySQL, Redis, Docker Compose, or external service checks.
It does not push an image or deploy the application.

`application-prod.example.yml` remains a production configuration template only. It is not loaded by default and is
not a production deployment manifest.

## V5.B.2.1 Config / Secret / Migration Boundary

V5.B.2.1 records the configuration and secret boundary in
`docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md` and
`docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md`.

`application-prod.example.yml` remains a template only. It is not loaded by default, does not deploy the application,
and does not imply production auth, production monitoring, readiness / liveness runtime changes, or real external
business integrations.

Secrets must be supplied through environment variables, the deployment platform, or a future secret manager. The
current Docker image does not contain secrets, and the CI default gate does not inject live secrets or run live LLM,
live Spring AI, live PGvector, live MySQL, Redis, Docker Compose, or external service checks.

Flyway / Liquibase migration management remains pending V5.B.2.2. The current PGvector schema baseline is
`schema-rag-postgres.sql` with version `2026-06-01-001`; it is a baseline reference, not a migration framework.

V5.A.1 adds an explicit opt-in `JdbcPolicyVectorRepository` for the `rag-postgres` / `pgvector` profile. This is an
infrastructure adapter behind `PolicyVectorRepository`, not a new Agent tool, not a public RAG HTTP endpoint, and not a
retrieval algorithm change. Default validation still uses fake / in-memory dependencies and does not connect to
PostgreSQL / PGvector. See
[V5.A.1 JdbcPolicyVectorRepository](version-updates/EXEC_PLAN_V5_A1_JDBC_POLICY_VECTOR_REPOSITORY.md).

V5.A.3 adds an explicit opt-in live PGvector smoke test for the JDBC adapter:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

The smoke uses `AFTERSALE_PGVECTOR_URL`, `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`, and optional
`AFTERSALE_PGVECTOR_SCHEMA`. Missing environment configuration skips the test through assumptions. The smoke uses
fake / fixed vectors and validates SQL connectivity, persistence, lookup, vector search ranking, cleanup, and sanitized
failure messages only. It does not call real LLMs, real embedding providers, Spring AI `VectorStore`, ToolRegistry, or
`search_aftersale_policy`, and it does not validate RAG quality.

V5.A closes the RAG production path foundation through V5.A.1 opt-in JDBC adapter, V5.A.2 schema baseline, V5.A.3
opt-in connectivity smoke, and V5.A.4 docs completion record. See
[V5.A RAG Production Path Completion](version-updates/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md) and
[V5.A RAG Production Path Summary](version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md). V5.A does not make live PGvector
part of the default gate, does not validate RAG retrieval quality, does not validate real embedding quality, does not
enable Spring AI `VectorStore` production use, and does not add Flyway / Liquibase migration management.

