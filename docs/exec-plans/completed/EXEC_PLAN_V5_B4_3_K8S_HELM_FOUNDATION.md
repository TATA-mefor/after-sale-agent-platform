# V5.B.4.3 K8s / Helm Foundation

Date: 2026-06-15

Status: Completed

## Goal

V5.B.4.3 adds Kubernetes manifest templates and a Helm chart skeleton for AfterSale-Agent,
providing safe, reviewable deployment manifests with non-root securityContext,
readiness/liveness probes, ConfigMap/Secret boundary, and profile-aware configuration.

## Scope Completed

- K8s manifest foundation: `deploy/k8s/` with README, deployment, service, configmap, and
  secret example.
- Helm chart skeleton: `deploy/helm/after-sale-agent-platform/` with Chart.yaml, values.yaml,
  templates, helpers, and NOTES.txt.
- Non-root container securityContext with capability drop.
- Readiness / liveness probe wiring to Spring Boot Actuator health groups.
- ConfigMap for non-sensitive configuration only.
- Secret placeholder boundary with `REPLACE_WITH_RUNTIME_SECRET`.
- API key auth profile and Prometheus opt-in profile deployment configuration.
- New `docs/deploy/K8S_HELM_FOUNDATION.md` documentation.
- Updated deployment roadmap, README, production config docs, auth docs, observability docs,
  validation docs, quality score, remediation plan, and correction plan.
- New `K8sHelmFoundationDocsTest` docs harness test.
- Updated existing docs harness tests for V5.B.4.3 state wording.

## What Changed

### New Files
- `deploy/k8s/README.md`
- `deploy/k8s/deployment.yaml`
- `deploy/k8s/service.yaml`
- `deploy/k8s/configmap.yaml`
- `deploy/k8s/secret.example.yaml`
- `deploy/helm/after-sale-agent-platform/Chart.yaml`
- `deploy/helm/after-sale-agent-platform/values.yaml`
- `deploy/helm/after-sale-agent-platform/README.md`
- `deploy/helm/after-sale-agent-platform/templates/_helpers.tpl`
- `deploy/helm/after-sale-agent-platform/templates/deployment.yaml`
- `deploy/helm/after-sale-agent-platform/templates/service.yaml`
- `deploy/helm/after-sale-agent-platform/templates/configmap.yaml`
- `deploy/helm/after-sale-agent-platform/templates/secret.yaml`
- `deploy/helm/after-sale-agent-platform/templates/NOTES.txt`
- `docs/deploy/K8S_HELM_FOUNDATION.md`
- `docs/exec-plans/completed/EXEC_PLAN_V5_B4_3_K8S_HELM_FOUNDATION.md`
- `src/test/java/io/github/tatame/aftersale/docs/K8sHelmFoundationDocsTest.java`

### Updated Files
- `README.md` — V5.B.4.3 completed, V5.B.4.4 planned, links to K8s/Helm docs.
- `docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md` — V5.B.4.3 completed.
- `docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md` — K8s/Helm foundation section.
- `docs/deploy/AUTH_RUNTIME_FOUNDATION.md` — K8s/Helm cross-reference.
- `docs/deploy/AUTH_RBAC_BOUNDARY.md` — K8s/Helm cross-reference.
- `docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md` — K8s/Helm cross-reference.
- `docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md` — K8s/Helm cross-reference.
- `docs/quality/VALIDATION_COMMANDS.md` — V5.B.4.3 validation section.
- `docs/quality/QUALITY_SCORE.md` — V5.B.4.3 status entry.
- `docs/quality/PROJECT_REMEDIATION_PLAN.md` — V5.B.4.3 completed.
- `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_PLAN.md` — V5.B.4.3 completed.
- `src/test/java/io/github/tatame/aftersale/docs/ApiKeyAuthFoundationDocsTest.java` — wording update.
- `src/test/java/io/github/tatame/aftersale/docs/AuthRbacBoundaryDocsTest.java` — wording update.
- `src/test/java/io/github/tatame/aftersale/docs/DeploymentHardeningRoadmapDocsTest.java` — wording update.
- `src/test/java/io/github/tatame/aftersale/docs/ObservabilityDocsCompletionDocsTest.java` — wording update.

## Kubernetes Manifest Boundary

- `deployment.yaml`: non-root securityContext, readiness/liveness probes on `/actuator/health/readiness`
  and `/actuator/health/liveness`, safe env placeholders with `secretKeyRef` and `configMapKeyRef`,
  resource requests/limits placeholders.
