## Production Config Template

Stage 1 of the project review correction adds a safe production configuration example:

```text
src/main/resources/application-prod.example.yml
```

See [Production Config Template](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md) for the environment variable groups,
secret placeholder boundary, and default offline validation boundary.

This template is not loaded by default, is not a production deployment manifest, and does not add production auth,
production monitoring, secret-manager integration, live PGvector validation, live dependency readiness checks, or real
payment / logistics / refund integrations. Do not commit real API keys, database passwords, tokens, private endpoints,
local absolute paths, raw prompts, or raw datasets. Default validation still does not require real LLMs, API keys,
PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live calls, or external network.

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
and does not imply production auth, production monitoring, live dependency readiness checks, or real external business
integrations.

Secrets must be supplied through environment variables, the deployment platform, or a future secret manager. The
current Docker image does not contain secrets, and the CI default gate does not inject live secrets or run live LLM,
live Spring AI, live PGvector, live MySQL, Redis, Docker Compose, or external service checks.

V5.B.2.2 adds a Flyway migration foundation while keeping Flyway disabled by default. Liquibase is not introduced.
The current PGvector manual / Docker init baseline remains `schema-rag-postgres.sql` with version `2026-06-01-001`;
the Flyway PGvector migration is a schema-only versioned copy for explicit opt-in migration usage.

## V5.B.2.2 Flyway Migration Foundation

V5.B.2.2 adds:

- Flyway dependencies managed by Spring Boot dependency management.
- Default `spring.flyway.enabled: false` in `application.yml`.
- MySQL migration location `classpath:db/migration/mysql`, guarded by `AFTERSALE_FLYWAY_ENABLED:false`.
- PGvector migration location `classpath:db/migration/pgvector`, guarded by `AFTERSALE_RAG_FLYWAY_ENABLED:false`.
- Schema-only MySQL and PGvector baseline migrations.

See `docs/deploy/MIGRATION_FOUNDATION.md`.

This does not enable migrations in the default profile, does not add Liquibase, does not add profile matrix runtime
validation, and does not connect default validation to MySQL, PostgreSQL, PGvector, Docker, Redis, real LLMs, real
embedding providers, Spring AI live providers, or external network.

## V5.B.2.3 Profile Matrix Validation

V5.B.2.3 Profile Matrix Validation adds file-based harness coverage for default, `mysql`, `rag-postgres`, production
template, Flyway, CI, and live smoke boundaries. Profile matrix validation harness completed; V5.B.2 current scope
completed. Runtime profile behavior was not changed.

The production template remains template only. The harness verifies that profile-specific variables stay explicit:

```text
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
AFTERSALE_FLYWAY_ENABLED:false
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
AFTERSALE_PGVECTOR_SCHEMA
AFTERSALE_RAG_FLYWAY_ENABLED:false
```

Secret manager is not implemented, production deployment is not completed, production auth / RBAC is not completed,
production monitoring is not completed, and real refund / exchange / payment / logistics integrations are not
connected.

## V5.B Production Hardening Completion

V5.B Production Hardening current planned scope completed. See
`docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md` and
`docs/exec-plans/completed/EXEC_PLAN_V5_B_PRODUCTION_HARDENING_COMPLETION.md`.

Current production hardening baseline:
- Production config template (`application-prod.example.yml`).
- Profile matrix validation.
- Flyway migration foundation (default-disabled).
- API key auth profile (`security-api-key`).
- Observability profiles (readiness/liveness, metrics, Prometheus opt-in, correlation).
- K8s/Helm deployment templates.
- Release/rollback governance (checklists, runbook, image tag policy).

Still future:
- Real secret manager.
- Real production config injection.
- Real registry / cluster deployment.
- Real release automation.

## V5.B.4.3 K8s / Helm Foundation

