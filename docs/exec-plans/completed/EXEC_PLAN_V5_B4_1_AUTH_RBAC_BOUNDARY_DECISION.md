# V5.B.4.1 Production Auth / RBAC Boundary Decision

Date: 2026-06-04

Status: Completed

## Goal

Define the production authentication and RBAC boundary before runtime auth implementation. This closes the first
V5.B.4 security step as a documentation and docs-harness task.

## Scope Completed

- Added `docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md`.
- Added `docs/deploy/AUTH_RBAC_BOUNDARY.md`.
- Updated README, OpenAPI docs, deployment roadmap, production config docs, validation commands, quality score,
  remediation plan, and V5 status docs.
- Added `AuthRbacBoundaryDocsTest`.

## What Changed

V5.B.4.1 records the current auth gap, planned role model, API access matrix, actuator boundary, Swagger UI boundary,
approval boundary, ToolRegistry boundary, RAG evidence-only boundary, K8s exposure precondition, and release /
rollback security precondition.

## Current Auth Gap

At V5.B.4.1 time, production auth / RBAC runtime was not implemented. V5.B.4.2 later added the opt-in Spring
Security / API Key Auth Foundation, but full production IAM remains future work. The default backend API surface is
not safe for direct public internet exposure unless an explicit security profile and deployment controls are enabled.
Production K8s / Helm / Ingress exposure must wait for the later deployment hardening tasks.

## Role Model Boundary

The planned roles are `CUSTOMER`, `AGENT_OPERATOR`, `SUPERVISOR`, `ADMIN`, and `SYSTEM_SERVICE`. They are documented
as the future production vocabulary and are not runtime authorities in V5.B.4.1.

## API Access Matrix Boundary

The access matrix covers Ticket, AgentRun, Approval, ToolCallTrace, Execution Tree, health, Swagger UI, Prometheus,
future admin ingestion API, and ToolRegistry direct access. ToolRegistry direct access is never public.

## Actuator / OpenAPI Boundary

Default Actuator exposure remains health-only. Swagger UI remains local / internal API documentation and does not
represent production public API readiness.

## Approval / ToolRegistry Boundary

High-risk actions require Approval. ToolRegistry remains the Agent tool execution entry. `search_aftersale_policy`
remains LOW-risk read-only. V5.B.4.1 does not allow LLMs, planners, Skill code, or external callers to execute tools
directly.

## RAG Evidence-only Boundary

RAG evidence remains policy evidence only. Retrieval score is not business decision confidence and does not execute
refunds, exchanges, coupon compensation, payment changes, logistics changes, or dispute closure.

## K8s Exposure Boundary

Kubernetes / Helm / Ingress exposure remains planned and must wait for runtime auth and production profile policy.

## Release / Rollback Boundary

Release / rollback hardening remains planned. Future release gates should check auth configuration, actuator exposure,
secret injection, migration safety, and rollback notes.

## Runtime Non-change Boundary

V5.B.4.1 does not modify `src/main/java`, `src/main/resources`, `pom.xml`, Dockerfile, CI workflow, migration SQL,
ToolRegistry, AgentApplicationService, RAG runtime, health indicators, metrics recorder, Prometheus config,
correlation filter, OpenAPI runtime config, ToolCallTrace, Workspace, or Execution Tree.

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

## Known Limitations

- At V5.B.4.1 time, Spring Security runtime was not implemented; V5.B.4.2 later added the opt-in API key auth
  foundation.
- JWT / opaque token runtime remains unimplemented; API key foundation was added in V5.B.4.2.
- OAuth2 / OIDC and session login are not implemented.
- K8s / Helm is not implemented.
- Release / rollback automation is not implemented.
- Production deployment is not completed.
- Real refund / exchange / coupon compensation / payment / logistics integrations are not connected.

## Follow-ups

- V5.B.4.2 Spring Security / API Key Auth Foundation completed.
- V5.B.4.3 K8s / Helm Foundation.
- V5.B.4.4 Release / Rollback Foundation.

## Completion Signal

TASK_COMPLETE