- `service.yaml`: ClusterIP only, port 8080.
- `configmap.yaml`: non-sensitive config only (log level, RAG health toggle, profile names).
- `secret.example.yaml`: `stringData` with `REPLACE_WITH_RUNTIME_SECRET` placeholders only.
- No Ingress, HPA, NetworkPolicy, PVC, or production namespace.
- No real API keys, passwords, tokens, private endpoints, or local absolute paths.

## Helm Chart Boundary

- Chart name: `after-sale-agent-platform`, type: `application`.
- appVersion aligned with `pom.xml`.
- No external chart dependencies.
- `values.yaml`: safe placeholders for image, resources, profiles, secrets.
- `ingress.enabled: false`, `autoscaling.enabled: false`, `serviceAccount.create: false`.
- `secrets.create: false` by default; `existingSecret` supported.
- Profile composition helper for `security-api-key`, `observability-prometheus`, and `prod`.
- Secret template renders only when explicitly opted in.
- NOTES.txt includes security warnings.

## Image Boundary

- Image repository: placeholder `after-sale-agent-platform`.
- Image tag: `REPLACE_WITH_IMAGE_TAG`.
- No real registry address.
- No registry credentials.
- No image push or release workflow.

## ConfigMap / Secret Boundary

- ConfigMap: non-sensitive values only. No API keys, passwords, tokens, private endpoints.
- Secret: `stringData` placeholders only. No base64-encoded real secrets.
- K8s Secret file has `.example` suffix.
- Helm Secret template defaults to not creating a Secret.
- `existingSecret` supports pre-created, externally-managed Secrets.
- Production must use external secret injection.

## Security Profile Boundary

- `security-api-key` profile is documented in K8s env vars and Helm values.
- API key env vars are referenced from Secret via `secretKeyRef` with `optional: true`.
- Profile is NOT enabled by default in manifests.
- OAuth2 / OIDC, JWT, session login, user database remain future work.

## Observability Profile Boundary

- `observability-prometheus` profile is documented in K8s env vars and Helm values.
- Profile is NOT enabled by default in manifests.
- `/actuator/prometheus` is only exposed with explicit opt-in.
- Production monitoring backend, Grafana, alerting remain future work.

## Readiness / Liveness Probe Boundary

- Readiness: `GET /actuator/health/readiness` on port 8080.
- Liveness: `GET /actuator/health/liveness` on port 8080.
- Conservative initial delays: 15s readiness, 30s liveness.
- Health probes remain public under all profiles.

## Container Security Context Boundary

- `runAsNonRoot: true`.
- `allowPrivilegeEscalation: false`.
- `capabilities.drop: [ALL]`.
- `readOnlyRootFilesystem` noted as future hardening.

## Ingress / External Exposure Boundary

- Ingress disabled by default.
- No Ingress resource in raw K8s manifests.
- Production Ingress, TLS, cert-manager remain future work.
- Swagger UI is not exposed publicly.

## Release / Rollback Boundary

- No release / rollback automation implemented.
- No GitHub release workflow.
- No image registry push.
- V5.B.4.4 Release / Rollback Foundation planned.

## Runtime Non-change Boundary

V5.B.4.3 does NOT modify:
- `src/main/java/**`
- `src/main/resources/**`
- `pom.xml`
- `Dockerfile`
- `.github/workflows/ci.yml`
- Migration SQL
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
- Prometheus server, Grafana, OpenTelemetry collector
- External network

## Validation Commands

```bash
mvn test -Dtest=K8sHelmFoundationDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional (not default gate):

```bash
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
kubectl apply --dry-run=client -f deploy/k8s/
```

## Known Limitations

- No real Kubernetes deployment has been executed.
- No Helm release has been installed.
- No image has been pushed to a registry.
- No external secret manager integration.
- No sealed-secrets or ExternalSecrets.
- No production Ingress with TLS.
- No cert-manager or service mesh.
- No HPA or NetworkPolicy.
- No production database provisioning.
- No OAuth2 / OIDC runtime.
- No release / rollback automation.
- No live PGvector validation in default gate.

## Follow-ups

- V5.B.4.4: Release / Rollback Foundation.
- Future: external secret manager integration.
- Future: production Ingress with TLS.
- Future: HPA, NetworkPolicy, production namespace design.
- Future: cert-manager, service mesh evaluation.
- Future: production database provisioning.
- Future: OAuth2 / OIDC.

## Completion Signal

TASK_COMPLETE
