# V1 Quality Score

Date: 2026-05-14

## Current Completion Score

Overall V1 score: 82 / 100.

The project has a complete local demo path, mechanical validation commands, modular package boundaries, traceable
Agent tool execution, and clear Harness documentation. The score is not higher because V1 intentionally avoids a real
LLM, real persistence, real order lookup tools, production security, and a richer evaluation dataset.

## Architecture Quality

Score: 86 / 100.

Strengths:

- Modular monolith package layout is in place under `com.example.aftersale`.
- API classes call application services instead of repositories.
- Domain models are independent from Spring Web.
- Agent orchestration does not directly depend on business repositories.
- Tools are prevented from direct repository access by ArchUnit.
- Business modules do not depend on the Agent module.

Current gaps:

- Infrastructure is in-memory only.
- Approval is represented at domain/tool-risk level, not as a full application flow.
- Order is modeled as a domain concept, but V1 does not include executable order query tools.

## Test Quality

Score: 84 / 100.

Strengths:

- API tests cover ticket creation and lookup.
- Domain tests cover Ticket, AgentRun, ToolCallTrace, and approval-risk behavior.
- Policy tests cover initialized V1 policy data, matching, empty results, and tool execution.
- Tool tests cover registry lookup, unknown tools, low-risk execution, high-risk approval blocking, and failure output.
- Agent flow tests cover the demo path, trace exposure, failure status, and trace cleanup.
- Architecture tests provide mechanical back-pressure for key boundaries.

Current gaps:

- Tests use in-memory collaborators and do not cover real persistence.
- Intent classification has deterministic examples but not a broad evaluation dataset.
- Trace assertions verify key behavior but do not benchmark latency or concurrency.

## Agent Capability Quality

Score: 76 / 100.

Strengths:

- Agent creates an `AgentRun`.
- Intent classification is deterministic and testable.
- Planning output is structured JSON.
- Tools are invoked through `ToolRegistry`.
- Tool calls are recorded as `ToolCallTrace`.
- Final suggestions include policy evidence.
- Tool failures surface as failed outputs and failed traces.

Current gaps:

- No real LLM or model adapter.
- No prompt versioning implementation beyond document guidance.
- No real order lookup or user order history tool.
- No multi-step reasoning beyond the V1 rule-based orchestration.

## Demo Readiness

Score: 88 / 100.

Strengths:

- The app can be started locally with Maven.
- README includes a curl-based demo walkthrough.
- Demo shows ticket creation, AgentRun creation, ticket update, and trace query.
- Tool trace output demonstrates input, output, status, latency, and run linkage.

Current gaps:

- Demo is API-only.
- Demo data resets when the application restarts.
- The expected trace contains policy retrieval and ticket note tools, not order lookup.

## Current Shortcomings

- V1 uses in-memory repositories only.
- Agent behavior is rule-based and intentionally deterministic.
- No production authentication or authorization.
- No real refund, compensation, payment, inventory, logistics, or order system integration.
- Human approval is a boundary and domain concept, not a complete operator workflow.
- No vector RAG, reranking, or retrieval evaluation set.

## V2 Improvement Directions

- Add real MySQL persistence and migration scripts.
- Add executable order query tools such as `get_order_by_id` and `get_user_orders`.
- Add an approval application service and review API for high-risk actions.
- Introduce a real LLM adapter behind a stable Agent interface.
- Add prompt versioning and prompt regression tests.
- Upgrade policy retrieval to vector or hybrid retrieval with cited evidence.
- Add an Agent evaluation dataset for intent, tool selection, and final suggestion quality.
- Add Docker Compose for repeatable local demos.
- Add observability metrics for AgentRun and tool latency.

## V2 Quality Targets

V2 质量目标聚焦真实 LLM 接入后的可控性。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| LLM 接入质量 | LLM 只作为 Planner，不直接执行工具 | 代码结构 + 测试 + 文档检查 |
| Planner 抽象质量 | AgentApplicationService 依赖 AgentPlanner 抽象 | 单元测试 + 架构检查 |
| 测试确定性 | 默认测试不依赖真实 LLM/API Key/外部网络 | `mvn test` 离线通过 |
| 安全边界 | LLM 不得绕过 ToolRegistry 或审批边界 | 代码检查 + AgentPlan 校验 |
| 可回滚性 | RuleBasedAgentPlanner 保留 | 配置切换测试 |
| 可配置性 | planner mode 可配置 | 配置测试 |
| Trace 完整性 | 工具调用仍记录 ToolCallTrace | AgentRunFlowTest |
| 文档一致性 | README 不夸大未完成能力 | M9/V2 review 检查 |

### V2.1 目标评分

| 项目 | 目标分 |
|---|---:|
| Planner 抽象 | 90 |
| LLM 接入边界 | 85 |
| 测试确定性 | 95 |
| 安全边界 | 90 |
| 文档一致性 | 90 |
| 可演示性 | 85 |

### V2.1 不接受的退化

- V1 demo 不能跑；
- 默认测试需要 API Key；
- ToolCallTrace 丢失；
- Agent 绕过 ToolRegistry；
- API Key 出现在仓库；
- 高风险动作被自动执行；
- README 把计划能力写成已完成能力。

