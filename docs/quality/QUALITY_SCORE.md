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

V2.4 质量目标聚焦 Specialist Agent Handler 的分发正确性、边界安全和对既有流程的非退化。当前 V2.4 已完成
模块化单体内的 handler registry 和策略类分发。

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
- README 或 Harness 文档把微服务、真实退款、真实换货或真实优惠券补偿写成 V2.4 已实现能力。

### V2.4 Current Status

Status: completed for specialist handler dispatch in the modular monolith.

Completed:

- `SpecialistAgentHandler` defines the handler contract.
- `SpecialistAgentHandlerRegistry` maps each supported `SubtaskType` to one handler and rejects duplicate coverage.
- `SubtaskExecutionContext` carries run, ticket, plan, subtask, tool list, risk summary, and previous results.
- `SubtaskExecutionResult` returns structured status, summary, evidence, tool calls, error, and approval requirement.
- RETURN, EXCHANGE, COUPON_CONSULTATION, LOGISTICS_ISSUE, GENERAL_CONSULTATION, and HUMAN_ESCALATION have handlers.
- `AgentApplicationService` dispatches multi-intent subtasks through the registry.
- Handler tool calls still go through `ToolRegistry` and keep `ToolCallTrace` records.
- ArchUnit prevents handler dependency on repositories, LLM infrastructure, and Spring Web.
- Default tests remain offline and deterministic.

Remaining follow-up:

- Approval APIs are completed in V2.7.
- Execution Tree is completed in V2.8.
- Handler behavior remains deterministic and policy/tool based; there is no real refund, exchange, coupon compensation,
  logistics, payment, database, or microservice integration.

## V2.5 Quality Targets

V2.5 质量目标聚焦受控政策检索工具的结构化输出、可替换边界和 handler 调用路径。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| 检索模型结构化 | 使用 `PolicySearchQuery` / `PolicySnippet` / `PolicySearchResult` | 单元测试 + 代码检查 |
| ToolRegistry 边界 | `search_aftersale_policy` 只能通过 ToolRegistry 被 Agent/Handler 使用 | ToolRegistry 测试 + ArchUnit |
| Handler 调用顺序 | Handler 在动作工具前执行政策检索 | Specialist handler 单元测试 |
| 空结果安全 | 未支持 query 返回空结果和 message，不编造依据 | PolicySearchTest |
| 可替换性 | 当前为内存关键词检索，后续可替换 VectorStore / PGvector | 架构文档 + Repository 抽象 |
| 测试确定性 | 默认测试不依赖真实 LLM、API Key、PGvector 或网络 | `mvn test` 离线通过 |

### V2.5 Current Status

Status: completed for controlled in-memory policy retrieval through ToolRegistry.

Completed:

- Added `PolicySearchQuery`, `PolicySnippet`, and `PolicySearchResult`.
- `PolicyRepository` now exposes controlled search.
- `InMemoryPolicyRepository` performs local keyword retrieval.
- `SearchAfterSalePolicyToolExecutor` registers LOW-risk `search_aftersale_policy`.
- Tool output includes structured results and message.
- Handler tool planning keeps policy retrieval before action tools.
- Unsupported policy queries return structured empty results.
- Default tests remain offline and deterministic.

## V2.6 Quality Targets

V2.6 质量目标聚焦单次 `AgentRun` 内结构化工作记忆的完整性、一致性和边界安全。当前 V2.6 已完成
in-memory `AgentWorkspace` 基础实现，不表示已经实现长期记忆、跨会话记忆或外部持久化。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| Workspace 字段完整性 | `AgentWorkspace` 能表达订单事实、政策依据、子任务记忆、工具结果摘要和风险标记 | 模型测试 |
| Handler 写入一致性 | Handler 执行工具后按约定写入 workspace | Handler flow 测试 |
| Final summary 来源 | final summary 基于 workspace 汇总，而不是散落局部变量 | AgentRun flow 测试 |
| Trace / workspace 边界 | workspace 不替代 ToolCallTrace，trace 继续完整记录工具调用 | Trace 测试 + 文档检查 |
| ToolRegistry 边界 | workspace 不绕过 ToolRegistry、不直接访问 Repository | ArchUnit + 单元测试 |
| 敏感信息控制 | workspace 不保存 API Key、敏感凭证、完整长 prompt 或 LLM 原始长文本 | 单元测试 + 代码检查 |
| 默认测试确定性 | 默认测试不依赖真实 LLM、API Key、Redis、MySQL、向量库或网络 | `mvn test` 离线通过 |

