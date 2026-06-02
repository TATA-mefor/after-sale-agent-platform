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

