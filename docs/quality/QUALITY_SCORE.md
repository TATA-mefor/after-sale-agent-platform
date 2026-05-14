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