### V2.6 Current Status

Status: completed for single-`AgentRun` in-memory structured workspace.

Completed:

- Added `AgentWorkspace`, `OrderFact`, `PolicyEvidence`, `SubtaskMemory`, `ToolResultSummary`, and `RiskFlag`.
- `AgentApplicationService` creates workspace when an `AgentRun` starts.
- `SubtaskExecutionContext` carries workspace to Specialist Handlers.
- Single-intent direct tool execution writes order facts, policy evidence, and tool result summaries.
- Multi-intent handler execution writes order facts, policy evidence, tool result summaries, subtask memories, and risk
  flags.
- Final summary is assembled from workspace content.
- ToolCallTrace remains the audit record for tool calls.
- Default tests remain offline and deterministic.

### V2.6 不接受的退化

- workspace 替代 ToolCallTrace；
- workspace 绕过 ToolRegistry；
- workspace 直接访问 Repository；
- workspace 保存 API Key、敏感凭证、完整长 prompt 或 LLM 原始长文本；
- workspace 演变为长期用户画像或跨会话记忆；
- 默认测试需要真实 LLM、Redis、MySQL、PGvector 或外部网络；
- README 或 Harness 文档把 V2.6 写成已实现能力。

## V2.7 Quality Targets

V2.7 质量目标聚焦高风险审批 API 的可调用性、幂等边界和非真实业务执行边界。当前 V2.7 已完成
in-memory Approval APIs 基础实现。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| 审批请求结构化 | `ApprovalRequest` 保留 ticketId、agentRunId、subtaskId、toolName、riskLevel、status 和 decision reason | 服务测试 + API 测试 |
| Pending 查询 | 可以查询待审批请求 | API 测试 |
| 单条查询 | 可以查询单个审批请求 | API 测试 |
| 审批状态流转 | approve / reject 只能处理 PENDING 请求 | 服务测试 + API 冲突测试 |
| 拒绝原因 | reject 必须保存 reason | 服务测试 |
| Ticket 回写 | 审批创建和审批结果写入 Ticket note / status | 服务测试 |
| 高风险边界 | high-risk subtask 创建审批请求并进入 WAITING_HUMAN_APPROVAL | Agent flow 测试 |
| 低风险边界 | LOW risk action 不创建审批请求 | 服务测试 |
| 架构边界 | Controller 不访问 Repository，Agent / Handler 不直接访问 ApprovalRepository | ArchitectureTest |
| 测试确定性 | 默认测试不依赖真实 LLM、API Key、Redis、MySQL 或网络 | `mvn test` 离线通过 |

### V2.7 Current Status

Status: completed for in-memory approval API flow.

Completed:

- Added `ApprovalApplicationService`.
- Added `ApprovalRepository` and `InMemoryApprovalRepository`.
- Added `ApprovalController` and approval response/decision DTOs.
- Added pending query, single request query, approve, and reject endpoints.
- Approval decisions write back to Ticket note/status.
- High-risk subtasks create approval requests and leave tickets waiting for human approval.
- Low-risk actions do not create approval requests.
- Repeated approval decisions return clear conflict errors.
- Default tests remain offline and deterministic.

### V2.7 不接受的退化

- approval API 执行真实退款、真实换货或真实优惠券补偿；
- Controller 直接访问 Repository；
- Handler / Agent 直接访问 ApprovalRepository；
- 已完成审批请求可被重复 approve / reject；
- reject 不保存 reason；
- high-risk subtask 被标记为直接完成；
- 默认测试需要真实 LLM、API Key、Redis、MySQL 或外部网络。

