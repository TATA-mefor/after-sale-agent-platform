# 项目审查异步 / 流式 / 批量 API 评估决策

Date: 2026-06-01

Status: Completed

## Context

项目审查指出当前 API 缺少异步 AgentRun、SSE / WebSocket 流式输出、批量 API、取消 / 重试和 AgentRun 列表分页。
阶段 3.2 已补 Ticket list/query pagination，阶段 3.3 已补 `GET /api/agent-runs/{runId}` 只读状态轮询。

阶段 3.4 只做 async AgentRun、status polling、SSE / WebSocket、batch API、cancel / retry 和 AgentRun list
pagination 的评估决策，不新增 runtime endpoint，不修改 Controller / DTO / service 行为。

## Current API Baseline

当前 HTTP API 是 demo/backend API surface，不是完整生产 API 平台。

- Ticket: create / get / list with bounded pagination。
- AgentRun: create/start for a ticket，以及 `GET /api/agent-runs/{runId}` read-only status polling。
- Trace: ToolCallTrace read-only view。
- Execution Tree: read-only explanation view。
- Approval: pending / get / approve / reject。
- Health: `/api/health` 和 `/actuator/health`。
- API docs: `/v3/api-docs` 和 Swagger UI。

`search_aftersale_policy` 仍是 LOW-risk read-only ToolRegistry tool，不是 public RAG HTTP endpoint。RAG evidence
是 policy evidence，不执行业务动作。

## Problem Statement

生产级异步、流式和批量 API 会改变执行模型、安全模型和审计模型，不能直接作为小修补加入当前同步 demo path。
如果缺少状态机、幂等、权限、限流、事件脱敏和部分失败模型，这些 API 反而会绕过当前 ToolRegistry / Approval /
ToolCallTrace 边界。

## Decision

阶段 3.4 的决策是：

- 保留当前 synchronous create/start + `GET /api/agent-runs/{runId}` status polling 作为当前安全路径。
- 不在本阶段实现 async AgentRun runtime。
- 不在本阶段实现 SSE / WebSocket streaming。
- 不在本阶段实现 batch API。
- 不在本阶段实现 cancel / retry API。
- AgentRun list pagination 作为后续 read-only API 候选，优先级高于 streaming 和 batch。
- 任何后续 streaming / batch / cancel / retry 都必须先完成 production auth / RBAC、幂等、限流和审计设计。

推荐后续顺序：

1. AgentRun list pagination。
2. async AgentRun execution design。
3. status polling hardening。
4. cancel / retry with idempotency and state-machine rules。
5. SSE streaming as opt-in after auth / RBAC。
6. batch API after idempotency / rate-limit / partial-failure design。
7. WebSocket only if SSE cannot satisfy review needs。

## Async AgentRun Evaluation

Async AgentRun 不是简单把当前方法放到后台线程。它需要：

- AgentRun state machine 明确 QUEUED / RUNNING / WAITING_APPROVAL / SUCCEEDED / FAILED / CANCELLED 等状态。
- executor / queue / scheduler 选择和关闭策略。
- idempotent run request，避免重复提交同一 ticket 的重复执行。
- failure recovery、timeout、duplicate submission protection。
- ToolCallTrace ordering 和 Execution Tree 读取一致性。
- Approval pending state preservation，不能因后台任务重试绕过审批。
- 默认测试继续离线，不依赖 Redis、消息队列、Docker、数据库或外部网络。

因此，本阶段不实现 async AgentRun，只记录 future design boundary。

## Status Polling Evaluation

当前 `GET /api/agent-runs/{runId}` 是安全的最小读取路径：

- 只返回 runId、ticketId、status、时间字段、final / failure summary 和 trace / execution-tree 链接。
- 不运行 Planner。
- 不调用 ToolRegistry。
- 不写 ToolCallTrace。
- 不修改 Ticket、Workspace、Approval 或 Execution Tree。
- 不暴露 raw prompt、raw LLM response、provider secret 或完整 workspace。

后续即使引入 async AgentRun，也应继续保留 status polling 作为基础观测路径。

## SSE / WebSocket Streaming Evaluation

SSE / WebSocket streaming 当前不实现。后续若实现，应满足：

- streaming 只输出安全事件摘要，例如 AgentRun status、subtask status、tool call started / completed、
  approval required。
- 不暴露 raw prompt、raw LLM response、API key、完整 tool output、完整 evidence chunk、raw dataset path 或
  provider config。
- streaming 不替代 ToolCallTrace，最终审计事实仍来自 ToolCallTrace、ApprovalRequest 和 Execution Tree。
- streaming 不绕过 RiskPolicy / Approval，不让高风险动作自动执行。
- 生产 auth / RBAC 是 streaming 的前置条件，trace、execution-tree 和 streaming 都需要权限控制。

SSE 比 WebSocket 更适合作为第一候选，因为它满足单向进度流的常见需求，复杂度低于双向连接。WebSocket 仅在需要
双向交互或多路订阅时再评估。

## Batch API Evaluation

Batch API 当前不实现。后续若实现，应先定义：

- size limit。
- rate limit。
- idempotency key / duplicate submission strategy。
- partial failure model。
- per-item audit record。
- approval backlog control，避免批量创建高风险审批堆积。
- permission model，防止批量接口绕过人工审核或运营权限。

