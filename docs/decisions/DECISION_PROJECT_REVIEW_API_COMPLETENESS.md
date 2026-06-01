# 项目审查 API 完整性决策

Date: 2026-06-01

Status: Completed

## Context

项目审查指出当前 HTTP API 缺少分页、异步 AgentRun、SSE / WebSocket 流式输出和批量操作。该判断总体成立，
但需要先把当前 API surface 与后续 API hardening 路线区分清楚。

本决策记录只做阶段 3.1：API Surface Audit / API Completeness Decision。它不新增 endpoint，不修改
Controller，不修改 OpenAPI runtime，不修改 AgentRun、ToolRegistry、RAG、Approval 或 Execution Tree 行为。

## Current API Surface

当前 API 是 demo/backend API surface，不是完整生产 CRUD 平台。

- Health: `GET /api/health` 和 `GET /actuator/health`。
- Ticket: `POST /api/tickets` 创建工单；`GET /api/tickets/{ticketId}` 读取单个工单。
- AgentRun: `POST /api/tickets/{ticketId}/agent-runs` 为已有工单创建并触发当前 AgentRun。
- Trace: `GET /api/agent-runs/{runId}/traces` 提供 ToolCallTrace 只读审计视图。
- Execution Tree: `GET /api/agent-runs/{runId}/execution-tree` 提供只读执行树视图。
- Approval: `GET /api/approval-requests/pending`、`GET /api/approval-requests/{approvalRequestId}`、
  `POST /api/approval-requests/{approvalRequestId}/approve` 和
  `POST /api/approval-requests/{approvalRequestId}/reject`。
- API docs: `/v3/api-docs` 和 Swagger UI 展示现有 HTTP API 文档。

`search_aftersale_policy` 是内部 LOW-risk read-only ToolRegistry tool，不是 public RAG HTTP endpoint。RAG
evidence 是 policy evidence，不执行业务动作。

## Current Limitations

- Ticket 当前没有 list / query / pagination endpoint。
- AgentRun 当前没有独立的 get/status polling endpoint。
- AgentRun 当前不是生产级异步 job system。
- Trace 和 Execution Tree 是查询视图，不是实时流式输出。
- SSE / WebSocket trace streaming 未实现。
- Batch API 未实现。
- Production auth / RBAC 未实现。
- Idempotency、rate limiting、operator audit hardening 和 API versioning 仍是 future work。
- OpenAPI docs 是现有 API 文档，不代表 production API hardening 已完成。

## Decision

阶段 3.1 只完成 API surface 审计和 API completeness 决策。当前 API 文档应明确它是 demo/backend surface，不是
完整生产 CRUD。

后续 API 改进按小阶段推进：

- 阶段 3.2：list / pagination foundation。
- 阶段 3.3：AgentRun get/status polling endpoint。
- 阶段 3.4：async AgentRun、SSE / WebSocket streaming 和 batch API 评估。

除非后续产品需求明确，否则不新增 public RAG search HTTP endpoint。Agent runtime 仍通过 ToolRegistry 调用
`search_aftersale_policy`，高风险动作仍由 RiskPolicy / Approval gate 保护。

## API Improvement Roadmap

1. 先补只读 list/query 类 endpoint 的分页策略，避免一次性返回无界集合。
2. 再补 AgentRun read/status 模型，让前端或 reviewer 能独立查询运行状态。
3. 再评估异步 AgentRun 与进度模型，明确同步 demo path 与异步 production path 的边界。
4. 最后评估 SSE / WebSocket 和 batch API，先有事件模型、安全边界和测试策略，再实现 runtime。

## Pagination Strategy

分页是阶段 3.2 的候选范围，不属于阶段 3.1 已实现能力。

建议策略：

- 对 list/query endpoint 使用显式 `page` / `size` 或 cursor 参数。
- 设置默认 page size 和最大 page size。
- 返回总数或下一页标记时避免引入昂贵默认查询。
- OpenAPI examples 使用 fake/demo 数据，不包含客户隐私、secret 或本地路径。

## AgentRun Read / Status Strategy

AgentRun get/status polling 是阶段 3.3 的候选范围，不属于阶段 3.1 已实现能力。

建议策略：

- 新增只读 AgentRun get endpoint 时只返回状态、ticketId、runId、intent、final summary 和时间字段等安全摘要。
- ToolCallTrace 细节继续通过 trace endpoint 查询。
- Execution Tree 继续作为解释视图，不替代 AgentRun status。
- 不在 status endpoint 中暴露 raw prompt、provider secrets、长 raw provider response 或完整内部 workspace。