V5.B.4.3 adds Kubernetes manifest templates and a Helm chart skeleton. See
`docs/deploy/K8S_HELM_FOUNDATION.md`, `deploy/k8s/README.md`, and
`deploy/helm/after-sale-agent-platform/README.md`.

Key boundaries:

- All manifests use safe placeholders. The image does not contain secrets.
- K8s `secret.example.yaml` uses `stringData` with `REPLACE_WITH_RUNTIME_SECRET` only.
- Helm chart defaults `secrets.create: false`; production should use `existingSecret`.
- `security-api-key` profile is available for auth runtime boundary through
  `SPRING_PROFILES_ACTIVE` or `profiles.securityApiKey.enabled: true`.
- `observability-prometheus` profile is explicit opt-in and NOT enabled by default.
- Ingress is disabled by default; production Ingress exposure remains future work.
- External secret manager integration remains future work.
- This is deployment manifest foundation only. Production deployment is not completed.
- Release / rollback hardening foundation completed (V5.B.4.4). Release automation and real
  rollback execution remain future work.

## V5.B.4.1 Auth / RBAC Boundary

V5.B.4.1 completes the production authentication / RBAC boundary decision only. See
`docs/deploy/AUTH_RBAC_BOUNDARY.md` and `docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md`.

The production template does not enforce authentication by itself. It remains an example configuration file and is not
loaded by default. V5.B.4.2 adds the opt-in `security-api-key` profile for Spring Security API key auth foundation.
Future production profile work still needs full IAM hardening before any public exposure.

Current boundary:

- full production auth / RBAC runtime is not implemented;
- opt-in Spring Security API key auth foundation is available through `security-api-key`;
- JWT, OAuth2 / OIDC, user database, and session login are not implemented;
- Kubernetes / Helm exposure waits for runtime auth;
- release / rollback governance foundation completed; release / rollback automation remains planned;
- ToolRegistry is not a public API;
- high-risk actions require Approval;
- `search_aftersale_policy` remains LOW-risk read-only;
- RAG evidence remains evidence-only policy support.

## V5.B.3.1 Readiness / Liveness Boundary

V5.B.3.1 readiness / liveness actuator probe boundary completed. See
`docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md` and
`docs/exec-plans/completed/EXEC_PLAN_V5_B3_1_READINESS_LIVENESS_BOUNDARY.md`.

The default Actuator exposure remains health-only. `/actuator/health`, `/actuator/health/liveness`, and
`/actuator/health/readiness` are available. `/actuator/env`, `/actuator/beans`, `/actuator/configprops`,
`/actuator/heapdump`, `/actuator/threaddump`, and `/actuator/prometheus` remain unavailable by default.

This does not add OpenTelemetry, collector configuration, production monitoring, or live DB / PGvector / LLM /
embedding readiness checks. V5.B.3.2 Micrometer metrics foundation completed, V5.B.3.3 Prometheus opt-in exposure
completed, V5.B.3.4 tracing / correlation boundary completed for local HTTP log correlation, and V5.B.3.5
observability docs + completion record completed. Production monitoring backend, OpenTelemetry, dashboards, alerting,
log aggregation, and V5.B.4 auth / Kubernetes / release hardening remain future work.

## V5.B.3.2 Micrometer Metrics Foundation

V5.B.3.2 adds low-cardinality Micrometer application metrics through the existing Spring Boot Actuator / Micrometer
core dependency. It records AgentRun, ToolCall, Approval, RAG search, and provider-call observations with sanitized
tags. It does not require any production secret, API key, database, PGvector, Docker, Redis, real LLM, real embedding
provider, Spring AI live provider, Prometheus, OpenTelemetry collector, or external network in the default gate.

Actuator exposure remains health-only. `/actuator/metrics` and `/actuator/prometheus` are not exposed by default.
Prometheus exposure is available only through the explicit `observability-prometheus` profile. OpenTelemetry tracing,
dashboards, production monitoring backend, production auth, Kubernetes / Helm, and release / rollback hardening remain
planned / future work.

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