Batch API 不得让 LLM 直接执行工具，不得绕过 ToolRegistry，不得执行真实退款、换货、优惠券补偿、支付、物流或
争议关闭。

## Cancel / Retry Evaluation

Cancel / retry API 当前不实现。后续若实现，应先明确：

- 哪些 AgentRun 状态允许 cancel。
- COMPLETED / FAILED / WAITING_APPROVAL 状态下的 retry 语义。
- retry 是否复用原始 input、是否创建新 run、如何关联原始 trace。
- 已完成工具调用的 side-effect boundary。
- Approval pending 时 cancel / retry 是否允许。
- ToolCallTrace、Workspace 和 Execution Tree 如何表达取消或重试链路。

在没有幂等和状态机规则前，不应暴露 cancel / retry runtime API。

## AgentRun List Pagination Evaluation

AgentRun list pagination 是最小的后续 read-only API 候选，建议早于 async / streaming / batch 推进。

建议边界：

- `GET /api/agent-runs?page=0&size=20` 只读分页。
- 支持 ticketId、status、createdFrom、createdTo 等安全过滤。
- 不返回 raw prompt、raw provider response、完整 tool output 或完整 workspace。
- 不执行 Planner，不调用 ToolRegistry，不写 ToolCallTrace，不修改 Ticket 或 Approval。
- 默认 tests 使用 in-memory repository，继续离线。

## Security / Auth Boundary

Production auth / RBAC 未完成，必须作为 streaming、batch、cancel / retry 和生产 API hardening 的前置项。

后续权限至少需要覆盖：

- approval approve / reject。
- trace read。
- execution tree read。
- AgentRun status read。
- streaming subscription。
- batch submission。
- cancel / retry。

当前项目不声明 production API 已安全加固。

## ToolRegistry / Planner Boundary

LLM 可以规划工具，但不得直接执行工具。ToolRegistry 仍是 Agent tool execution entry。

任何 async / streaming / batch / retry 设计都不得：

- 绕过 ToolRegistry。
- 绕过 RiskPolicy / Approval。
- 绕过 ToolCallTrace。
- 把 `search_aftersale_policy` 暴露成 public RAG HTTP endpoint。
- 让 RAG evidence 执行业务动作。

## Trace / Workspace / Execution Tree Boundary

ToolCallTrace 仍是工具调用审计事实来源。Workspace 仍是单次 AgentRun 工作记忆。Execution Tree 仍是只读解释视图。

Streaming 和 status polling 只能展示这些事实的安全摘要，不能替代审计记录，也不能写入新的业务状态。

## Observability Boundary

当前 observability baseline 是 MDC / structured logs、ToolCallTrace、ApprovalRequest、Execution Tree、
Actuator health、RAG readiness diagnostics、OpenAPI docs 和 offline RAG evaluation metrics。

Async / streaming 后续会需要 AgentRun queue metrics、event delivery metrics、retry metrics 和 latency metrics，
但这些是 future / opt-in，不属于阶段 3.4 runtime 实现。

## Default Offline Boundary

阶段 3.4 docs harness 只读仓库文件，不启动应用，不调用 HTTP，不连接数据库，不调用 LLM、embedding provider、
Spring AI VectorStore、Docker、Redis、MySQL、PostgreSQL、PGvector 或外部网络。

默认验证仍不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider 或
external network。

## Non-goals

- 不实现 async AgentRun runtime。
- 不实现 SSE / WebSocket runtime。
- 不实现 batch API runtime。
- 不实现 cancel / retry API。
- 不实现 AgentRun list pagination。
- 不实现 production auth / RBAC。
- 不新增 public RAG HTTP endpoint。
- 不修改 ToolRegistry、Planner、RAG runtime、ingestion、health、OpenAPI config、ToolCallTrace、Workspace、
  Execution Tree 或 AgentApplicationService。
- 不接入真实退款、换货、优惠券补偿、支付、物流或争议关闭。

## Alternatives Considered

- 直接实现后台线程式 async AgentRun：拒绝。缺少幂等、状态机、失败恢复和关闭策略。
- 直接实现 SSE：拒绝。缺少事件模型、权限控制和字段脱敏边界。
- 直接实现 WebSocket：拒绝。当前需求以只读进度展示为主，WebSocket 复杂度过高。
- 直接实现 batch AgentRun：拒绝。缺少限流、部分失败、审批堆积和审计模型。
- 公开 RAG search HTTP endpoint：拒绝。当前 RAG 检索是 ToolRegistry 内部能力。

## Consequences

- 当前 API 口径保持真实：Ticket pagination 和 AgentRun status polling 已完成；async / streaming / batch 未实现。
- 后续 API hardening 有明确顺序，避免一次性引入复杂 runtime。
- ToolRegistry、Approval、ToolCallTrace、Workspace、Execution Tree 和 RAG evidence-only 边界继续保持。
- 生产 auth / RBAC、幂等、限流和 API audit hardening 被明确为 streaming / batch 前置工作。

## Follow-ups

- 阶段 4：领域模型强化。
- 后续 API 任务：AgentRun list pagination。
- 后续 API 任务：async AgentRun execution design。
- 后续 API 任务：cancel / retry state-machine and idempotency design。
- 后续 API 任务：SSE streaming after auth / RBAC。
- 后续 API 任务：batch API after rate-limit and partial-failure design。

## Completion Signal

TASK_COMPLETE