### V2.1 Current Status

Status: completed for adapter boundary and V2.1.1 structured LLM provider-call boundary.

Completed:

- `AgentApplicationService` now depends on `AgentPlanner`.
- `RuleBasedAgentPlanner` preserves V1 deterministic behavior.
- `FakeAgentPlanner` supports deterministic AgentRun tests.
- `LlmAgentPlanner` validates required API Key configuration and does not fake a provider call.
- `LlmClient` / `LlmRequest` / `LlmResponse` isolate provider calls from Agent orchestration.
- The OpenAI-compatible provider client is implemented behind `LlmClient`.
- AgentPlan JSON parsing and validation are implemented.
- Planner prompt construction is centralized under `agent/prompt`.
- Default tests run without real LLM, API Key, or external network.
- Tool execution remains behind `ToolRegistry`.
- ToolCallTrace remains in the AgentRun flow.

Remaining V2.1 follow-up:

- Add live provider smoke tests that are explicitly opt-in and never part of default `mvn test`.
- Add prompt regression fixtures and broader malformed-output cases.

## V2.3 Quality Targets

V2.3 质量目标聚焦复杂售后诉求拆解的准确性、合法性和可观测性。当前 V2.3 已完成基础实现。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| 子任务拆解准确性 | 复杂售后问题能拆成 RETURN / EXCHANGE / COUPON_CONSULTATION / LOGISTICS_ISSUE 等结构化子任务 | Planner 单元测试 + 评测样例 |
| 子任务工具规划合法性 | 每个 subtask 的 plannedTools 均来自 ToolRegistry 已注册工具 | AgentPlanValidator 测试 |
| 子任务 trace 可观测性 | 每个实际工具调用继续写入 ToolCallTrace，并能追溯到多意图处理链路 | AgentRun flow 测试 + trace API 检查 |
| 子任务依赖校验 | dependencies 只能引用合法 subtaskId，且不得形成循环依赖 | Validator 单元测试 |
| 风险边界 | 子任务不能声明真实退款、真实换货、优惠券补偿或争议关闭已经完成 | Parser / Validator 负向测试 |
| 测试确定性 | 默认测试不依赖真实 LLM、API Key 或外部网络 | `mvn test` 离线通过 |

### V2.3 不接受的退化

- V1/V2.1/V2.2 demo 流程不能跑；
- 默认测试需要真实 LLM 或 API Key；
- 子任务绕过 ToolRegistry 执行工具；
- ToolCallTrace 丢失或隐藏失败工具调用；
- 高风险动作被写成已完成；
- README 或 Harness 文档把 V2.4+ 未完成扩展能力写成已实现能力。

### V2.3 Current Status

Status: completed for deterministic rule-based multi-intent planning and sequential subtask execution.

Completed:

- `AgentPlan` supports `subtasks`.
- `AgentPlanParser` parses structured subtask JSON.
- `AgentPlanValidator` rejects unknown subtask type, unknown tools, blank subtask policy query, missing dependencies,
  dependency cycles, duplicate IDs, and oversized subtask lists.
- `RuleBasedAgentPlanner` splits return + exchange + coupon consultation messages into subtasks.
- `AgentApplicationService` executes subtasks sequentially through ToolRegistry.
- ToolCallTrace input JSON carries subtask metadata.
- Default tests remain offline and deterministic.

## V2.4 Quality Targets

V2.4 质量目标聚焦 Specialist Agent Handler 的分发正确性、边界安全和对既有流程的非退化。V2.4 当前是下一阶段目标，
不表示功能已经完成。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| Handler 分发正确性 | registry 能按 `SubtaskType` 找到唯一 handler | Registry 单元测试 |
| Handler 支持类型覆盖 | RETURN / EXCHANGE / COUPON_CONSULTATION / LOGISTICS_ISSUE 有明确 handler 或 fallback | 覆盖测试 + 文档检查 |
| Handler 工具调用合法性 | handler 内部工具调用必须通过 ToolRegistry | Flow 测试 + mock/fake registry 测试 |
| Handler 不访问 Repository | handler 不依赖业务 Repository 或 infrastructure repository | ArchUnit |
| Handler 结果可追踪 | handler 内部工具调用继续写入 ToolCallTrace | AgentRun flow 测试 |
| 风险边界 | handler 不直接执行真实退款、换货、优惠券补偿、支付、物流或争议关闭 | 单元测试 + 风险策略检查 |
| 单意图 / 多意图流程不退化 | V1/V2.2 单意图和 V2.3 多意图流程继续通过 | `mvn test` + API flow 测试 |
| 测试确定性 | 默认测试不依赖真实 LLM、API Key 或外部网络 | `mvn test` 离线通过 |

### V2.4 不接受的退化

- handler 绕过 ToolRegistry；
- handler 直接访问 Repository；
- handler 直接调用 LLM；
- handler 直接执行真实高风险动作；
- ToolCallTrace 丢失 handler 内部工具调用；
- V2.3 多意图流程不能跑；
- 默认测试需要真实 LLM 或 API Key；
- README 或 Harness 文档把 V2.4 写成已实现能力。
