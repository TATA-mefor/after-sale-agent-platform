# after-sale-agent-platform Helm Chart

Date: 2026-06-15

Status: V5.B.4.3 K8s / Helm Foundation completed (Helm chart skeleton only)

## Purpose

This is a Helm chart skeleton for AfterSale-Agent. It provides a safe, reviewable template
for Kubernetes deployment, but it is **NOT a production deployment**.

## What This Chart Provides

- Deployment with non-root securityContext, readiness/liveness probes, and resource placeholders.
- ClusterIP Service (no Ingress, no LoadBalancer by default).
- ConfigMap for non-sensitive configuration only.
- Secret template with placeholder values (disabled by default; prefer `existingSecret`).
- Profile composition helper for `security-api-key`, `observability-prometheus`, and `prod`.
- Safe defaults: Ingress disabled, autoscaling disabled, service account creation disabled.

## What This Chart Does NOT Provide

- Real secrets — all values are `REPLACE_WITH_RUNTIME_SECRET` placeholders.
- Real registry credentials.
- Ingress exposure (disabled by default).
- HPA configuration (disabled by default).
- NetworkPolicy.
- PVC or database StatefulSet.
- Migration Job.
- OAuth2 / OIDC configuration.
- Cert-manager integration.
- Service mesh configuration.
- Production namespace or node assignments.

## Local Render (Optional)

```bash
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
```

This command is optional and NOT part of the default Maven validation gate.
If `helm` is not installed, skip it.

## Secret Safety

- `secrets.create` is `false` by default.
- When `secrets.create` is false and `existingSecret` is empty, the deployment references
  a Secret that is expected to be created externally.
- Production deployments MUST use `existingSecret` with a pre-created Secret managed by
  external secret manager, sealed secrets, or cluster secret process.
- The chart's Secret template contains ONLY placeholder values from `values.yaml`.

## Profile Configuration

Set profile flags in `values.yaml`:

```yaml
profiles:
  prod:
    enabled: true
  securityApiKey:
    enabled: true    # Enable API key auth
  prometheus:
    enabled: false   # Keep Prometheus opt-in
```

- `security-api-key` profile enables stateless API key auth with `X-API-Key` header.
- `observability-prometheus` profile exposes `/actuator/prometheus` (opt-in only).
- Both profiles are NOT enabled by default in the Helm chart.

## Readiness / Liveness Probes

- Readiness: `GET /actuator/health/readiness`
- Liveness: `GET /actuator/health/liveness`
- Health probes are public under all profiles (required by K8s).

## Container Security Context

- `runAsNonRoot: true`
- `allowPrivilegeEscalation: false`
- All capabilities dropped.
- `readOnlyRootFilesystem` is noted as future hardening; Spring Boot may need `/tmp` writable.

## Release / Rollback

V5.B.4.4 Release / Rollback Foundation completed. See
`docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md` for release checklist, rollback runbook,
image tag policy, and Helm release review policy. Image tags for production review must
be immutable. This Helm chart is deployment foundation only — no real `helm install` or
rollback has been executed.

## Follow-ups

- Future secret manager integration.
- Future production Ingress with TLS.
- Future HPA and NetworkPolicy.
- Future cert-manager and service mesh evaluation.
- Future GitHub release workflow and registry push.
