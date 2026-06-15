# K8s / Helm Foundation

Date: 2026-06-15

Status: V5.B.4.3 K8s / Helm Foundation completed

## Goal

V5.B.4.3 adds Kubernetes manifest templates and a Helm chart skeleton for AfterSale-Agent.
It provides safe, reviewable deployment manifests with non-root securityContext,
readiness/liveness probes, ConfigMap/Secret boundary, and profile-aware configuration.

This is a **deployment manifest foundation**, not a production deployment.

## Manifest Path

```
deploy/k8s/
├── README.md
├── deployment.yaml
├── service.yaml
├── configmap.yaml
└── secret.example.yaml
```

## Helm Chart Path

```
deploy/helm/after-sale-agent-platform/
├── Chart.yaml
├── values.yaml
├── README.md
└── templates/
    ├── _helpers.tpl
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    └── NOTES.txt
```

## Image Placeholder Boundary

- Container image uses `REPLACE_WITH_IMAGE_TAG` placeholder.
- No real image registry address is included.
- Image pull policy defaults to `IfNotPresent`.
- No registry credentials are embedded.

## ConfigMap / Secret Boundary

### ConfigMap
- Contains non-sensitive configuration only: log level, RAG health toggle, profile names.
- Does NOT contain API keys, database passwords, tokens, or private endpoints.
- Documents the Actuator exposure boundary and opt-in profile behavior.

### Secret
- `secret.example.yaml` (K8s) uses `stringData` with `REPLACE_WITH_RUNTIME_SECRET` placeholders.
- Helm chart default has `secrets.create: false` and `existingSecret: ""`.
- When `secrets.create` is true, the Secret template uses only values.yaml placeholders.
- Production must use `existingSecret` or external secret injection.
- No real API keys, database passwords, or tokens are included.

## API Key Profile Deployment Note

- `security-api-key` profile is available for auth runtime boundary.
- K8s manifests and Helm chart document how to enable it through `SPRING_PROFILES_ACTIVE` or
  `profiles.securityApiKey.enabled: true`.
- API key env vars are referenced from Secret via `secretKeyRef`.
- The profile is NOT enabled by default in the manifests.

## Prometheus Opt-in Profile Note

- `observability-prometheus` profile is explicit opt-in.
- K8s manifests and Helm chart document the profile name but do NOT enable it by default.
- When enabled, `/actuator/prometheus` is exposed only after explicit opt-in.
- This is not a production monitoring claim.

## Readiness / Liveness Probes

- Readiness probe: `GET /actuator/health/readiness` on port 8080.
- Liveness probe: `GET /actuator/health/liveness` on port 8080.
- Probe timing uses conservative defaults (15s/30s initial delay).
- Health probes remain public under all profiles (required by K8s).

## SecurityContext

- Pod securityContext: `runAsNonRoot: true`, `runAsUser: 1001`, `runAsGroup: 1001`.
- Container securityContext: `runAsNonRoot: true`, `allowPrivilegeEscalation: false`.
- All capabilities dropped (`drop: [ALL]`).
- `readOnlyRootFilesystem` is noted as future hardening; Spring Boot may need `/tmp` writable.

## Service Boundary

- Service type is `ClusterIP` only.
- No Ingress, NodePort, or LoadBalancer.
- Port 8080 mapped as `http`.

## Ingress Boundary

- Ingress is disabled by default in Helm chart (`ingress.enabled: false`).
- K8s raw manifests do not include an Ingress resource.
- Production Ingress exposure is future work.
- Do not expose the current API surface publicly without full IAM hardening.

## Secret Safety

- All Secret values are `REPLACE_WITH_RUNTIME_SECRET` placeholders.
- No base64-encoded real secrets.
- No `.env` content copied into manifests.
- K8s Secret filename includes `.example` suffix.
- Helm Secret template defaults to not creating a Secret.
- `existingSecret` field supports pre-created, externally-managed Secrets.
- Docs state production must use external secret manager, sealed secrets, or cluster
  secret process.

## Local Render / Review Commands (Optional)

These commands are optional and NOT part of the default Maven validation gate:

```bash
# Helm template render
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
```

```bash
# K8s dry-run validation
kubectl apply --dry-run=client -f deploy/k8s/
```

If `helm` or `kubectl` is not installed, skip these commands. Default Maven validation
does NOT require helm, kubectl, Kubernetes, Docker, MySQL, PostgreSQL, PGvector, Redis,
real LLMs, real embedding providers, or external network.

## What Is NOT Completed

- This is NOT a production deployment.
- No `kubectl apply` has been executed.
- No Helm release has been installed.
- No image has been pushed to a registry.
- No GitHub release workflow exists.
- No release / rollback automation exists (planned for V5.B.4.4).
- No external secret manager is integrated.
- No sealed-secrets or ExternalSecrets configuration exists.
- No production Ingress or TLS termination exists.
- No cert-manager or service mesh configuration exists.
- No HPA or NetworkPolicy exists (documented as future only).
- No production database provisioning exists.
- No OAuth2 / OIDC or JWT runtime exists.
- No live PGvector validation is included.
- No real refund / exchange / payment / logistics integrations.

## V5.B Production Hardening Status

V5.B Production Hardening current planned scope completed. See
`docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md` and
`docs/exec-plans/completed/EXEC_PLAN_V5_B_PRODUCTION_HARDENING_COMPLETION.md`.

## Release / Rollback Governance

Release / rollback governance foundation is completed (V5.B.4.4). See
`docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md` for the release checklist,
rollback runbook, image tag policy, and post-release verification checklist.

K8s/Helm templates are deployment foundation only. No real Helm install, kubectl apply,
image push, or rollback has been executed. Image tags for production review must be
immutable (Git SHA or version tag). Ingress and production exposure remain future work.

## Future Secret Manager

External secret manager integration is future work. Current manifests support `existingSecret`
as a pre-created Secret boundary, but automated secret injection, rotation, and audit are
not implemented.

## Future Production Hardening

Future production hardening includes Ingress with TLS, HPA, NetworkPolicy, cert-manager,
service mesh evaluation, production namespace design, node affinity/tolerations, and
production database provisioning.

## Validation Commands

Targeted docs harness:

```bash
mvn test -Dtest=K8sHelmFoundationDocsTest
```

Default gate:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Completion Signal

TASK_COMPLETE
