# V5.B.4.2 Spring Security / API Key Auth Foundation

Date: 2026-06-04

Status: Completed

## 目标

V5.B.4.2 在 V5.B.4.1 Production Auth / RBAC Boundary Decision 的基础上，引入最小可验证的 Spring Security
runtime auth foundation。当前实现选择显式 opt-in 的 API key 认证，用于在 `security-api-key` profile 下保护
Ticket、AgentRun、Approval、Trace、ExecutionTree、OpenAPI / Swagger UI 和 opt-in Prometheus endpoint。

本阶段不是完整 production IAM。OAuth2 / OIDC、JWT issuer / JWKS、session login、user database、secret
manager、tenant isolation、Kubernetes / Helm、release / rollback automation 仍是 future work。

## 为什么选择 API Key Foundation

- API key foundation 足够小，可以机械化验证 endpoint protection 和角色边界。
- 默认 profile 仍保持 permit-all，避免破坏本地 demo、默认测试和离线验证。
- API key 只通过环境变量或外部配置注入，仓库不保存真实 secret。
- 后续可以在不改变 Controller 业务语义的前提下替换为 OAuth2 / OIDC 或 JWT resource server。

## Profile

启用方式：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=security-api-key
```

`security-api-key` profile 默认设置 `agent.security.enabled=true`。默认 profile 中
`agent.security.enabled=false`，不会要求 API key。

## 环境变量

当前 API key header：

```text
AFTERSALE_SECURITY_API_KEY_HEADER
```

默认 header 名称：

```text
X-API-Key
```

角色 key 通过以下环境变量或等价外部配置注入：

```text
AFTERSALE_SECURITY_ADMIN_API_KEY
AFTERSALE_SECURITY_SUPERVISOR_API_KEY
AFTERSALE_SECURITY_OPERATOR_API_KEY
AFTERSALE_SECURITY_SYSTEM_SERVICE_API_KEY
```

这些值必须使用占位符或部署系统注入。不要把真实 API key、password、secret 或 raw token 写入仓库。

## Roles

当前 runtime key mapping 支持：

- `ADMIN`
- `SUPERVISOR`
- `AGENT_OPERATOR`
- `SYSTEM_SERVICE`

V5.B.4.1 中记录的 `CUSTOMER` 角色仍保留为 future runtime path，本阶段不实现 customer key。

## Endpoint Protection Matrix

| Endpoint | `security-api-key` profile boundary |
| --- | --- |
| `/actuator/health` | Public |
| `/actuator/health/liveness` | Public |
| `/actuator/health/readiness` | Public |
| `/api/tickets/**` | `ADMIN` / `SUPERVISOR` / `AGENT_OPERATOR` / `SYSTEM_SERVICE` |
| `/api/agent-runs/**` | `ADMIN` / `SUPERVISOR` / `AGENT_OPERATOR` / `SYSTEM_SERVICE` |
| Approval pending / get | `ADMIN` / `SUPERVISOR` / `AGENT_OPERATOR` |
| Approval approve / reject | `ADMIN` / `SUPERVISOR` |
| ToolCallTrace read | `ADMIN` / `SUPERVISOR` / `AGENT_OPERATOR` |
| ExecutionTree read | `ADMIN` / `SUPERVISOR` / `AGENT_OPERATOR` |
| `/v3/api-docs` / Swagger UI | `ADMIN` / `SUPERVISOR` |
| `/actuator/prometheus` when exposed | `ADMIN` / `SYSTEM_SERVICE` |
| Other requests | Authenticated |

Missing or invalid API key returns `401`。Authenticated but insufficient role returns `403`。Responses do not echo the
raw API key.

## Health Boundary

Health endpoints remain public under the security profile so local probes and deployment probes can check process
health without credentials. Sensitive actuator endpoints such as `env`, `beans`, `configprops`, `heapdump` and
`threaddump` remain unexposed.

## OpenAPI / Swagger Boundary

OpenAPI docs remain existing API documentation only. Under `security-api-key`, `/v3/api-docs` and Swagger UI require
`ADMIN` or `SUPERVISOR` API key. This does not create new public RAG endpoints and does not change Controller paths.

## Prometheus Boundary

Prometheus remains opt-in through `observability-prometheus`。When that profile is combined with `security-api-key`,
`/actuator/prometheus` requires `ADMIN` or `SYSTEM_SERVICE`。The default profile still does not expose Prometheus.

## ToolRegistry / Approval Boundary

ToolRegistry remains internal Agent tool execution boundary and is not a public HTTP API. High-risk actions remain
Approval-gated. This stage does not change Approval state machine semantics.

## RAG Evidence-only Boundary

`search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool. RAG evidence is policy evidence only; it is
not a business decision and does not execute refund, exchange, compensation, payment or logistics actions.

## Default Local / Test Behavior

Default local and test profiles remain permit-all and offline. Default validation does not require real LLM, API Key,
PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding provider, Spring AI live model, Spring AI `VectorStore` or
external network.

## Secret Safety

- API keys are read from environment variables or external configuration placeholders.
- API keys are not written to logs, responses, MDC or metrics tags.
- Invalid-key responses are generic.
- This document contains no real secret values.

## Known Limitations

- Full production auth / IAM is not completed.
- OAuth2 / OIDC is not implemented.
- JWT issuer / JWKS is not implemented.
- Session login and password login are not implemented.
- User database and tenant isolation are not implemented.
- Secret manager integration is not implemented.
- Rate limiting and abuse protection are not implemented.
- Kubernetes / Helm and release / rollback automation remain planned.

## Follow-ups

- V5.B.4.3: Kubernetes / Helm Foundation (completed; see `docs/deploy/K8S_HELM_FOUNDATION.md`).
  K8s manifests use `security-api-key` profile placeholder.
- V5.B.4.4: Release / Rollback Foundation (completed; see `docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md`).
  Release review confirms API keys are provisioned via env/secret source; release automation remains future.
- Future: OAuth2 / OIDC or JWT resource server, secret manager, rate limit, audit hardening and tenant isolation.

## Completion Signal

TASK_COMPLETE