## V2.8 Quality Targets

V2.8 质量目标聚焦执行过程的只读可解释视图。当前 V2.8 已完成 in-memory Execution Tree API 基础实现。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| 根节点完整性 | execution tree 返回 runId、ticketId、AgentRun 状态、finalSuggestion、rootSummary 和时间戳 | API 测试 |
| Subtask 归属 | 多意图 AgentRun 返回多个 subtask node | API 测试 |
| ToolCall 归属 | trace inputJson 中存在 subtaskId 时挂到对应 subtask | API 测试 |
| Root-level trace | 无 subtaskId 的 trace 留在 root-level toolCalls | API 测试 |
| Approval 归属 | high-risk subtask 创建的 ApprovalRequest 挂到对应 subtask | API 测试 |
| 只读边界 | 查询 execution tree 不改变 Ticket 或 ApprovalRequest 状态 | API 测试 |
| 错误清晰度 | 不存在 runId 返回 `AGENT_RUN_NOT_FOUND` | API 测试 |
| 架构边界 | Controller 不访问 Repository，聚合逻辑放在 ApplicationService | ArchitectureTest |
| 测试确定性 | 默认测试不依赖真实 LLM、API Key、Redis、MySQL 或网络 | `mvn test` 离线通过 |

### V2.8 Current Status

Status: completed for read-only in-memory execution tree view.

Completed:

- Added `ExecutionTreeApplicationService`.
- Added `AgentExecutionTreeController`.
- Added structured execution tree response models.
- Tool calls are attached to subtasks by `ToolCallTrace.inputJson.subtaskId` when present.
- Tool calls without subtask metadata remain root-level.
- Approval requests are attached to subtasks by `runId` and `subtaskId` when present.
- Missing run IDs return a clear not-found API response.
- Execution tree queries do not mutate Ticket or ApprovalRequest state.
- Default tests remain offline and deterministic.

### V2.8 不接受的退化

- Execution Tree API 修改 Ticket、AgentRun、ToolCallTrace 或 ApprovalRequest；
- Controller 直接访问 Repository；
- 为 Execution Tree 大幅重构 Agent 主执行链路或 ToolCallTrace 模型；
- 默认测试需要真实 LLM、API Key、Redis、MySQL、向量库或外部网络；
- README 或 Harness 文档把前端可视化、并行执行、消息队列或真实业务执行写成 V2.8 已实现能力。

## V2.9 Quality Targets

V2.9 质量目标聚焦离线、确定性的 Agent 规划评测。当前 V2.9 已完成 JSONL 评测集和 rule-based evaluation
runner，不表示已完成真实 LLM 评测或 LLM-as-judge。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| 数据集版本化 | 售后评测 case 存放在 `docs/evaluation/aftersale_cases.jsonl` | 文件检查 + loader 测试 |
| 场景覆盖 | 覆盖退货、换货、仅退款、维修、物流、优惠券、多意图、高风险、普通咨询和 UNKNOWN | 数据集检查 |
| 计划合法性 | 每条 case 生成的 `AgentPlan` 必须通过 `AgentPlanValidator` | Evaluation 测试 |
| 指标结构化 | report 包含 total/passed/failed 和 accuracy 指标 | Evaluation 测试 |
| 失败可定位 | failure 包含 caseId、field、expected、actual 和 message | Evaluation 测试 |
| Tool 边界 | policy category 评测通过受控 `search_aftersale_policy` 工具 | Evaluation 测试 |
| 测试确定性 | 默认评测使用 RuleBased planner，不依赖真实 LLM、API Key 或网络 | `mvn test` 离线通过 |
| 非状态变更 | 默认评测不创建或修改 Ticket、AgentRun、ToolCallTrace 或 ApprovalRequest | 代码检查 + 测试 |

### V2.9 Current Status

Status: completed for offline deterministic evaluation dataset and runner.

Completed:

