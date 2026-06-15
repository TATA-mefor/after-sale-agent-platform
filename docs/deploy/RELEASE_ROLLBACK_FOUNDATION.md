# Release / Rollback Foundation

Date: 2026-06-15

Status: V5.B.4.4 Release / Rollback Foundation completed

## Goal

V5.B.4.4 defines the release governance and rollback runbook foundation for AfterSale-Agent.
It provides release checklists, image tag policy, Helm/K8s review policy, rollback trigger
matrix, rollback strategy, and change/release note templates.

This is a **release governance / runbook foundation**, not a production deployment and not
release automation. No real release, rollback, image push, or Helm install has been executed.

## Current Release Boundary

- `Dockerfile` exists (V5.B.1) but no image has been pushed to a registry.
- `.github/workflows/ci.yml` runs quality gates but does not release.
- K8s manifests and Helm chart exist (V5.B.4.3) but no `kubectl apply` or `helm install` has been executed.
- No GitHub release workflow exists.
- No semantic-release or automated version bump exists.
- No CD pipeline exists.
- No registry credentials are configured.
- Production deployment is not completed.
- All releases so far are documentation / code review only.

## Release Preconditions

Before any future release, the following preconditions must be met:

1. **Branch clean**: no uncommitted changes, no dirty worktree.
2. **No secret committed**: no real API keys, passwords, tokens, or local absolute paths in the diff.
3. **Default quality gate passes**:
   ```bash
   mvn test
   mvn checkstyle:check
   mvn spotbugs:check
   mvn test -Dtest=ArchitectureTest
   ```
4. **Docs harness passes**: all `*DocsTest` tests pass.
5. **Profile matrix review**: default, `mysql`, `rag-postgres`, `security-api-key`,
   `observability-prometheus` profiles reviewed for correctness.
6. **Migration review**: Flyway disabled by default; migration baseline reviewed if enabled.
7. **Security profile review**: API keys provisioned via env/secret source (not committed).
8. **Readiness / liveness review**: probes point to correct Actuator paths.
9. **Prometheus opt-in review**: `/actuator/prometheus` remains unexposed by default.
10. **Rollback target identified**: previous known-good image tag or commit recorded.

## Required Validation Gates

These gates must pass in the default offline path. Docker/Helm/kubectl gates are optional
for local review and not part of the default Maven gate.

