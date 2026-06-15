# V5.B.4.1 Production Auth / RBAC Boundary Decision

Date: 2026-06-04

Status: Completed

## Context

V5.B.1 到 V5.B.3 已完成 container / CI foundation、配置与迁移边界、profile matrix validation、readiness /
liveness、Micrometer metrics foundation、Prometheus opt-in exposure、local tracing / correlation boundary 和
observability docs closure。V5.B.4.2 后续已补充 opt-in API key auth foundation，但 full production authentication
或 RBAC runtime 仍未完成。

本决策用于在实现 Spring Security 前先固定 API surface、角色模型、权限矩阵和部署前置条件。它是
documentation-first boundary decision，不实现认证、授权、JWT、API key filter、OAuth2 / OIDC、session login、
Kubernetes / Helm、release automation 或 rollback automation。

## Current API Surface

- Ticket create / get / list。
- AgentRun create / get status。
- Approval pending / get / approve / reject。
- ToolCallTrace read-only view。
- Execution Tree read-only view。
- `/api/health`、`/actuator/health`、`/actuator/health/liveness`、`/actuator/health/readiness`。
- `/v3/api-docs` 和 Swagger UI。
- `/actuator/prometheus` 仅在 `observability-prometheus` profile 显式启用时暴露。

## Current Auth Gap

- 当前 V5.B.4.2 已补充 opt-in API key auth foundation，但 full production auth / RBAC runtime 仍未完成。
- 当前 API surface 是 demo/backend API surface，不应直接暴露到 public internet。
- K8s、Ingress、Helm chart 和 production deployment 必须等待 auth boundary 进入 runtime 后再开放外部入口。
- 对接真实退款、换货、支付、物流、订单中心、客服系统或生产 ingestion admin API 前，需要认证、授权、审计、
  rate limiting 和 abuse protection。

## Production Auth Goal

生产认证目标是让不同调用方只能访问其职责内的 API，并让审批、trace、execution tree、admin 和 actuator
surface 有清晰的默认关闭或受限访问策略。认证边界不得让 LLM、Agent、Skill 或外部调用方绕过 ToolRegistry、
RiskPolicy、Approval、ToolCallTrace、Workspace 或 Execution Tree。

## RBAC Role Model

- `CUSTOMER`：终端用户，只能创建和读取自己相关的售后工单状态。
- `AGENT_OPERATOR`：客服 / 运营人员，可查看工单、触发 AgentRun、查看 trace / execution tree。
- `SUPERVISOR`：主管，可执行高风险审批 approve / reject，并查看审批队列。
- `ADMIN`：平台管理员，可管理配置、未来 admin ingestion API 和受限运维视图。
- `SYSTEM_SERVICE`：内部服务账号，用于受控 service-to-service 调用。

## API Access Matrix

| Surface | CUSTOMER | AGENT_OPERATOR | SUPERVISOR | ADMIN | SYSTEM_SERVICE |
| --- | --- | --- | --- | --- | --- |
| Ticket create | allow own ticket | allow | allow | allow | allow if scoped |
| Ticket get / list | own tickets only | allow assigned / scoped | allow scoped | allow scoped | allow if scoped |
| AgentRun create | no by default | allow scoped | allow scoped | allow scoped | allow if scoped |
| AgentRun status | own / scoped | allow scoped | allow scoped | allow scoped | allow if scoped |
| Approval pending / list | no | read scoped if needed | allow | allow | no by default |
| Approval approve / reject | no | no | allow | allow emergency only | no by default |
| Trace read | no raw trace by default | allow scoped | allow scoped | allow scoped | allow if scoped |
| ExecutionTree read | own summary only if productized | allow scoped | allow scoped | allow scoped | allow if scoped |
| Health | public minimal or platform scoped | allow | allow | allow | allow |
| OpenAPI / Swagger UI | disabled or internal only | internal only | internal only | allow internal | no by default |
| Prometheus opt-in endpoint | no | no | no | platform monitoring only | platform monitoring only |
| Admin ingestion future API | no | no | no | allow | allow if scoped |
| ToolRegistry direct access | never public | never public | never public | never public | never public |

