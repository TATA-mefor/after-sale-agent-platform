# V5.B Production Hardening — Completion Summary

Date: 2026-06-15

Status: V5.B Production Hardening current planned scope completed

## Overview

V5.B is the production hardening phase of AfterSale-Agent. It takes the V4 RAG/Agent/demo
foundation and adds container, CI, configuration, migration, observability, auth, Kubernetes,
and release/rollback hardening across 13 sub-phases (V5.B.1 through V5.B.4.4).

> **Key message**: The project has moved from demo/backend foundation toward production
> hardening foundation. It is NOT a full production deployment. Real production IAM,
> monitoring backend, release automation, and cluster deployment are still future work.

## What V5.B Completed

### B1 — Container + CI
- Multi-stage Dockerfile (Maven build + Java 17 JRE runtime, non-root `aftersale` user).
- `.dockerignore` excluding `.env`, keys, certs, IDE artifacts, logs, temp files.
- GitHub Actions CI: `mvn test`, checkstyle, spotbugs, ArchitectureTest, Docker build.
- Image does not contain secrets; no registry push.

### B2 — Config + Secret + Migration
- Configuration baseline: `application.yml` default offline, `application-prod.example.yml`
  template only, explicit `mysql`/`rag-postgres` opt-in profiles.
- Secret boundary: Docker/CI do not bake or inject live secrets. Real secrets from env vars
  or future secret manager.
- Flyway migration foundation: dependencies added, default-disabled, profile-specific
  migration locations, schema-only MySQL/PGvector baseline migrations. Liquibase not introduced.
- Profile matrix validation: file-based harness coverage for all profiles, Flyway, CI,
  and live smoke boundaries.

### B3 — Observability
- Readiness/liveness Actuator probes: `/actuator/health/liveness`,
  `/actuator/health/readiness`. Actuator exposure health-only.
- Micrometer low-cardinality metrics: `aftersale.*` meters with sanitized tags.
- Prometheus opt-in: `observability-prometheus` profile. Not exposed by default.
- Local HTTP correlation: `X-Correlation-Id`/`X-Request-Id` with safe character/len bounds,
  MDC `correlationId`/`requestId`. Structured logging.
- Observability docs map and completion record.

### B4 — Auth + K8s + Release/Rollback
- Auth/RBAC boundary decision: role model (`CUSTOMER`, `AGENT_OPERATOR`, `SUPERVISOR`,
  `ADMIN`, `SYSTEM_SERVICE`), API access matrix, actuator/OpenAPI/Approval/ToolRegistry/RAG
  boundaries.
- Opt-in API key auth: `security-api-key` profile, `X-API-Key` header, stateless enforcement.
  Default profile permit-all. Health probes public.
- K8s/Helm foundation: manifest templates (deployment, service, configmap, secret.example),
  Helm chart skeleton, non-root securityContext, readiness/liveness probes, ConfigMap/Secret
  placeholder boundary. No Ingress, no HPA.
- Release/rollback foundation: release checklist, image tag policy (immutable, no `latest`),
  13-trigger rollback matrix, post-release verification, change/release note templates.
  No release/rollback automation.

## Default Offline Validation

Every V5.B phase preserves the default offline validation gate:

```bash
mvn test                    # ~629 tests, all pass
mvn checkstyle:check        # 0 violations
mvn spotbugs:check          # 0 bugs
mvn test -Dtest=ArchitectureTest  # 42 rules, all pass
```

Default validation does NOT require Docker, Helm, kubectl, Kubernetes, real LLMs, API keys,
PostgreSQL, PGvector, MySQL, Redis, real embedding providers, or external network.

## Production Safety Boundaries

- **No real secrets committed**: all manifests, templates, and docs use placeholders.
- **No real registry**: image references use `REPLACE_WITH_IMAGE_TAG`.
- **No real cluster**: K8s manifests are template foundation only.
- **No automation executed**: release/rollback are governance docs, not scripts.
- **Auth is opt-in**: default profile remains permit-all and offline.
- **Migration is default-disabled**: Flyway `enabled: false`.
- **Prometheus is opt-in**: `/actuator/prometheus` unexposed by default.
- **Docs harness coverage**: every phase has targeted `*DocsTest` verifying file existence,
  boundary wording, secret safety, and non-goals.

## What Is Still Future

| Area | Status |
|------|--------|
| Production cluster deployment | Not done |
| Image registry push | Not done |
| Release automation (GitHub release, CD) | Not done |
| Rollback automation | Not done |
| Secret manager integration | Not done |
| External IAM (OAuth2/OIDC/JWT) | Not done |
| Production monitoring backend | Not done |
| Grafana dashboards / alerting | Not done |
| OpenTelemetry / distributed tracing | Not done |
| SBOM / image signing / provenance | Not done |
| Canary / blue-green / Argo CD / Flux | Not done |
| Production DB provisioning | Not done |
| Real refund / exchange / payment / logistics | Not connected |

## Interview Talking Points

**What does "V5.B current planned scope completed" mean?**
The 13 sub-phases of V5.B (container, CI, config, migration, observability, auth, K8s,
release/rollback) each met their planned scope. V5.B.4.2 added real runtime auth (opt-in
API key). V5.B.3 added real Micrometer metrics. V5.B.1 added a real Dockerfile and CI.
But production deployment, release automation, and monitoring backend are explicitly future.

**Is this production-ready?**
No. V5.B is production hardening *foundation* — the scaffolding that a real production
deployment would build on. The project has a Dockerfile, CI, K8s manifests, Helm chart,
release checklist, and opt-in auth. But no image has been pushed, no cluster deployed,
no secrets managed externally, and no monitoring backend exists.

**What's the most production-relevant signal?**
The default offline validation gate (629 tests, checkstyle, spotbugs, architecture tests)
runs deterministically without any external dependencies. This is the baseline a production
release would need to maintain.

## References

- [V5.B Completion Record](../exec-plans/completed/EXEC_PLAN_V5_B_PRODUCTION_HARDENING_COMPLETION.md)
- [Deployment Hardening Roadmap](DEPLOYMENT_HARDENING_ROADMAP.md)
- [Container + CI Hardening](CONTAINER_CI_HARDENING.md)
- [Config / Secret / Migration Plan](CONFIG_SECRET_MIGRATION_PLAN.md)
- [Migration Foundation](MIGRATION_FOUNDATION.md)
- [Observability Docs Completion](OBSERVABILITY_DOCS_COMPLETION.md)
- [Auth / RBAC Boundary](AUTH_RBAC_BOUNDARY.md)
- [Auth Runtime Foundation](AUTH_RUNTIME_FOUNDATION.md)
- [K8s / Helm Foundation](K8S_HELM_FOUNDATION.md)
- [Release / Rollback Foundation](RELEASE_ROLLBACK_FOUNDATION.md)
- [Production Config Template](PRODUCTION_CONFIG_TEMPLATE.md)

## Completion Signal

TASK_COMPLETE