- Added 15-case JSONL evaluation dataset.
- Added `docs/evaluation/EVALUATION.md`.
- Added `EvaluationCase`, `EvaluationExpected`, `EvaluationResult`, `EvaluationReport`, `EvaluationMetric`, and
  `EvaluationFailure`.
- Added `EvaluationApplicationService`.
- Default evaluation runs with `RuleBasedAgentPlanner`.
- Every generated plan is validated with `AgentPlanValidator`.
- Expected tools, subtask types, policy categories, risk level, and approval requirements are checked.
- Current rule-based limitations are surfaced as structured failures instead of hidden.
- Default tests remain offline and deterministic.

### V2.9 不接受的退化

- 默认评测调用真实 LLM、OpenAI provider、外部网络或需要 API Key；
- 评测使用 LLM-as-judge；
- 评测绕过 `AgentPlanValidator`；
- 评测修改 Ticket、AgentRun、ToolCallTrace 或 ApprovalRequest；
- 引入外部评测框架导致本地测试复杂化；
- 删除或降低现有架构、Checkstyle、SpotBugs 或 JUnit 约束。

## V2.10 Quality Targets

V2.10 质量目标聚焦基于 V2.9 评测暴露问题的 deterministic fallback robustness。当前 V2.10 已完成
`RuleBasedAgentPlanner` 规则覆盖增强，不表示已经引入真实 LLM 评测、语义理解模型或外部依赖。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| Refund-only 识别 | 仅退款、只退款、不退货退款、未发货取消并退款等表达能进入 `REFUND_ONLY` | Planner 测试 + Evaluation 测试 |
| Coupon 咨询识别 | 优惠券、退券、补券、优惠没退等表达能进入 `COUPON_CONSULTATION` | Planner 测试 + Evaluation 测试 |
| 双意图拆解 | 退货 + 换货、退货 + 优惠券、物流 + 退款咨询能生成多个 subtasks | Planner 测试 + JSONL 评测 |
| 高风险识别 | 直接退款、立刻退款、投诉、平台介入、金额较大、补偿、赔偿等触发 `HIGH` 或审批要求 | Planner 测试 + Evaluation 测试 |
| 评测改进 | V2.10 rule-based evaluation 至少通过 13/15 cases，且 plan validity 为 100% | Evaluation 测试 |
| 边界不变 | 不调用真实 LLM、不绕过 AgentPlanValidator、ToolRegistry、Approval、Trace 或 Workspace | 单元测试 + 架构检查 |
| 测试确定性 | 默认测试不依赖真实 LLM、API Key、Redis、MySQL、向量库或网络 | `mvn test` 离线通过 |

### V2.10 Current Status

Status: completed for deterministic robustness improvements in the rule-based fallback.

Completed:

- `RuleBasedAgentPlanner` expands refund-only, coupon-only, logistics, return, exchange, and high-risk keyword coverage.
- Single coupon consultation creates a `COUPON_CONSULTATION` subtask while keeping the top-level intent as
  `GENERAL_CONSULTATION`.
- Two-intent combinations now produce multiple ordered subtasks and assign `MEDIUM` plan risk unless high-risk language
  is present.
- High-risk language sets `HIGH` plan risk and propagates `HIGH` risk to generated subtasks, preserving human approval
  boundaries.
- In-memory policy keyword priority now favors special goods and repair policies before generic return/quality matches.
- Evaluation tests assert improved V2.10 pass/fail bounds and high-risk approval expectation coverage.
- Default tests remain offline and deterministic.

### V2.10 不接受的退化

- 为通过评测读取或硬编码 `caseId`；
- 删除评测 case 或降低评测字段断言；
- 默认评测调用真实 LLM、OpenAI provider、外部网络或需要 API Key；
- Planner 或 handler 绕过 `AgentPlanValidator`、`ToolRegistry`、Approval、Trace 或 Workspace 边界；
- 执行真实退款、真实换货、真实优惠券补偿、支付变更、物流变更或争议关闭；
- 降低 ArchUnit、Checkstyle、SpotBugs 或 JUnit 约束。