## Actuator Access Boundary

Default Actuator exposure remains health-only. Sensitive endpoints such as env、beans、configprops、heapdump、
threaddump and default metrics views must not be broadly exposed. `/actuator/prometheus` remains explicit opt-in and
must be protected by platform network policy or future auth when used outside local review.

## OpenAPI / Swagger UI Boundary

OpenAPI and Swagger UI document existing backend APIs. They are useful for local review and interview demos, but they
are not a production public developer portal. Production deployments should disable or restrict Swagger UI unless a
future authenticated internal docs route is explicitly implemented.

## Approval Boundary

High-risk actions require Approval. Approving a request in the current project records the decision boundary; it does
not execute real refunds, exchanges, payments, logistics changes, coupon compensation, inventory mutations, or dispute
closure.

## ToolRegistry / High-risk Action Boundary

ToolRegistry is the Agent tool execution entry and is not a public API. LLMs and planners may produce structured plans,
but they must not execute tools directly. `search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool.
Future auth must protect API callers without weakening ToolRegistry, RiskPolicy, Approval, ToolCallTrace, Workspace, or
Execution Tree boundaries.

## RAG Evidence-only Boundary

RAG evidence is policy evidence only. Retrieval score is not business decision confidence. RAG evidence must not become
automatic refund, exchange, payment, logistics, coupon compensation, inventory mutation, or dispute-closing execution.

## K8s Exposure Precondition

Kubernetes / Helm / Ingress exposure should wait for a later scoped task to add deployment manifests and a clear
production profile policy. V5.B.4.2 introduces opt-in API key auth foundation, but it is not full production IAM.
Without explicit auth, the current API surface must stay behind local, internal, or development network boundaries.

## Release / Rollback Security Precondition

Release and rollback automation should not publish unauthenticated production APIs. Future release gates should include
auth configuration checks, actuator exposure checks, secret injection checks, migration safety checks, and rollback
notes for auth-related configuration.

## Candidate Implementation Options

- Spring Security + JWT：适合无状态 API 和清晰角色声明，但需要 token issuance / verification strategy。
- Spring Security + opaque token / API key：实现较小，适合内部服务或 demo hardening，但轮换、撤销和审计需要设计。
- Session login：适合后台管理 UI，但当前项目没有生产 UI。
- Gateway-level auth：适合平台网关统一管控，但应用内仍需要 method / endpoint authorization。
- Service-to-service token：适合内部调用，需要 scope、audience 和 rotation boundary。
- Future OAuth2 / OIDC：适合接企业 IdP，但应作为后续生产集成任务。

## Decision

- V5.B.4.2 已引入 Spring Security / API Key Auth Foundation。
- local / default profile may remain test-friendly，默认验证继续离线。
- production profile should require auth before public exposure。
- 最小 runtime 选择在 V5.B.4.2 决定为 API key foundation 并已实现。
- K8s / Helm / Ingress exposure waits for runtime auth boundary。
- V5.B.4.1 不实现 auth runtime，只固定 RBAC 边界和后续实现方向；V5.B.4.2 后续实现 opt-in API key auth
  foundation。

## Non-goals

- V5.B.4.1 本身不实现 Spring Security runtime。
- V5.B.4.1 本身不实现 JWT、API key filter、OAuth2 / OIDC、session login 或 gateway auth；V5.B.4.2 后续实现
  opt-in API key filter。
- 不新增 K8s / Helm。
- 不新增 release / rollback automation。
- 不修改 ToolRegistry、AgentApplicationService、RAG runtime、health indicators、OpenAPI config、ToolCallTrace、
  Workspace 或 Execution Tree。
- 不接真实退款、换货、优惠券补偿、支付、物流、订单中心或争议关闭系统。

## Follow-ups

- V5.B.4.2：Spring Security / API Key Auth Foundation completed。
- V5.B.4.3：K8s / Helm Foundation，必须以前置 auth boundary 为条件。
- V5.B.4.4：Release / Rollback Foundation，必须包含 auth / actuator exposure / secret checks。

## Completion Signal

TASK_COMPLETE