### Default Gates (Required)
```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### Optional Local Gates (Not Default Gate)
```bash
docker build -t after-sale-agent-platform:local .
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
kubectl apply --dry-run=client -f deploy/k8s/
```

Optional gates require Docker, Helm, and kubectl installed locally. If not installed,
skip them. Default Maven validation does not require these tools.

## Image Tag Policy

### Prohibited
- `latest` tag for any production-tracked image.

### Recommended Immutable Tags
- Git SHA short: `git-<shortsha>` (e.g., `git-a1b2c3d`)
- Release version tag: `vX.Y.Z` (e.g., `v0.1.0`)
- Build timestamp may be used as auxiliary metadata only, NOT as the sole production
  rollback identifier.

### Release Tag Record
Every release must record:
- Image repository placeholder: `after-sale-agent-platform`
- Image tag: `REPLACE_WITH_IMAGE_TAG`
- Git commit SHA: `REPLACE_WITH_GIT_SHA`
- Chart version: `REPLACE_WITH_CHART_VERSION`
- App version: `REPLACE_WITH_APP_VERSION`
- Active profiles: `REPLACE_WITH_SPRING_PROFILES_ACTIVE`
- Migration enabled: `REPLACE_WITH_FLYWAY_ENABLED`
- Auth profile: `REPLACE_WITH_AUTH_PROFILE`
- Observability profile: `REPLACE_WITH_OBSERVABILITY_PROFILE`

### Rollback Image Rule
Rollback MUST return to a known-good immutable image tag. Never rollback to `latest`.

### Future
- Image signing (Cosign / Sigstore).
- SBOM generation.
- Provenance attestation.
- These are future hardening items, not implemented.

## Container Image Boundary

- No real registry address is used in docs or manifests.
- No real image digest is recorded (placeholder only).
- No registry credentials are embedded in the repository.
- Docker image build validation (`docker build`) is a CI check only; no push occurs.

## Helm Release Review Boundary

Before any future Helm release, review:
1. `values.yaml` — no real secrets, no real registry.
2. `templates/secret.yaml` — `secrets.create` is `false`; `existingSecret` is configured.
3. `templates/deployment.yaml` — probes, securityContext, resource requests correct.
4. `templates/configmap.yaml` — non-sensitive values only.
5. Profile composition in `_helpers.tpl` — correct for target environment.
6. Ingress disabled (`ingress.enabled: false`).
7. Autoscaling disabled (`autoscaling.enabled: false`).
8. ServiceAccount behavior as expected.

No `helm install` or `helm upgrade` has been executed. This review policy is a pre-release
checklist, not proof of Helm release execution.

## Config / Secret Review Boundary

Before any future release, review:
1. `application.yml` — default offline baseline intact.
2. `application-prod.example.yml` — template only, no real values.
3. `application-security-api-key.yml` — opt-in only.
4. `application-observability-prometheus.yml` — opt-in only.
5. All sensitive values use environment variable placeholders.
6. No real API keys, database passwords, tokens, or private endpoints committed.
7. Secret source identified: env vars, deployment platform, or future secret manager.
8. Production secret manager still future work.

## Migration Review Boundary

Before any future release with database changes:
1. Flyway is disabled by default (`spring.flyway.enabled: false`).
2. Migration locations are correct for target profile.
3. Schema-only baseline migrations are version-controlled.
4. Demo seed data (`data-mysql.sql`) is NOT deployed as migration.
5. DB schema rollback is NOT automatic — requires human review.
6. Flyway migration rollback is NOT implemented.
7. Destructive migrations (DROP, TRUNCATE, schema change with data loss) require
   separate human approval outside the release pipeline.
8. PGvector / DB live migration validation remains opt-in / future.

## Auth / Security Review Boundary

Before any future release with auth:
1. `security-api-key` profile behavior reviewed.
2. API keys provisioned via environment variables or external secret source.
3. Health probes remain public (required for K8s).
4. Protected endpoints enforce correct role boundary:
   - Ticket/AgentRun: `ADMIN`/`SUPERVISOR`/`AGENT_OPERATOR`/`SYSTEM_SERVICE`
   - Approval approve/reject: `ADMIN`/`SUPERVISOR`
   - OpenAPI/Swagger: `ADMIN`/`SUPERVISOR`
   - Prometheus (opt-in): `ADMIN`/`SYSTEM_SERVICE`
5. Sensitive actuator endpoints remain unexposed.
6. OAuth2 / OIDC, JWT, session login still not implemented.
7. Secret manager still future work.
8. Rate limiting still future work.

## Observability Review Boundary

Before any future release:
1. `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` available.
2. Sensitive actuator endpoints NOT exposed.
3. `observability-prometheus` profile is opt-in only.
4. Correlation IDs present in logs.
5. No secrets in logs, metrics tags, or health details.
6. Production monitoring backend still future work.
7. OpenTelemetry / distributed tracing still future work.

## Release Checklist

Use `docs/deploy/release-templates/RELEASE_CHECKLIST_TEMPLATE.md`.

This is a pre-release review checklist. It does not represent an executed release.

## Post-release Verification Checklist

After any future release, verify:
1. `/actuator/health` returns UP.
2. `/actuator/health/liveness` returns UP.
3. `/actuator/health/readiness` returns UP.
4. Authenticated API smoke check (if `security-api-key` enabled).
5. OpenAPI / Swagger UI accessible under protected profile (if auth enabled).
6. Prometheus endpoint accessible only if explicitly opt-in.
7. Logs contain correlation IDs.
8. No secrets in logs or health details.
9. No exposed sensitive actuator endpoints.
10. RAG default/offline path untouched.
11. Approval / high-risk boundary intact.
12. Rollback command / previous image tag known and documented.

This is a runbook checklist. It does not represent an executed post-release verification.

## Rollback Trigger Matrix

| # | Trigger | Symptom | Detection | Immediate Action | Rollback | Verify After Rollback |
|---|---------|---------|-----------|-----------------|----------|----------------------|
| 1 | Startup failure | Pod CrashLoopBackOff, exit code non-zero | `kubectl get pods`, liveness probe failure | Stop traffic, check logs | Rollback to last known-good image tag | `/actuator/health` UP, pod stable |
| 2 | Readiness failure | Pod not ready, traffic not routed | `kubectl get pods`, readiness probe failure | Stop traffic, check readiness dependencies | Rollback if config/profile root cause | `/actuator/health/readiness` UP |
| 3 | Liveness restart loop | Frequent pod restarts | `kubectl describe pod`, restart count climbing | Check memory/CPU, review recent changes | Rollback if resource or code regression | Restart count stable for 5+ min |
| 4 | Auth rejection spike | HTTP 401/403 rate increase | Auth metrics, application logs | Verify API key config; check Secret values | Rollback if auth config regression | Auth smoke test passes |
| 5 | 5xx spike | HTTP 500/502/503 rate increase | Application logs, error metrics | Check downstream dependencies, review recent changes | Rollback if application regression | 5xx rate returns to baseline |
| 6 | Migration failure | Flyway error on startup | Application logs, startup failure | Stop deployment; DO NOT proceed with partial migration | Rollback to previous version; review migration SQL | Application starts cleanly with previous schema |
| 7 | Configuration error | Wrong profile, wrong endpoint | Logs, health check failures | Verify ConfigMap/Secret/values | Rollback config to last known-good state | Health probes pass |
| 8 | Secret missing | API key or DB password unset | Application startup log error | Verify Secret exists and keys are correct | Rollback or provision missing Secret | Application starts, auth passes |
| 9 | API key misconfiguration | Wrong role mapping, access denied | Auth logs, 403 responses | Verify key-to-role mapping in env/Secret | Rollback to known-good API key config | Role-based access smoke test passes |
| 10 | Degraded RAG / vector path | PGvector search errors, fallback used | RAG health indicator, `fallbackUsed` in trace | Check PGvector connectivity; verify opt-in path intact | Disable opt-in PGvector path if config error; rollback if code regression | Default offline RAG path operational |
| 11 | Actuator exposure leak | Sensitive endpoint accessible | Security scan, manual probe | Verify `management.endpoints.web.exposure.include` | Rollback config immediately | Sensitive endpoints return 404 or are unauthenticated |
| 12 | Performance regression | Increased latency, increased resource usage | Metrics, probe timing | Profile application; check recent changes | Rollback if code regression | Latency and resource usage return to baseline |
| 13 | Severe security finding | CVE, exposed secret, auth bypass | Security advisory, audit | Isolate affected component; escalate | Rollback to unaffected version; apply fix in separate release | Security smoke test passes; vulnerability mitigated |

## Rollback Strategy

### Image Tag Rollback
- Return to the last known-good immutable image tag.
- Update Deployment image tag via `kubectl set image` or Helm `--set image.tag=<tag>`.
- Verify pod restarts with correct image.

### Helm Values Rollback
- Revert `values.yaml` changes to last known-good state.
- Run `helm upgrade` with reverted values (future; Helm upgrade not yet executed).
- Verify ConfigMap and Secret reflect reverted values.

### Config Rollback
- Revert ConfigMap to last known-good content.
- Restart pods to pick up new ConfigMap.
- Verify application behavior.

### Secret Rollback
- If Secret was accidentally deleted or corrupted, re-provision from known-good source.
- If Secret values changed, revert to previous values.
- Restart pods to pick up updated Secret.

### Migration Rollback Caution
- **DB schema rollback is NOT an automatic safe operation.**
- Flyway migration rollback is NOT implemented.
- Destructive migrations (DROP, TRUNCATE, data-loss schema changes) are PROHIBITED
  without separate human review and backup verification.
- PGvector / DB live migration validation remains opt-in / future.
- If migration failure occurs during release: STOP the release, rollback the application
  to the previous version that matches the current schema, then resolve the migration
  issue separately.

### Feature / Profile Rollback
- Disable problematic profile by removing it from `SPRING_PROFILES_ACTIVE`.
- Example: remove `security-api-key` if auth config is broken; remove
  `observability-prometheus` if Prometheus exposes unexpected metrics.
- Restart pods after profile change.

### Emergency Disablement Boundary
- Actuator exposure can be tightened by reverting `management.endpoints.web.exposure.include`
  to `health` only.
- Opt-in profiles (`security-api-key`, `observability-prometheus`) can be disabled
  immediately.
- Ingress can be disabled (`ingress.enabled: false`) to stop external traffic routing.

### When NOT to Rollback
- Isolated, non-critical bug with known workaround.
- Minor cosmetic issue with no user impact.
- Configuration change that can be hot-fixed without restart.
- Secret rotation that is working correctly (do not revert security improvements).

### When to Escalate
- Data corruption or data loss.
- Security breach or exposed credentials.
- Multi-service cascade failure.
- Unknown root cause after initial rollback.
- Migration failure with uncertain schema state.

## Rollback Verification

After any rollback, verify:
1. Health probes pass.
2. Auth smoke test passes (if auth enabled).
3. No secrets in logs.
4. Correlation IDs present.
5. Sensitive actuator endpoints unexposed.
6. RAG default path operational.
7. Approval boundary intact.
8. Metrics return to baseline.
9. No residual errors from rollback procedure.
10. Rollback documented in change record.

Use `docs/deploy/release-templates/ROLLBACK_CHECKLIST_TEMPLATE.md`.

## Change Record Template

Use `docs/deploy/release-templates/CHANGE_RECORD_TEMPLATE.md`.

Every release and every rollback must produce a change record. The template includes:
- Change ID
- Date
- Author
- Type (release / rollback / hotfix / config-change)
- Image tag before / after
- Profiles before / after
- Migration status
- Auth / observability status
- Rollback target
- Verification results
- Known Issues
- Follow-ups

## Release Note Template

Every release note should include:
- Version / tag.
- Date.
- Summary of changes.
- New capabilities.
- Profile / config changes.
- Migration notes (if any).
- Security / auth notes.
- Observability notes.
- Known limitations.
- Rollback instructions (image tag / commit to revert to).

## Incident / Escalation Notes

- All rollbacks must be recorded as change records.
- Escalation path: maintainer → security reviewer → platform admin.
- Secret exposure incidents require immediate secret rotation and separate post-mortem.
- Migration failure incidents require schema state documentation before any recovery action.
- Production incident response runbooks remain future work.

## Non-goals

V5.B.4.4 does NOT implement:
- GitHub release workflow.
- Image registry push.
- Semantic-release or automated version bump.
- Helm install / upgrade execution.
- kubectl apply execution.
- Real Kubernetes cluster deployment.
- Production deployment.
- Production rollback execution.
- Registry credential setup.
- External secret manager.
- Production monitoring backend.
- Alerting rules.
- Canary / blue-green deployment automation.
- Argo CD / Flux.
- Terraform.
- Cloud provider deployment.
- Database migration execution.
- Runtime code changes.
- `src/main/java/**` modifications.

## Follow-ups

- Future: GitHub release workflow.
- Future: image registry push.
- Future: external secret manager integration.
- Future: production monitoring backend.
- Future: alerting rules.
- Future: canary / blue-green deployment.
- Future: Argo CD / Flux evaluation.
- Future: Terraform / IaC evaluation.
- Future: production incident response runbooks.
- Future: Flyway migration rollback strategy.
- Future: image signing and SBOM.

## Validation Commands

Targeted docs harness:

```bash
mvn test -Dtest=ReleaseRollbackFoundationDocsTest
```

Default gate:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional local gates (NOT default):

```bash
docker build -t after-sale-agent-platform:local .
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
kubectl apply --dry-run=client -f deploy/k8s/
```

Default Maven validation does NOT require Docker, Helm, kubectl, Kubernetes, real LLMs,
API keys, PostgreSQL, PGvector, MySQL, Redis, or external network.

## Completion Signal

TASK_COMPLETE
