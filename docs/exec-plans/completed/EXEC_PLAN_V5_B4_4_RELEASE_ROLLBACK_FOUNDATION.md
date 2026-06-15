# V5.B.4.4 Release / Rollback Foundation

Date: 2026-06-15

Status: Completed

## Goal

V5.B.4.4 defines the release governance and rollback runbook foundation for AfterSale-Agent.
It provides release checklists, image tag policy, Helm/K8s review policy, rollback trigger
matrix, rollback strategy, change/release note templates, and post-release verification
checklists.

This is a **release governance / runbook foundation**, not release automation and not
production deployment.

## Scope Completed

- Release / rollback foundation document: `docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md`.
- Release checklist template: `docs/deploy/release-templates/RELEASE_CHECKLIST_TEMPLATE.md`.
- Rollback checklist template: `docs/deploy/release-templates/ROLLBACK_CHECKLIST_TEMPLATE.md`.
- Change record template: `docs/deploy/release-templates/CHANGE_RECORD_TEMPLATE.md`.
- Image tag policy: immutable tags, no `latest`, Git SHA / version tag recommendations.
- Rollback trigger matrix: 13 triggers with symptom, detection, action, rollback, verify.
- Rollback strategy: image, config, secret, profile, migration caution, escalation.
- Post-release verification checklist.
- Updated deployment roadmap, README, production config docs, K8s/Helm READMEs, auth docs,
  observability docs, validation docs, quality score, remediation plan, and correction plan.
- New `ReleaseRollbackFoundationDocsTest` docs harness test.
- Updated existing docs harness tests for V5.B.4.4 state wording.

## What Changed

### New Files
- `docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md`
- `docs/deploy/release-templates/RELEASE_CHECKLIST_TEMPLATE.md`
- `docs/deploy/release-templates/ROLLBACK_CHECKLIST_TEMPLATE.md`
- `docs/deploy/release-templates/CHANGE_RECORD_TEMPLATE.md`
- `docs/exec-plans/completed/EXEC_PLAN_V5_B4_4_RELEASE_ROLLBACK_FOUNDATION.md`
- `src/test/java/io/github/tatame/aftersale/docs/ReleaseRollbackFoundationDocsTest.java`

### Updated Files
- `README.md` — V5.B.4.4 completed, V5.B.4 current scope completed.
- `docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md` — V5.B.4.4 completed, V5.B.4 current scope completed.
- `docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md` — release / rollback review section.
- `docs/deploy/K8S_HELM_FOUNDATION.md` — release / rollback cross-reference.
- `deploy/k8s/README.md` — release / rollback cross-reference.
- `deploy/helm/after-sale-agent-platform/README.md` — release / rollback cross-reference.
- `docs/deploy/AUTH_RUNTIME_FOUNDATION.md` — release review note.
- `docs/deploy/AUTH_RBAC_BOUNDARY.md` — release review note.
- `docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md` — release review note.
- `docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md` — release review note.
- `docs/quality/VALIDATION_COMMANDS.md` — V5.B.4.4 validation section.
- `docs/quality/QUALITY_SCORE.md` — V5.B.4.4 quality entry.
- `docs/quality/PROJECT_REMEDIATION_PLAN.md` — V5.B.4.4 completed, V5.B.4 current scope completed.
- `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md` — V5.B.4.4 completed.
- Multiple existing docs harness tests — V5.B.4.4 completed wording.

## Release Checklist Boundary

- Pre-flight checks: branch clean, no secret, default quality gates.
- Optional Docker/Helm/kubectl checks (not default gate).
- Release info record: image tag, git SHA, chart/app version, profiles, migration, auth,
  observability.
- Post-release verification: health probes, auth smoke, no secret leakage.
- Template uses `REPLACE_WITH_*` placeholders only.
- No real execution claims.

## Rollback Runbook Boundary

- Rollback trigger documentation.
- Rollback strategy: image, config, secret, profile, migration caution.
- Emergency disablement: profiles, actuator exposure, ingress.
- Escalation guidance.
- Rollback verification checklist.
- Migration rollback is explicitly NOT automatic — human review required.
- Template uses `REPLACE_WITH_*` placeholders only.

## Image Tag Policy Boundary

- `latest` prohibited for production-tracked images.
- Recommended: Git SHA short (`git-<shortsha>`) or version tag (`vX.Y.Z`).
- Rollback must use known-good immutable tag.
- Image signing, SBOM, provenance are future work.

## Helm / K8s Release Review Boundary

- Values, templates, profiles, ingress, autoscaling, service account reviewed.
- `secrets.create: false`, `existingSecret` pattern.
- No `helm install` or `kubectl apply` executed.
- Review policy only; not execution proof.

## Config / Secret Review Boundary

- All sensitive values use env var placeholders.
- Secret source identified: env vars, deployment platform, future secret manager.
- Production secret manager still future.
- No real secrets in any template or doc.

## Migration Review Boundary

- Flyway disabled by default.
- Schema-only baseline migrations.
- Demo seed not deployed as migration.
- DB schema rollback requires human review.
- Destructive migrations prohibited without separate approval.
- PGvector live validation remains opt-in.

## Auth / Observability Review Boundary

- API key provisioning via env/secret source reviewed.
- Role boundary verified.
- Sensitive actuator endpoints remain unexposed.
- Prometheus opt-in only.
- Correlation IDs in logs, no secrets in logs.
- Production monitoring still future.

## Automation Boundary

- No GitHub release workflow.
- No image registry push.
- No semantic-release or version bump automation.
- No Helm install / upgrade automation.
- No kubectl apply automation.
- No CD pipeline.
- No Argo CD, Flux, Terraform, or cloud provider deployment.

## Production Deployment Boundary

- Production deployment is not completed.
- No real Kubernetes cluster deployment.
- No real image has been pushed to a registry.
- No Helm release has been installed.
- No production monitoring backend.
- No external secret manager.

## Runtime Non-change Boundary

V5.B.4.4 does NOT modify:
- `src/main/java/**`
- `src/main/resources/**`
- `pom.xml`
- `Dockerfile`
- `.github/workflows/ci.yml`
- Migration SQL
- K8s manifest YAML (except README references)
- Helm templates (except README references)
- ToolRegistry
- AgentApplicationService
- RAG runtime
- Spring Security runtime
- Health indicators
- Metrics recorder
- Prometheus config
- Correlation filter
- OpenAPI runtime config
- ToolCallTrace
- Workspace
- Execution Tree

## Default Offline Boundary

Default validation does NOT require:
- Real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis
- Real embedding provider, Spring AI live provider calls
- Kubernetes, Helm, kubectl
- Docker registry
- External network

## Validation Commands

```bash
mvn test -Dtest=ReleaseRollbackFoundationDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional (not default gate):

```bash
docker build -t after-sale-agent-platform:local .
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
kubectl apply --dry-run=client -f deploy/k8s/
```

## Known Limitations

- No real release has been executed.
- No rollback has been executed.
- No image has been pushed to a registry.
- No Helm install / upgrade has been executed.
- No kubectl apply has been executed.
- No GitHub release workflow exists.
- No external secret manager.
- No Flyway migration rollback.
- No production monitoring backend.
- No production incident response runbooks.
- No image signing or SBOM.

## Follow-ups

- Future: GitHub release workflow.
- Future: image registry push.
- Future: external secret manager integration.
- Future: production monitoring backend.
- Future: alerting rules and incident response runbooks.
- Future: canary / blue-green deployment evaluation.
- Future: Argo CD / Flux evaluation.
- Future: Flyway migration rollback strategy.
- Future: image signing and SBOM.

## Completion Signal

TASK_COMPLETE
