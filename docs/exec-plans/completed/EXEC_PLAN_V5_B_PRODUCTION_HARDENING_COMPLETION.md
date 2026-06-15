# V5.B Production Hardening Completion

Date: 2026-06-15

Status: Completed

## Goal

V5.B Production Hardening adds production-oriented foundation work to AfterSale-Agent across
container/CI, configuration/secret/migration, observability, and auth/K8s/release-rollback
dimensions. V5.B is a **production hardening foundation**, not a full production deployment.

V5.B current planned scope completed. Production deployment, release automation, rollback
automation, external IAM, secret manager, production monitoring backend, SBOM/signing, and
real external business integrations remain future work.

## Scope Completed

| Phase | Scope | Status |
|-------|-------|--------|
| V5.B.1 | Container + CI Foundation | Completed |
| V5.B.2.1 | Config / Secret Boundary | Completed |
| V5.B.2.2 | Flyway Migration Foundation | Completed |
| V5.B.2.3 | Profile Matrix Validation | Completed |
| V5.B.3.1 | Readiness / Liveness Boundary | Completed |
| V5.B.3.2 | Micrometer Metrics Foundation | Completed |
| V5.B.3.3 | Prometheus Opt-in Exposure | Completed |
| V5.B.3.4 | Tracing / Correlation Boundary | Completed |
| V5.B.3.5 | Observability Docs + Completion | Completed |
| V5.B.4.1 | Auth / RBAC Boundary Decision | Completed |
| V5.B.4.2 | Spring Security / API Key Auth | Completed |
| V5.B.4.3 | K8s / Helm Foundation | Completed |
| V5.B.4.4 | Release / Rollback Foundation | Completed |

## What Changed

### V5.B.1 Container + CI Summary

- Multi-stage Dockerfile with non-root `aftersale` user.
- `.dockerignore` secret-safety exclusions.
- `.github/workflows/ci.yml` Maven quality gate + Docker build validation.
- Image does not contain secrets; no registry push.
- Docs: `docs/deploy/CONTAINER_CI_HARDENING.md`.
- Test: `ContainerCiHardeningDocsTest`.

### V5.B.2 Config + Secret + Migration Summary

- V5.B.2.1: config baseline, profile matrix, secret boundary, migration follow-up plan.
  Docs: `docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md`, `DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md`.
- V5.B.2.2: Flyway dependencies, default-disabled, profile-specific migration locations,
  schema-only MySQL/PGvector baseline migrations. Liquibase not introduced.
  Docs: `docs/deploy/MIGRATION_FOUNDATION.md`.
- V5.B.2.3: file-based profile matrix validation harness for default, mysql, rag-postgres,
  prod template, Flyway, CI, live smoke boundaries.
  Tests: `ProfileMatrixValidationTest`, `ProfileMatrixValidationDocsTest`.
- Flyway remains disabled by default; secret manager not implemented.

### V5.B.3 Observability Summary

- V5.B.3.1: readiness/liveness Actuator probes. `/actuator/health/liveness`,
  `/actuator/health/readiness` available. Actuator exposure health-only.
- V5.B.3.2: low-cardinality Micrometer `ApplicationMetricsRecorder`. Tag sanitization.
  `/actuator/metrics` not exposed by default.
- V5.B.3.3: `micrometer-registry-prometheus` + `observability-prometheus` opt-in profile.
  `/actuator/prometheus` not exposed by default.
- V5.B.3.4: safe `X-Correlation-Id`/`X-Request-Id` handling, MDC correlationId/requestId,
  structured logging. No OpenTelemetry or distributed tracing.
- V5.B.3.5: observability docs map + completion record.
- Production monitoring backend, Grafana, alerting, OpenTelemetry remain future work.

### V5.B.4 Auth + K8s + Release/Rollback Summary

- V5.B.4.1: documentation-only RBAC role model, API access matrix, actuator/OpenAPI boundary,
  Approval/ToolRegistry/RAG boundary. Docs: `DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md`,
  `AUTH_RBAC_BOUNDARY.md`.
- V5.B.4.2: opt-in Spring Security API key auth. `security-api-key` profile enables
  `X-API-Key` enforcement. Default profile permit-all and offline. Roles: `ADMIN`,
  `SUPERVISOR`, `AGENT_OPERATOR`, `SYSTEM_SERVICE`. Runtime tests included.
  Docs: `AUTH_RUNTIME_FOUNDATION.md`.
- V5.B.4.3: K8s manifest templates + Helm chart skeleton. Non-root securityContext,
  readiness/liveness probe wiring, ConfigMap/Secret placeholder boundary. All manifests
  use `REPLACE_WITH_*` placeholders. No Ingress, no HPA. Docs: `K8S_HELM_FOUNDATION.md`.