## Async AgentRun Strategy

生产级异步 AgentRun 是阶段 3.4 或后续任务，不属于阶段 3.1 已实现能力。

建议策略：

- 先定义 AgentRun 状态模型和幂等边界。
- 再决定是否引入队列、调度器或线程池。
- 默认测试必须继续离线、确定性，不依赖 Redis、消息队列、Docker、数据库或外部网络。
- 异步执行不得绕过 ToolRegistry、RiskPolicy、Approval 或 ToolCallTrace。

## Streaming / SSE / WebSocket Strategy

SSE / WebSocket trace streaming 是 future / opt-in API path，不属于阶段 3.1 已实现能力。

建议策略：

- 先确定事件类型：AgentRun status、subtask status、tool call started / completed、approval required。
- 流式事件只暴露安全摘要，不暴露 secret、raw prompt、raw provider response 或完整 evidence chunk。
- 流式输出必须与 ToolCallTrace 和 Execution Tree 的审计事实一致。

## Batch API Strategy

Batch API 是 future work，不属于阶段 3.1 已实现能力。

建议策略：

- 只有在明确批量创建工单、批量查询或运营审核需求后再设计。
- 批量 API 必须有 size limit、partial failure 模型和审计记录边界。
- 批量 API 不得用于绕过 Approval 执行高风险动作。

## OpenAPI Documentation Strategy

OpenAPI docs 应展示当前 existing HTTP APIs，并清楚标记边界：

- 当前 Ticket API 是 create/get，不是完整 Ticket CRUD。
- 当前 AgentRun API 是 create/start，不是完整异步 job API。
- Trace 和 Execution Tree 是 read-only views。
- Approval API 是 pending/get/approve/reject。
- `search_aftersale_policy` 是 ToolRegistry tool，不是 public RAG HTTP endpoint。
- `/actuator/health` 是默认 health exposure；OpenAPI docs 不代表 production deployment。

## Security / Auth Boundary

Production auth / RBAC 是 future work，不属于阶段 3.1 已实现能力。当前文档不能声明 production auth 已完成。

后续 API hardening 需要单独处理：

- authentication / authorization；
- operator role and approval permission；
- idempotency key；
- rate limiting；
- API audit hardening；
- error response consistency；
- sensitive field redaction。

## ToolRegistry Boundary

ToolRegistry 仍是 Agent tool execution entry。LLM 可以规划工具，但不得直接执行工具，也不得通过 HTTP surface
获得绕过 ToolRegistry 的执行能力。

`search_aftersale_policy` 仍是 LOW-risk read-only tool。它只返回 policy evidence，不执行退款、换货、优惠券补偿、
支付、物流、库存或争议关闭。

## Default Offline Boundary

阶段 3.1 docs harness 只读仓库文件，不启动应用，不调用 HTTP，不连接数据库，不调用 LLM、embedding provider、
Spring AI VectorStore、Docker、Redis、MySQL、PostgreSQL、PGvector 或外部网络。

默认验证仍不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider 或
external network。

## Non-goals

- 不新增 endpoint。
- 不修改 Controller、DTO runtime 或 OpenAPI runtime config。
- 不实现分页。
- 不实现 AgentRun get/status polling。
- 不实现异步 AgentRun。
- 不实现 SSE / WebSocket。
- 不实现 batch API。
- 不新增 public RAG HTTP endpoint。
- 不实现 production auth / RBAC。
- 不接入真实退款、换货、补偿、支付或物流系统。

## Alternatives Considered

- 直接实现分页和 AgentRun status：拒绝。当前阶段目标是先校准 API 事实和路线，避免范围膨胀。
- 公开 RAG search HTTP endpoint：拒绝。当前产品边界中 RAG policy retrieval 是 Agent ToolRegistry 内部能力。
- 直接引入 SSE：拒绝。当前缺少事件模型、异步运行模型和安全字段边界。

## Consequences

- README、OpenAPI docs、整改方案和质量文档会用同一事实口径描述当前 API。
- 后续 API 改进可以按阶段拆分，避免一次性把 production API hardening 混入当前 demo/backend surface。
- ToolRegistry、Approval、RAG evidence-only 和默认离线验证边界继续保持。

## Follow-ups

- 阶段 3.2：Ticket / Approval 等 list endpoint 的分页策略和最小实现。
- 阶段 3.3：AgentRun get/status polling endpoint。
- 阶段 3.4：异步 AgentRun、SSE / WebSocket、batch API 评估。
- 后续安全任务：production auth / RBAC、idempotency、rate limiting、audit hardening。

## Completion Signal

TASK_COMPLETE
