# EXEC_PLAN_V5_B4_2_SPRING_SECURITY_API_KEY_AUTH_FOUNDATION

Date: 2026-06-04

Status: Completed

## Goal

Introduce the smallest verifiable Spring Security auth foundation after the V5.B.4.1 Auth / RBAC boundary decision,
while preserving default local/test permit-all behavior and default offline validation.

## Scope Completed

- Added Spring Security dependency managed by Spring Boot.
- Added `agent.security` configuration properties.
- Added `security-api-key` profile.
- Added API key credential validation with role mapping.
- Added stateless Spring Security filter chain and API key filter.
- Protected Ticket, AgentRun, Approval, Trace, ExecutionTree, OpenAPI / Swagger UI and opt-in Prometheus under the
  security profile.
- Kept health endpoints public.
- Added security runtime tests and docs harness coverage.
- Updated deployment, validation, quality and README status docs.

## What Changed

V5.B.4.2 adds an opt-in API key auth foundation. It does not change business Controller paths, request/response DTOs,
ToolRegistry execution semantics, AgentRun state machine, Approval state machine, RAG retrieval, ToolCallTrace schema,
Workspace logic or Execution Tree runtime.

## Spring Security Boundary

Spring Security is introduced only as an HTTP protection boundary. Default profile remains permit-all. The security
package is separated under `common.security` and is covered by architecture rules so it does not depend on business
repositories, ToolRegistry, RAG runtime, DataSource, JdbcTemplate, Spring AI concrete classes or PGvector classes.

## API Key Auth Boundary

API key auth is enabled by `security-api-key` or `agent.security.enabled=true`。The default header is `X-API-Key`。
Configured keys map to `ADMIN`, `SUPERVISOR`, `AGENT_OPERATOR` and `SYSTEM_SERVICE`。Missing configured keys fail fast
when auth is enabled. Raw keys are not echoed in responses, logs, MDC or metrics tags.

## Default Local/Test Boundary

Default profile sets `agent.security.enabled=false` and does not require API key environment variables. Existing local
demo APIs and default tests keep permit-all behavior.

## Security Profile Boundary

`application-security-api-key.yml` activates only on `security-api-key`。It uses placeholders for:

```text
AFTERSALE_SECURITY_ADMIN_API_KEY
AFTERSALE_SECURITY_SUPERVISOR_API_KEY
AFTERSALE_SECURITY_OPERATOR_API_KEY
AFTERSALE_SECURITY_SYSTEM_SERVICE_API_KEY
```

No real secret values are stored in the repository.

## Role Mapping Boundary

Runtime role mapping supports `ADMIN`, `SUPERVISOR`, `AGENT_OPERATOR` and `SYSTEM_SERVICE`。`CUSTOMER` remains a
future runtime path from the V5.B.4.1 role model.

## Endpoint Protection Boundary

- Health probes are public.
- Ticket and AgentRun APIs require a valid application role.
- Approval approve / reject requires `ADMIN` or `SUPERVISOR`。
- Trace and ExecutionTree read APIs allow `ADMIN`, `SUPERVISOR` and `AGENT_OPERATOR`。
- OpenAPI / Swagger UI requires `ADMIN` or `SUPERVISOR`。
- Prometheus, when exposed by opt-in profile, requires `ADMIN` or `SYSTEM_SERVICE`。
- Sensitive actuator endpoints remain unexposed.

## Actuator / OpenAPI Boundary

`/actuator/health`, `/actuator/health/liveness` and `/actuator/health/readiness` remain public. `/v3/api-docs` and
Swagger UI are protected under `security-api-key` and remain documentation for existing APIs only.

## Prometheus Boundary

Prometheus remains opt-in through `observability-prometheus` and is protected by API key roles when combined with
`security-api-key`。The default profile does not expose `/actuator/prometheus`。

## ToolRegistry / Approval Boundary

ToolRegistry remains internal. High-risk actions remain Approval-gated. This task does not add a public ToolRegistry
API and does not change Approval business transitions.

## Secret Safety Boundary

All examples use placeholders. No database password, API key, token, secret, local absolute path or raw dataset path is
committed.

## OAuth2 / OIDC Boundary

OAuth2 / OIDC, JWT issuer / JWKS, resource-server validation, session login, password login, user database and tenant
isolation are not implemented.

## K8s / Release Boundary

Kubernetes / Helm remains V5.B.4.3 planned. Release / rollback automation remains V5.B.4.4 planned. Production
deployment is not completed.

## Runtime Behavior Boundary

This stage changes only HTTP authentication/authorization behavior under an explicit security profile. Business runtime
semantics are unchanged.

## Default Offline Boundary

Default validation does not need real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider, Spring AI `VectorStore` or external network.

## Validation Commands

```bash
mvn test -Dtest=SecurityDefaultBoundaryTest
mvn test -Dtest=ApiKeyCredentialValidatorTest
mvn test -Dtest=ApiKeyAuthBoundaryTest
mvn test -Dtest=ApiKeyAuthFoundationDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Full production auth / IAM is not completed.
- OAuth2 / OIDC and JWT are not implemented.
- User database, tenant isolation, secret manager and rate limiting are not implemented.
- Kubernetes / Helm and release / rollback are not implemented.
- Real refund, exchange, compensation, payment and logistics integrations are not connected.

## Follow-ups

- V5.B.4.3 Kubernetes / Helm Foundation.
- V5.B.4.4 Release / Rollback Foundation.
- Future production IAM, secret manager, rate limiting, audit hardening and tenant isolation.

## Completion Signal

TASK_COMPLETE
