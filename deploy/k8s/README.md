# K8s Manifests

Date: 2026-06-15

Status: V5.B.4.3 K8s / Helm Foundation completed (manifest skeleton only)

## Purpose

This directory contains Kubernetes manifest templates for AfterSale-Agent. These are
**safe placeholder manifests**, not a production deployment.

They are designed for local review (`kubectl apply --dry-run=client`) and Helm template
review. They do not represent a live Kubernetes deployment and do not contain real secrets.

## Files

- `deployment.yaml` — Deployment with non-root securityContext, readiness/liveness probes,
  safe env placeholder references, and resource requests/limits placeholders.
- `service.yaml` — ClusterIP Service only. No Ingress, no NodePort, no LoadBalancer.
- `configmap.yaml` — Non-sensitive configuration only. No API keys, passwords, tokens,
  or private endpoints.
- `secret.example.yaml` — Placeholder Secret using `stringData`. Must not be used as-is
  in production. All values are `REPLACE_WITH_*` placeholders.

## What These Manifests Are

- Safe Kubernetes manifest templates for review.
- A foundation for future K8s deployment design.
- Readiness / liveness probe wiring aligned with Spring Boot Actuator health groups.
- Non-root container securityContext with capability drop.
- A ConfigMap for non-sensitive profile and log configuration.
- A Secret example showing the expected key names only.

## What These Manifests Are NOT

- These are NOT a production deployment.
- They do NOT contain real API keys, database passwords, tokens, private endpoints,
  or local absolute paths.
- They do NOT represent a running cluster state.
- They do NOT include Ingress, HPA, NetworkPolicy, PVC, or production namespace.
- They do NOT include Helm release automation, image registry push, or CD.
- They do NOT include a real Secret; only `secret.example.yaml` with placeholders.

## Local Review Commands (Optional)

These commands are optional. They are NOT part of the default Maven validation gate.
If `helm` or `kubectl` is not installed, skip them.

```bash
# Dry-run Kubernetes manifests
kubectl apply --dry-run=client -f deploy/k8s/

# Helm template rendering
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
```

Default Maven validation does NOT require `kubectl`, `helm`, Kubernetes, Docker,
MySQL, PostgreSQL, PGvector, Redis, real LLMs, real embedding providers, or external network.

## Secret Safety

`secret.example.yaml` contains ONLY placeholder values. It must NEVER be renamed to
`secret.yaml` and applied to a real cluster without external secret injection.

Real secrets must come from:
- External secret manager (future).
- Sealed Secrets (future).
- Cluster secret process (future).
- Deployment platform environment injection.

## Profile Notes

- `security-api-key` profile is available for auth runtime boundary. K8s manifests
  document how to enable it through `SPRING_PROFILES_ACTIVE`.
- `observability-prometheus` profile is explicit opt-in. It is NOT enabled by default
  in these manifests.

## Release / Rollback

V5.B.4.4 Release / Rollback Foundation completed. See
`docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md` for release checklist, rollback runbook,
image tag policy, and post-release verification. Image tags for production review must
be immutable. These K8s manifests are deployment foundation only — no real `kubectl apply`
or Helm install has been executed.

## Follow-ups

- Future secret manager integration.
- Future production Ingress with TLS.
- Future HPA, NetworkPolicy, and production namespace design.
- Future cert-manager and service mesh evaluation.
- Future production database provisioning.
- Future OAuth2 / OIDC.
- Future GitHub release workflow and registry push.
