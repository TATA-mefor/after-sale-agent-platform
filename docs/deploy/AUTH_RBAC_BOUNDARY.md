# Production Auth / RBAC Boundary

Date: 2026-06-04

Status: Completed for V5.B.4.1 boundary decision; V5.B.4.2 API key auth foundation completed; full production IAM
remains planned.

## Purpose

This document explains the production authentication and RBAC boundary for the current AfterSale-Agent backend. It is
the deployment-facing companion to `docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md`.

V5.B.4.1 is documentation-only. It does not add Spring Security, JWT, API key runtime, OAuth2 / OIDC, session login,
Kubernetes / Helm, release automation, rollback automation, secret manager integration, or production deployment.
V5.B.4.2 later adds the opt-in Spring Security / API Key Auth Foundation; full production IAM remains future work.

## Current Auth Gap

The current API is a demo/backend API surface. Without explicitly enabling `security-api-key`, it should not be
directly exposed to the public internet.

Current gaps:

- no full production authentication runtime;
- no production RBAC enforcement;
- no production rate limit / abuse protection;
- no production trace access control;
- no production admin ingestion API;
- no production external refund / exchange / payment / logistics integration.

## Role Model

- `CUSTOMER`
- `AGENT_OPERATOR`
- `SUPERVISOR`
- `ADMIN`
- `SYSTEM_SERVICE`

These roles are the planned production authorization vocabulary. V5.B.4.2 implements runtime authorities for
`ADMIN`, `SUPERVISOR`, `AGENT_OPERATOR`, and `SYSTEM_SERVICE`; `CUSTOMER` remains future runtime work.

## API Access Matrix Boundary

| API surface | Boundary |
| --- | --- |
| Ticket create | Customer own ticket or scoped operator access in future auth runtime. |
| Ticket get / list | Own or scoped access; not public broad listing. |
| AgentRun create | Operator / supervisor / admin / scoped service access only. |
| AgentRun status | Own or scoped read access. |
| Approval pending / list | Supervisor / admin surface; not customer public surface. |
| Approval approve / reject | Supervisor / admin only; high-risk decisions remain approval-gated. |
| Trace read | Internal scoped operator / supervisor / admin access only. |
| ExecutionTree read | Read-only scoped explanation view; not raw public trace. |
| Health | Minimal health can remain platform-readable. |
| OpenAPI / Swagger UI | Local or internal docs only; not a public production portal. |
| Prometheus opt-in endpoint | Platform monitoring only when explicitly enabled and protected. |
| Admin ingestion future API | Future admin-only surface. |
| ToolRegistry direct access | Never public. ToolRegistry remains internal Agent execution boundary. |

## Actuator Boundary

Default exposure remains health-only. Sensitive Actuator endpoints must not be broadly exposed. Prometheus remains
explicit opt-in and should be protected by platform network policy or future auth when used beyond local review.

## OpenAPI / Swagger UI Boundary

Swagger UI helps review existing APIs. It does not prove production auth, production deployment, public API readiness,
or a new public RAG endpoint. Under `security-api-key`, Swagger UI and `/v3/api-docs` require `ADMIN` or `SUPERVISOR`.
Production deployments still need full IAM hardening before public exposure.

## Approval / ToolRegistry Boundary

High-risk actions require Approval. ToolRegistry is not a public API. LLMs, planners, Skill code, and external callers
must not execute tools directly or bypass RiskPolicy. `search_aftersale_policy` remains LOW-risk read-only.

## RAG Evidence-only Boundary

RAG evidence is policy evidence only. It is not a business decision and not a business action. Evidence score is a
retrieval score, not confidence that a refund, exchange, coupon compensation, payment change, logistics change, or
dispute closure should happen.

## K8s Exposure Boundary

K8s / Helm / Ingress exposure remains planned. Do not expose the current API surface publicly through K8s or an
ingress route until deployment manifests and production IAM hardening are implemented and validated.

## Release / Rollback Boundary

Release / rollback hardening remains planned. Future release gates should include auth configuration checks, actuator
exposure checks, secret injection checks, migration checks, and rollback notes.

## Default Offline Boundary

Default validation remains offline and deterministic. It does not require real LLM, API Key, PostgreSQL, PGvector,
Docker, MySQL, Redis, real embedding provider, Spring AI live provider calls, Spring AI `VectorStore`, secret manager,
Kubernetes / Helm, Prometheus server, Grafana, OpenTelemetry collector, or external network.

## Validation Commands

```bash
mvn test -Dtest=AuthRbacBoundaryDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Follow-ups

- V5.B.4.2 Spring Security / API Key Auth Foundation completed.
- V5.B.4.3 K8s / Helm Foundation completed (see `docs/deploy/K8S_HELM_FOUNDATION.md`).
  K8s manifests use `security-api-key` profile placeholder; OpenAPI/Swagger should not be
  directly exposed to public network.
- V5.B.4.4 Release / Rollback Foundation completed (see `docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md`).
  Release review confirms API key provisioning and role boundary before any future release.
- V5.B Production Hardening current planned scope completed. See
  `docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md`.

## Completion Signal

TASK_COMPLETE