- V5.B.4.4: release governance/runbook foundation. Release checklist, image tag policy
  (immutable tags, no `latest`), 13-trigger rollback matrix, rollback strategy, post-release
  verification, change/release note templates. No release/rollback automation.
  Docs: `RELEASE_ROLLBACK_FOUNDATION.md`.

## Runtime / Documentation Boundary

V5.B phases are a mix of documentation-only and targeted runtime additions:

- **Documentation-only**: V5.B.2.1, V5.B.3.5, V5.B.4.1, V5.B.4.4 (and most of V5.B.2.3,
  V5.B.4.3).
- **Runtime additions** (opt-in profiles only): V5.B.1 (Dockerfile, CI), V5.B.2.2 (Flyway
  deps, default-disabled), V5.B.3.1-V5.B.3.4 (actuator probes, metrics, Prometheus,
  correlation), V5.B.4.2 (Spring Security API key auth).
- No runtime change modified `AgentApplicationService`, `ToolRegistry`, RAG retrieval,
  `search_aftersale_policy`, `ToolCallTrace`, `Workspace`, `Execution Tree`, or
  `Approval` state machine semantics.
- Default profile remains in-memory, offline, and permit-all.

## Default Offline Boundary

Default `mvn test` does NOT require:
- Real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis
- Real embedding provider, Spring AI live calls
- Kubernetes, Helm, kubectl
- Docker registry
- External network
- Secret manager
- CI runner
- Prometheus / Grafana / OpenTelemetry collector

## Security Boundary

- API key auth is explicit opt-in (`security-api-key` profile).
- Default profile remains permit-all and offline.
- No real API keys, database passwords, tokens, or private endpoints committed.
- `secret.example.yaml` uses `REPLACE_WITH_RUNTIME_SECRET` placeholders only.
- Helm chart defaults `secrets.create: false`.
- Production secret manager not implemented.
- External IAM (OAuth2/OIDC, JWT, session login, user database) not implemented.
- Rate limiting not implemented.

## Deployment Boundary

- K8s manifests and Helm chart are deployment foundation templates only.
- No `kubectl apply` executed.
- No `helm install` or `helm upgrade` executed.
- No image pushed to any registry.
- No GitHub release workflow.
- No CD pipeline.
- No production deployment.

## Release / Rollback Boundary

- Release checklist, rollback trigger matrix, image tag policy defined.
- Post-release verification checklist defined.
- Change record and release note templates available.
- Release automation not implemented.
- Rollback automation not implemented.
- Semantic-release / automated version bump not implemented.
- No real release or rollback executed.

## Production Readiness Boundary

V5.B establishes production hardening foundation but does NOT deliver:

| Capability | Status |
|-----------|--------|
| Production deployment | Not completed |
| Registry push | Not completed |
| Release automation | Not completed (governance docs only) |
| Rollback automation | Not completed (runbook docs only) |
| Secret manager | Not completed |
| External IAM / OIDC | Not completed |
| Production monitoring backend | Not completed |
| Grafana / dashboards / alerting | Not completed |
| OpenTelemetry / distributed tracing | Not completed |
| SBOM / image signing / provenance | Not completed |
| Canary / blue-green / Argo CD / Flux | Not completed |
| Production database provisioning | Not completed |
| Real refund / exchange / payment / logistics | Not connected |

## Known Limitations

- V5.B is production hardening foundation, not full production deployment.
- No real Kubernetes cluster deployment has been performed.
- No image has been pushed to a registry.
- No Helm release has been installed.
- No GitHub release workflow or CD pipeline exists.
- Flyway remains disabled by default; migration rollback not implemented.
- Production IAM (OAuth2/OIDC) not implemented.
- Secret manager not integrated.
- Production monitoring backend not implemented.
- Live PGvector validation is explicit opt-in only.
- Real refund / exchange / payment / logistics integrations not connected.

## Remaining Future Work

| Area | Future Tasks |
|------|-------------|
| Registry | Image push, registry credentials, image signing/SBOM |
| Release | GitHub release workflow, semantic-release, CD pipeline |
| Secret Mgmt | External secret manager, sealed secrets, secret rotation |
| IAM | OAuth2/OIDC, JWT, session login, user database, rate limiting |
| Monitoring | Grafana, alerting, OpenTelemetry, log aggregation |
| Deployment | Production Ingress, TLS, HPA, NetworkPolicy, cert-manager |
| Migration | Flyway migration rollback strategy, live migration validation |
| GitOps | Argo CD / Flux evaluation, canary / blue-green |
| Infrastructure | Terraform, cloud provider deployment, production DB provisioning |

## Validation Commands

```bash
mvn test -Dtest=ProductionHardeningCompletionDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Completion Signal

TASK_COMPLETE
