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

- Modular monolith package layout is in place under `io.github.tatame.aftersale`.
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

## V3 Quality Targets

V3 质量目标聚焦基础设施收口。当前 V3.1 已完成显式 MySQL profile 和 Spring JDBC persistence，V3.2 已完成
本地 Docker Compose 启动路径，V3.3 已完成 requestId 追踪和结构化日志基础能力，V3.5 已完成可选 demo
dataset enrichment，V3.6 已完成 order-item-aware order tool output，V3.7 已完成 item-specific return/exchange
recommendation。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| Persistence correctness | Ticket、AgentRun、ToolCallTrace、ApprovalRequest 在 MySQL profile 下可正确保存和查询 | Repository contract 测试 + opt-in integration test |
| Test profile stability | in-memory/test profile 保持离线、确定性、快速运行 | 默认 `mvn test` |
| Docker reproducibility | 本地 app + mysql 可按 README 复现启动 | Docker Compose smoke check |
| Logging traceability | requestId、ticketId、agentRunId、subtaskId、toolName、approvalRequestId 可用于日志关联 | 日志字段测试 + 手动检查 |
| Secret safety | 数据库密码、API Key、token 和敏感凭证不进入仓库 | 配置检查 + review |
| Backward compatibility with V2 demo | V2 ticket、AgentRun、approval、execution tree 和 evaluation demo 不退化 | 回归测试 + README demo |
| No regression of Agent boundaries | Agent/Handler 不访问 Repository，不绕过 ToolRegistry、Approval、Trace 或 Workspace | ArchitectureTest + 单元测试 |
| Seed reproducibility | generated seed 可由脚本以 bounded 参数复现 | Python script smoke + harness test |
| Raw data boundary | 本地 raw dataset 原始大文件不入仓 | `.gitignore` + review |
| Product/item demo support | `products` / `order_items` 支撑多商品售后 demo 数据 | schema / seed harness test |
| Order item tool traceability | `get_order_by_id` 输出和 AgentRun trace 可看到结构化 `orderItems` | OrderToolTest + AgentRunFlowTest |
| Item-specific recommendation | Return / Exchange handler 基于 workspace `orderItems` 生成商品明细级建议 | SpecialistAgentHandlerTest + AgentRunFlowTest |
| Dataset traceability | 三个公开数据集字段映射、清洗规则和未使用字段可追踪 | `docs/data/DATASET_MAPPING.md` |
| External data independence | 默认测试和默认启动不依赖 raw 数据集 | 默认 `mvn test` |

### V3.1 Current Status

Status: completed for explicit MySQL persistence profile.

Completed:

- Added `mysql` profile configuration using `AFTERSALE_MYSQL_URL`, `AFTERSALE_MYSQL_USERNAME`, and
  `AFTERSALE_MYSQL_PASSWORD`.
- Added Spring JDBC repositories for Ticket, AgentRun, ToolCallTrace, ApprovalRequest, Order, and AfterSalePolicy.
- Added `schema-mysql.sql` with the required core tables.
- Added `data-mysql.sql` with deterministic demo orders and after-sale policies.
- Preserved in-memory repositories for the default non-`mysql` profile.
- Added tests proving default Spring context has no `DataSource` and uses in-memory repositories.
- Added schema/seed harness tests checking required fields and avoiding committed real secrets.

Remaining V3 follow-up:

- Consider opt-in MySQL/Testcontainers integration tests without changing the default offline test path.

### V3.2 Current Status

Status: completed for local Docker Compose development startup.

Completed:

- Added `docker-compose.yml` with local `mysql` and `app` services.
- Added a Dockerfile that builds the Spring Boot jar and runs it on Java 17.
- MySQL container uses the V3.1 `schema-mysql.sql` and `data-mysql.sql` initialization scripts.
- App container starts with `SPRING_PROFILES_ACTIVE=mysql` and connects to the compose `mysql` service.
- Compose credentials are local placeholders with environment variable overrides, not real secrets.
- Redis is not enabled.
- Added offline harness tests for compose profile wiring, schema/seed mounting, Dockerfile presence, and secret safety.
- README documents start, health check, stop, cleanup, and default offline test behavior.

Remaining V3 follow-up:

- Keep Docker Compose smoke validation explicit and outside the default Maven test path.

### V3.3 Current Status

Status: completed for structured logging and basic observability.

Completed:

- Added `X-Request-Id` support for request correlation.
- Requests without `X-Request-Id` receive a generated response header.
- Requests with `X-Request-Id` receive the same value in the response header.
- Request-level MDC stores `requestId` during request handling and clears it after completion.
- Logging pattern includes `requestId`, `ticketId`, `agentRunId`, `subtaskId`, `toolName`, and
  `approvalRequestId`.
- Ticket creation, AgentRun execution, Specialist Handler execution, ToolRegistry calls, approval creation and
  decisions, and execution tree queries emit structured diagnostic logs.
- Added tests for generated request IDs, propagated request IDs, MDC cleanup, and required logging fields.
- No external observability platform, Docker, MySQL, Redis, real LLM, API Key, or network dependency is required by
  default tests.

Remaining V3 follow-up:

- Keep logs as diagnostics only; ToolCallTrace, ApprovalRequest records, and Execution Tree remain audit surfaces.
- Consider opt-in integration tests for MySQL or Docker Compose without joining the default Maven test path.

### V3.4 Final Quality Summary

Status: completed for V3 infrastructure closure review.

Current validation baseline:

- Test classes: 26 under `src/test/java/io/github/tatame/aftersale`.
- JUnit test methods: 130 discovered by the default Maven test run, with the live LLM smoke test skipped unless
  explicitly enabled.
- Architecture test methods: 11.
- ArchUnit rule checks: 15 `noClasses` boundary checks across API, domain, Agent, Tool, LLM infrastructure, Specialist
  Handler, Workspace, and Approval boundaries.
- Default validation commands remain:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Current infrastructure status:

- Default profile remains in-memory and offline.
- `mysql` profile is explicit opt-in and uses environment variables for database connection values.
- MySQL schema and deterministic seed data exist for Ticket, AgentRun, ToolCallTrace, ApprovalRequest, order demo data,
  and after-sale policy data.
- Docker Compose provides local app + MySQL startup with placeholder local credentials only.
- Structured logging supports `requestId`, `ticketId`, `agentRunId`, `subtaskId`, `toolName`, and
  `approvalRequestId`.
- Actuator health remains available.
- ToolCallTrace, ApprovalRequest records, and Execution Tree remain the audit and inspection surfaces.
- Default tests do not require MySQL, Docker, Redis, real LLMs, API keys, or external network.
- V2 demo boundaries remain compatible with V3 infrastructure profiles.

V3 final non-regression targets:

- No committed real secrets.
- No production claims for Docker Compose.
- No Controller direct Repository access.
- No Agent or Specialist Handler direct Repository access.
- No persistence bypass around ApplicationService, ToolRegistry, Approval, Trace, or Workspace boundaries.
- No log output of API keys, database passwords, full LLM prompts, sensitive credentials, or long raw text.

### V3.5 Data Quality Summary

Status: completed for optional demo dataset enrichment.

Current data infrastructure status:

- `schema-mysql.sql` contains `products` and `order_items` tables with foreign keys to existing order/product IDs.
- `data-mysql.sql` keeps the existing order and policy seed while adding minimal product and order-item rows.
- `scripts/data/build_demo_seed.py` uses the Python standard library to read UTF-8 / UTF-8-SIG CSV and basic XLSX files.
- Generated artifacts are small and reviewable under `data/generated`.
- Raw public datasets remain local in the gitignored raw dataset directory and are ignored by Git.
- `docs/data/DATASET_MAPPING.md` documents source usage, field mapping, status normalization, aftersale deadline rules,
  ignored fields such as `Age`, import steps, and limitations.
- Default `mvn test` does not require raw datasets, generated files from a fresh local run, MySQL, Docker, real LLMs,
  API keys, or external network.

V3.5 non-regression targets:

- Generated seed must remain optional enrichment for the explicit MySQL profile.
- Raw data files must not enter source control.
- `Age` must not be used for user profiling, ranking, segmentation, or personalization.
- Product/order-item seed must not force Agent main-flow changes or any real external order-center integration.
- Optional generated JSONL cases must not replace the default curated evaluation dataset without a separate plan.

### V3.6 Order Items Tool Quality Summary

Status: completed for order-item-aware order tool output.

Current order-item tool status:

- `OrderItem` is a pure domain model with no database framework dependency.
- `Order` now carries structured `orderItems` while preserving a compatibility constructor for existing callers.
- The explicit MySQL repository queries `order_items` joined with `products`, with a fallback primary item for older
  local seed data.
- The default in-memory repository seeds at least one item for every demo order.
- `get_order_by_id` returns `orderItems` with product, category, quantity, unit price, item status, return/exchange
  support flags, and special-item flag.
- `OrderFact` captures an item summary for workspace-based final suggestions.
- ToolCallTrace output JSON for `get_order_by_id` contains `orderItems`, so Execution Tree inspection can see the same
  serialized tool evidence.
- Default tests remain independent from MySQL, Docker, raw datasets, real LLMs, API keys, and external network.

V3.6 non-regression targets:

- Order tools remain low-risk read-only tools.
- Agent and Specialist Handler must still call order data only through ToolRegistry and application services.
- `orderItems` enrichment must not introduce real refund, exchange, payment, logistics, inventory, or coupon actions.
- MySQL order-item support must stay behind the explicit `mysql` profile.
- Default in-memory tests must continue to expose at least one item for `get_order_by_id`.

### V3.7 Item-Specific Recommendation Quality Summary

Status: completed for deterministic item-level return and exchange recommendations.

Current item recommendation status:

- `OrderItemFact` captures structured item facts in AgentWorkspace from `get_order_by_id` tool output.
- `ReturnAgentHandler` appends item-level return recommendations to subtask summary and Ticket note.
- `ExchangeAgentHandler` appends item-level exchange recommendations to subtask summary and Ticket note.
- Item matching is deterministic: product name first, then category, then coarse clothing keywords, then explicit
  fallback to the first item.
- Recommendations include order item ID, product ID, product name, category, return/exchange support flags,
  special-item flag, recommendation text, and reason.
- `supportReturn`, `supportExchange`, and `isSpecialItem` remain Java demo-rule derivations from current product and
  category data; V3.7 does not require new MySQL columns.
- Unsupported or special items route to policy/manual-review guidance and do not claim direct return or exchange can be
  executed.
- ToolCallTrace and Execution Tree are not structurally changed; tool evidence still comes through existing trace
  output and final summaries.
- Default tests remain independent from MySQL, Docker, raw datasets, real LLMs, API keys, and external network.

V3.7 non-regression targets:

- Handler must not directly access OrderRepository or any business Repository.
- Handler must continue to obtain order facts through ToolRegistry and workspace.
- Item-level recommendation must not execute real refund, exchange, inventory, logistics, payment, coupon, or dispute
  actions.
- Unsupported support flags and special-item flags must prevent direct return/exchange recommendation language.
- Fallback item selection must stay explicit in the recommendation reason.

### V3.8 Context Budget / Token Observability Quality Summary

Status: completed for deterministic LLM planner prompt budget controls.

Current context-budget status:

- LLM planner prompt input is split into typed sections rather than one uncontrolled concatenated string.
- Critical sections include `systemInstructions`, `outputSchema`, `plannerContractSummary`, `toolCatalogCompact`,
  `riskPolicySummary`, and `ticketContext`.
- Critical sections are not silently dropped or truncated to satisfy budget.
- Optional sections include `conversationHistory`, `ragContext`, `examples`, `debugHints`, `extendedPolicyText`, and
  `nonEssentialDocs`.
- Optional budget reduction follows the documented order: drop debug hints, reduce non-essential docs, truncate
  examples, truncate conversation history, compress RAG context, compress extended policy text, then compress
  non-critical ticket context fields.
- Tool catalog prompt content is compact and includes only tool name, risk level, required input fields, and short
  purpose.
- Token estimates use `max(1, chars / 4)` and avoid adding a tokenizer dependency.
- `LlmAgentPlanner` logs section token estimates, optional token drops, total input tokens, output budget, budget
  status, and budget action.
- Logs do not include full prompts, API keys, database passwords, sensitive credentials, or long raw documents.
- Sentinel phrase coverage verifies a long optional document is not fully inserted into the final prompt.
- Default tests remain independent from MySQL, Docker, raw datasets, real LLMs, API keys, and external network.

V3.8 non-regression targets:

- PromptFactory must keep budget application, telemetry calculation, and compact catalog construction in collaborators.
- `outputSchema`, `toolCatalogCompact`, and `riskPolicySummary` must not be removed to fit budget.
- Prompt budget errors must remain explicit and diagnosable.
- Provider output/cache token usage must not be fabricated when unavailable.
- Context-budget telemetry must remain diagnostic only and must not alter ToolRegistry, Approval, Trace, Workspace, or
  Agent execution semantics.

### V3.9 Real LLM + MySQL Seed Data Opt-In Validation Quality Summary

Status: completed for explicit live-only HTTP validation harness.

Current live-validation status:

- `RealAgentValidationLiveTest` is tagged `live` and disabled unless both `-Dlive.llm=true` and `-Dlive.mysql=true`
  are present.
- The live test is also disabled unless `OPENAI_API_KEY`, `AFTERSALE_MYSQL_URL`, `AFTERSALE_MYSQL_USERNAME`, and
  `AFTERSALE_MYSQL_PASSWORD` are present.
- The test uses the `mysql` profile and `agent.planner.mode=llm` only for the focused live class.
- The test drives the existing HTTP APIs: Ticket creation, AgentRun creation, Execution Tree lookup, and Trace lookup.
- Assertions focus on structure and boundaries rather than fixed LLM prose: run id, successful status, trace count,
  required tool names, `orderItems`, and item-level recommendation evidence.
- The test treats provider 403 / insufficient-balance responses as explicit live-provider setup failures.
- The OpenAI strict JSON schema now includes `subtasks`, matching the existing parser, validator, and specialist handler
  contract for multi-intent planning.
- Default tests remain independent from MySQL, Docker, raw datasets, real LLMs, API keys, and external network.

V3.9 non-regression targets:

- Default Maven validation must never load the MySQL/LLM live validation context.
- Live validation must continue using HTTP APIs rather than direct ApplicationService-only execution.
- Real LLM output may vary, so assertions should remain boundary-oriented and avoid long text equality.
- Live errors must not print API keys, database passwords, full prompt text, or personal paths.
- Tool execution must remain visible through ToolCallTrace and Execution Tree; the LLM must not execute tools directly.

### V3.10 DashScope Qwen LLM Provider Adapter Quality Summary

Status: completed for provider adapter and default-offline validation.

Current provider status:

- `openai-responses` remains the default live provider and uses the existing Responses client.
- `openai` remains accepted as a legacy alias for `openai-responses`.
- `dashscope-responses` uses `DASHSCOPE_API_KEY` and a DashScope responses-compatible endpoint.
- `dashscope-chat-compatible` uses `DASHSCOPE_API_KEY` and the OpenAI-compatible Chat Completions endpoint.
- Chat Completions request construction maps the planner system/user prompt into `messages`.
- Chat Completions response parsing reads `choices[0].message.content` and returns it as `LlmResponse` text.
- Provider errors include provider, endpoint host, model, status code, and a truncated sanitized body.
- Provider errors do not include API keys, database passwords, full prompts, or sensitive credentials.
- Live smoke and real validation paths are still explicit opt-in and skip when the selected provider key is absent.
- Default tests remain independent from MySQL, Docker, raw datasets, real LLMs, API keys, and external network.

V3.10 non-regression targets:

- Provider selection must not bypass `AgentPlanParser` or `AgentPlanValidator`.
- DashScope adapters must not let the LLM execute tools directly.
- `ToolRegistry`, Approval, Trace, Workspace, and specialist handler semantics must remain unchanged.
- Model/endpoint mismatch errors should stay visible as provider configuration errors, not business logic failures.
- No real `DASHSCOPE_API_KEY`, `OPENAI_API_KEY`, database password, or personal path may enter the repository.

### V3 不接受的退化

- 删除 in-memory/test profile；
- 默认测试依赖本地 MySQL、Docker、Redis、真实 LLM、API Key 或外部网络；
- 真实数据库密码或 API Key 出现在代码、测试、README、docs、配置或提交历史；
- Docker Compose 被写成生产部署方案；
- Controller 直接访问 Repository；
- Agent 或 Specialist Handler 直接访问 Repository；
- persistence 绕过 ApplicationService、ToolRegistry、Approval、Trace 或 Workspace 边界；
- 日志输出敏感凭证、完整长 prompt 或 LLM 原始长文本；
- 降低 ArchUnit、Checkstyle、SpotBugs 或 JUnit 约束。

## V4 Quality Targets

V4 质量目标聚焦 RAG、Spring AI、Tool / Skill 能力层和 Spring Boot 完整性。V4 不代表真实退款、真实换货、真实支付、真实物流、真实优惠券补偿或生产级权限系统已经实现。

| 维度 | 当前目标 | 验收方式 |
|---|---|---|
| Spring AI 接入质量 | Spring AI ChatClient / EmbeddingModel 通过 adapter 接入，不破坏 LlmClient / EmbeddingClient 边界 | 配置测试 + fake client 测试 + live opt-in smoke |
| RAG 检索质量 | KEYWORD / VECTOR / HYBRID 检索返回结构化 policy evidence | RAG retrieval tests |
| Vector Store 边界 | PGvector profile 显式 opt-in，默认测试不依赖 PostgreSQL / Docker | profile tests + architecture tests |
| Policy Ingestion | 文档可 chunk、去重、embedding、入库，并记录 ingestion run | ingestion service tests |
| Tool / Skill 分层 | Tool 是原子能力，Skill 是复合任务能力，Skill 必须通过 ToolRegistry 调 Tool | SkillRegistry tests + ArchUnit |
| Agent 集成质量 | RAG evidence 进入 ToolCallTrace、AgentWorkspace 和 Execution Tree | AgentRun flow tests |
| Evaluation | 默认离线评测包含 evidence recall、citation completeness、no fabrication、skill selection | Evaluation tests |
| Spring Boot 完整性 | ConfigurationProperties、migration、HealthIndicator、OpenAPI、minimal Security | config/API/security tests |
| 默认测试确定性 | 默认 mvn test 不依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或外部网络 | default validation commands |
| 风险边界 | RAG 不执行业务动作，Skill 不绕过 Approval，高风险动作仍需人工确认 | unit tests + risk policy tests |

### V4 Current Status

Status: completed. V4.0 pre-flight fixes, V4.1 Tool / Skill Layer Foundation, V4.2 Spring AI Adapter, V4.3 PGvector
profile / schema / fake vector store / compose docs, V4.4 Policy Ingestion Foundation, V4.5 Hybrid RAG Policy Search
Tool, V4.6 evaluation / demo / Actuator health / OpenAPI docs, and V4.7 documentation / architecture / final closure
are completed. V4 final closure is recorded in `version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md` and
summarized in `version-updates/V4_RELEASE_SUMMARY.md`.

### V4.0 Pre-flight Fixes Quality Summary

Status: completed for AgentRun boundary alignment before Spring AI / RAG implementation.

Current pre-flight status:

- AgentRun planning context now uses a dedicated executable tool policy instead of the full ToolRegistry catalog.
- AgentPlan validation rejects registered but currently non-executable tools before tool execution begins.
- LLM prompt tool catalog follows the AgentRun allowed tool set and does not expose tools without current AgentRun
  input mapping.
- Current AgentRun executable tools are `get_order_by_id`, `search_aftersale_policy`, and `add_ticket_note`.
- Specialist handlers check the same allowed tool set before executing planned tools.
- Evaluation planning uses the same AgentRun executable tool policy as the runtime path.
- AgentRun failures that leave the ticket in `CREATED` or `AGENT_RUNNING` mark the Ticket `FAILED` with a sanitized
  failure summary.
- Human approval status is preserved and not treated as an AgentRun failure.
- Comment cleanup removed didactic Spring/Java explanations and kept boundary-focused comments.
- No Spring AI, RAG, PGvector, VectorStore, Policy Ingestion, or SkillRegistry implementation was added in V4.0.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.0 non-regression targets:

- ToolRegistry remains the only tool execution entry point.
- LLM / Planner still only returns structured plans and never executes tools directly.
- Approval, Trace, Workspace, Planner, and Specialist Handler core semantics remain unchanged.
- Registered API/future-skill tools must not be exposed to AgentRun Planner until AgentRun has explicit input mapping
  and execution support.
- Failed AgentRuns must not leave tickets indefinitely in `AGENT_RUNNING`.

### V4.1 Tool / Skill Layer Foundation Quality Summary

Status: completed for Skill foundation and Specialist Handler compatibility.

Current Skill foundation status:

- `AgentSkill`, `SkillDefinition`, `SkillExecutionContext`, `SkillExecutionResult`, `SkillExecutionStatus`, and
  `SkillExecutionException` define the first Java Skill contract.
- `SkillRegistry` indexes unique skills by `skillName` and returns deterministic candidate lists by `SubtaskType`.
- Duplicate `skillName` values fail at registry construction with a clear error.
- `SpecialistHandlerSkillAdapter` exposes current Specialist Handler behavior as Skill-compatible execution without
  changing the AgentRun happy path.
- Registered Skill definitions cover Return, Exchange, Coupon, Logistics, General Consultation, and Human Escalation.
- `SkillRiskEvaluator` validates that Skill risk is not lower than the highest risk among required tools.
- Skill architecture rules prevent direct Repository, Spring Web, LLM infrastructure, Spring AI, vector/RAG
  infrastructure, and concrete `ToolExecutor` dependencies.
- `AgentApplicationService` still dispatches through `SpecialistAgentHandlerRegistry`; V4.1 does not complete
  SkillRegistry runtime migration.
- `plannedSkills` remains documented as a future extension and is not parsed or executed in V4.1.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.1 non-regression targets:

- Skill must remain a composite capability layer, not a replacement for ToolRegistry.
- Skill or adapter execution must keep actual tool calls behind ToolRegistry and ToolCallTrace.
- Skill must not directly access Repository, VectorStore, JdbcTemplate, Spring AI ChatClient, EmbeddingModel, LlmClient,
  or external business systems.
- HIGH-risk Skill behavior must still stop at Approval or human-routing boundaries and must not execute real business
  actions.

### V4.2 Spring AI Adapter Quality Summary

Status: completed for Spring AI chat and embedding adapter foundation.

Current Spring AI adapter status:

- `spring-ai-chat` is a supported LLM provider value and selects `SpringAiLlmClient`.
- OpenAI Responses, DashScope Responses, and DashScope Chat-compatible providers remain available.
- Spring AI chat is isolated behind `LlmClient`; provider output still enters `AgentPlanParser` and
  `AgentPlanValidator`.
- Spring AI `ChatClient` is not exposed to Agent, Handler, Skill, ToolRegistry, Repository, Controller, or domain code.
- `EmbeddingClient`, `FakeEmbeddingClient`, and `SpringAiEmbeddingClient` define the embedding provider boundary for
  later RAG stages.
- V4.2 does not create VectorStore, PGvector schema, policy ingestion, similarity search, or HYBRID retrieval runtime.
- Spring AI model auto-configuration defaults to `none` for chat, embedding, audio, image, and moderation unless a live
  operator explicitly enables the relevant model type.
- Provider error formatting is sanitized and must not include API keys, full prompts, database passwords, or tokens.
- Spring AI live smoke tests are explicit opt-in and do not create tickets, AgentRuns, traces, vector stores, or
  database rows.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.2 non-regression targets:

- Spring AI must not register project tools as provider-side function/tool callbacks.
- ToolRegistry remains the only project tool execution entry point.
- Skill and Handler code must not directly depend on Spring AI ChatClient, ChatModel, EmbeddingModel, or Spring AI
  adapters.
- Embedding adapter output is only a vector response boundary in V4.2; it is not a vector store or retrieval system.

### V4.3.1 PostgreSQL / PGvector Profile Boundary Quality Summary

Status: completed for dependency and profile boundary only.

Current PGvector boundary status:

- PostgreSQL JDBC driver is present as a runtime dependency for later opt-in PGvector work.
- Default configuration exposes `agent.rag.vector-store.pgvector.*` as disabled properties.
- `rag-postgres` is an explicit opt-in profile and is separate from the V3 `mysql` profile.
- `PgVectorProperties` and `PgVectorProfileGuard` validate required opt-in settings without creating a real
  PostgreSQL `DataSource`, `JdbcTemplate`, Spring AI `VectorStore`, schema, repository, or connection.
- Missing required PGvector configuration fails clearly and does not print the database password.
- Default profile tests verify no PostgreSQL / PGvector / VectorStore bean is created.
- MySQL non-regression tests verify PGvector properties do not pollute `application-mysql.yml`.
- Architecture tests prevent Agent, Handler, and Skill layers from depending directly on PGvector, VectorStore,
  `DataSource`, or `JdbcTemplate`.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.3.1 non-regression targets:

- No policy schema, migration, repository, VectorStore search, Policy Ingestion, RAG runtime, or Docker Compose
  PGvector service is implemented in V4.3.1.
- `search_aftersale_policy`, AgentRun, Skill runtime, ToolRegistry, Approval, Trace, and Workspace semantics remain
  unchanged.
- V4.3.2 must handle vector schema and repository contract before any PGvector-backed retrieval is exposed.

### V4.3.2 Vector Schema / Repository Contract Quality Summary

Status: completed for schema file and pure repository contract only.

Current vector schema / contract status:

- `schema-rag-postgres.sql` defines `policy_documents`, `policy_chunks`, and `policy_embeddings` with PGvector
  extension setup, primary keys, foreign keys, unique constraints, and retrieval-oriented indexes.
- The schema file is not referenced by the default profile and is not auto-loaded by default tests.
- `policy.rag.domain` contains pure Java records for documents, chunks, embeddings, vector search queries, results, and
  matches.
- `PolicyVectorRepository` is an interface-only contract and has no JDBC, `DataSource`, `JdbcTemplate`, PGvector, or
  Spring AI dependency.
- Schema harness tests check required DDL content and secret safety without connecting to PostgreSQL.
- Domain tests cover vector validation, topK / score bounds, and evidence-oriented result wording.
- Repository contract tests use a test-scope fake and do not create a production fake vector store.
- Architecture tests keep `policy.rag.domain` free of Spring / JDBC / Spring AI dependencies and prevent Agent,
  Handler, and Skill layers from depending directly on `PolicyVectorRepository`.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.3.2 non-regression targets:

- No JDBC repository, Spring AI `VectorStore` search, EmbeddingClient call, Policy Ingestion, RAG / HYBRID retrieval,
  or Docker Compose PGvector service is implemented in V4.3.2.
- `search_aftersale_policy`, AgentRun, Skill runtime, ToolRegistry, Approval, Trace, and Workspace semantics remain
  unchanged.
- V4.3.3 must handle fake vector store / default offline vector tests before any retrieval runtime is exposed.

### V4.3.3 Fake Vector Store / Default Offline Vector Tests Quality Summary

Status: completed for fake vector repository and offline vector contract tests only.

Current fake vector store status:

- `CosineSimilarityCalculator` provides deterministic cosine similarity for retrieval evidence scoring.
- `InMemoryPolicyVectorRepository` implements the `PolicyVectorRepository` save / find / search contract without
  PostgreSQL, PGvector, JDBC, Spring AI `VectorStore`, or `EmbeddingClient`.
- Search supports ranking by score, `topK`, `minScore`, category, productType, effectiveAt, and embeddingModel filters.
- Empty searches return a structured empty result without fabricated evidence or business action completion wording.
- Duplicate document / chunk / embedding writes are rejected with clear errors.
- The fake provider bean is opt-in via `agent.rag.vector-store.provider=fake` and does not create `DataSource`,
  `JdbcTemplate`, Spring AI `VectorStore`, real LLM, or real embedding provider beans.
- Architecture tests keep fake vector infrastructure away from JDBC, Spring AI, business repositories, Tool, Handler,
  and Skill packages.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.3.3 non-regression targets:

- No JDBC repository, PGvector live search, Spring AI `VectorStore` search, EmbeddingClient call, Policy Ingestion,
  RAG / HYBRID retrieval, or Docker Compose PGvector service is implemented in V4.3.3.
- `search_aftersale_policy`, AgentRun, Skill runtime, ToolRegistry, ToolCallTrace, Execution Tree, Approval, and
  Workspace semantics remain unchanged.
- V4.3.4 must handle Docker Compose / opt-in integration docs; V4.4 must handle Policy Ingestion; V4.5 must connect
  HYBRID retrieval to `search_aftersale_policy`.

### V4.3.4 Docker Compose / Opt-in PGvector Integration Docs Quality Summary

Status: completed for local development compose and documentation only.

Current PGvector local setup status:

- `docker-compose-rag.yml` defines an independent local development only PGvector PostgreSQL service.
- The compose file uses placeholder local credentials, a dedicated named volume, a healthcheck, and an initdb mount for
  `schema-rag-postgres.sql`.
- `.env.rag.example` documents `rag-postgres` and PGvector environment variables with placeholder values only.
- `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md` documents startup, stop, cleanup, schema initialization, health checks, default
  test isolation, and common local issues.
- Default `docker-compose.yml` remains the V3 app + MySQL path and is not polluted by PGvector dependencies.
- Compose/docs harness tests verify file presence, secret safety, default compose non-regression, and non-goal wording.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.3.4 non-regression targets:

- No `JdbcPolicyVectorRepository`, PGvector live search, Spring AI `VectorStore` search, EmbeddingClient call,
  Policy Ingestion, RAG / HYBRID retrieval, or app default PGvector connection is implemented in V4.3.4.
- `search_aftersale_policy`, AgentRun, Skill runtime, ToolRegistry, ToolCallTrace, Execution Tree, Approval, and
  Workspace semantics remain unchanged.
- PGvector compose is local development only and must not be represented as production deployment.
- V4.4 must handle Policy Ingestion; V4.5 must connect HYBRID retrieval to `search_aftersale_policy`.

### V4.4.1 Policy Ingestion Domain Model Quality Summary

Status: completed for ingestion domain, status, repository contract, and offline memory persistence foundation only.

Current ingestion foundation status:

- `PolicyIngestionRun`, `PolicyIngestionSource`, `PolicyIngestionDocument`, `PolicyIngestionChunk`, and
  `PolicyIngestionError` are pure domain records with validation.
- `PolicyIngestionStateMachine` defines allowed transitions from CREATED through RUNNING, CHUNKED, EMBEDDING, and
  terminal statuses. COMPLETED, FAILED, PARTIALLY_FAILED, and CANCELLED are terminal.
- `PolicyIngestionRepository` is an interface-only contract for run, document, chunk, and error persistence.
- `InMemoryPolicyIngestionRepository` supports default offline save/find/update behavior and rejects duplicate
  document, chunk, and error IDs.
- Sanitization tests cover API key, password, token, prompt, local path, and long raw text boundaries for error details.
- Architecture tests keep ingestion domain free of Spring / JDBC / Spring AI dependencies and keep Agent, Handler, and
  Skill layers away from ingestion repositories and memory infrastructure.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.4.1 non-regression targets:

- No chunking service, checksum dedup service, EmbeddingClient call, PolicyVectorRepository write,
  JdbcPolicyIngestionRepository, JdbcPolicyVectorRepository, Admin Controller, ingestion tool, RAG / HYBRID retrieval,
  or `search_aftersale_policy` behavior change is implemented in V4.4.1.
- Policy Ingestion remains an admin / pipeline capability and is not registered as an Agent runtime tool.
- `search_aftersale_policy`, AgentRun, Skill runtime, ToolRegistry, ToolCallTrace, Execution Tree, Approval, and
  Workspace semantics remain unchanged.
- V4.4.2 must handle chunking and checksum dedup; V4.4.3 must handle embedding pipeline with fake provider; V4.5 must
  connect HYBRID retrieval to `search_aftersale_policy`.

### V4.4.2 Chunking and Checksum Dedup Quality Summary

Status: completed for deterministic chunking, checksum, dedup service, and offline tests only.

Current chunking / checksum / dedup status:

- `PolicyChunkingOptions` validates max chunk size, overlap, max chunks, token estimate divisor, and min chunk size.
- `PolicyChunkingService` creates deterministic `PolicyIngestionChunk` records from `PolicyIngestionDocument.rawText`
  with chunk index starting at 0, overlap support, paragraph-boundary preference, token estimates, and chunk checksums.
- Max chunk overflow fails clearly without echoing complete raw text.
- `PolicyContentChecksumService` uses Java standard-library SHA-256 with line-ending normalization and trim.
- `PolicyIngestionDedupService` returns `NEW_CONTENT`, `DUPLICATE_DOCUMENT`, or `DUPLICATE_CHUNK` using checksum
  queries on `PolicyIngestionRepository`.
- `InMemoryPolicyIngestionRepository` supports document and chunk checksum lookup without database access.
- Tests cover chunking options, blank rawText, short/long chunking, overlap, paragraph boundaries, overflow, checksum
  determinism, dedup decisions, repository checksum queries, and architecture boundaries.
- Default tests remain independent from real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, and external
  network.

V4.4.2 non-regression targets:

- No EmbeddingClient call, Spring AI call, PolicyVectorRepository write, JdbcPolicyIngestionRepository,
  JdbcPolicyVectorRepository, Admin Controller, ingestion tool, RAG / HYBRID retrieval, or
  `search_aftersale_policy` behavior change is implemented in V4.4.2.
- Policy Ingestion remains an admin / pipeline capability and is not registered as an Agent runtime tool.
- `search_aftersale_policy`, AgentRun, Skill runtime, ToolRegistry, ToolCallTrace, Execution Tree, Approval, and
  Workspace semantics remain unchanged.
- V4.4.3 must handle embedding pipeline with fake provider; V4.5 must connect HYBRID retrieval to
  `search_aftersale_policy`.

### V4.4.3 Embedding Pipeline with Fake Provider Quality Summary

Status: completed for fake-provider embedding pipeline and offline tests only.

Current embedding pipeline status:

- `PolicyEmbeddingPipelineOptions`, `PolicyEmbeddingPipelineResult`, and `PolicyEmbeddingPipelineFailure` define the
  fake-provider pipeline control and result shape.
- `PolicyEmbeddingPipelineService` reads ingestion runs, documents, and chunks from `PolicyIngestionRepository`, calls
  the `EmbeddingClient` abstraction, and writes vector records through `PolicyVectorRepository`.
- Default tests use `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`; they do not call
  `SpringAiEmbeddingClient`, Spring AI `EmbeddingModel`, Spring AI `VectorStore`, PostgreSQL, PGvector, Docker, MySQL,
  Redis, real LLMs, API keys, or external network.
- Tests cover happy path vector writes, direct repository search after embedding, dimension mismatch handling,
  duplicate embedding skip/fail behavior, partial failure, all failure, run readiness, chunk limit, and sanitization.
- Pipeline failures redact credentials, local paths, prompts, and complete chunk content from result and persisted
  ingestion errors.
- Architecture tests allow ingestion application code to depend on `EmbeddingClient` abstraction and
  `PolicyVectorRepository` contract while forbidding Spring AI adapters/classes, Spring AI `VectorStore`, JDBC,
  `DataSource`, PGvector infrastructure, vector memory infrastructure, business repositories, Tool, Handler, and Skill
  dependencies.

V4.4.3 non-regression targets:

- No real Spring AI embedding call, `SpringAiEmbeddingClient` default test path, Spring AI `VectorStore`,
  JdbcPolicyIngestionRepository, JdbcPolicyVectorRepository, PostgreSQL / PGvector connection, Admin Controller,
  ingestion tool, RAG / HYBRID retrieval, or `search_aftersale_policy` behavior change is implemented in V4.4.3.
- Policy Ingestion remains an admin / pipeline capability and is not registered as an Agent runtime tool.
- `search_aftersale_policy`, AgentRun, Skill runtime, ToolRegistry, ToolCallTrace, Execution Tree, Approval, and
  Workspace semantics remain unchanged.
- V4.4.4 must handle ingestion docs / final V4.4 completion record; V4.5 must connect HYBRID retrieval to
  `search_aftersale_policy`.

### V4.4 Policy Ingestion Foundation Quality Summary

Status: completed for V4.4 ingestion foundation, documentation closeout, and offline harness coverage.

Current V4.4 quality status:

- Ingestion domain quality: V4.4.1 models ingestion run/source/document/chunk/error state with validation and sanitized
  error text.
- State transition correctness: `PolicyIngestionStateMachine` locks terminal states and validates CREATED, RUNNING,
  CHUNKED, EMBEDDING, COMPLETED, PARTIALLY_FAILED, FAILED, and CANCELLED transitions.
- Chunking determinism: V4.4.2 deterministic chunking uses bounded character windows, overlap, paragraph-boundary
  preference, chunk index from 0, and simple token estimates.
- Checksum/dedup determinism: SHA-256 checksum uses Java standard library with documented normalization, and dedup
  returns deterministic `NEW_CONTENT`, `DUPLICATE_DOCUMENT`, or `DUPLICATE_CHUNK` decisions.
- Fake embedding pipeline offline correctness: V4.4.3 uses `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`
  in default tests, then verifies repository writes and direct repository search without live infrastructure.
- Sanitized error handling: ingestion and embedding failures must not include complete raw text, complete chunk content,
  API keys, passwords, tokens, local paths, full prompts, or provider secrets.
- Default test boundary: default validation does not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs,
  API keys, real embedding providers, or external network.
- Architecture boundary: ingestion remains an admin / pipeline capability, not an Agent runtime tool, and Agent,
  Handler, Skill, and ToolRegistry runtime semantics remain unchanged.

Known limitations:

- No Admin Controller, no `ingest_policy_document` tool, no ToolRegistry wiring, no real Spring AI embedding default
  path, no `JdbcPolicyIngestionRepository`, no `JdbcPolicyVectorRepository`, no PGvector live writes, no RAG / HYBRID
  retrieval in the V4.4 ingestion stage itself, and no Agent runtime vector retrieval in V4.4 itself.

V4.5 follow-up:

- V4.5 connects controlled HYBRID retrieval to `search_aftersale_policy` while preserving LOW-risk read-only evidence
  semantics and ToolRegistry / ToolCallTrace boundaries.

### V4.5.1 RAG Search Contract Quality Summary

Status: completed for RAG search contract, retrieval mode, evidence model, mapper preparation, and offline harness
coverage.

Current V4.5.1 quality status:

- Retrieval mode quality: `RetrievalMode` defines KEYWORD, VECTOR, and HYBRID, defaults missing values to KEYWORD, and
  fails unknown modes clearly.
- Query contract quality: `RagPolicySearchQuery` validates query text, bounded topK, minScore range, optional filters,
  and default retrieval mode without executing retrieval.
- Evidence model quality: `RagPolicyEvidence` validates required snippet, score range, retrieval mode, source, and
  metadata safety. Evidence is evidence only and must not claim completed refunds, exchanges, compensation, or dispute
  closure.
- Mapper quality: keyword and vector mappers convert supplied `PolicySearchResult` and `VectorSearchResult` values
  without repository access, EmbeddingClient calls, Spring AI calls, VectorStore calls, or PGvector connections.
- Tool contract preparation quality: docs define future `search_aftersale_policy` input/output schema with
  `retrievalMode`, evidence IDs, scores, and source markers, while explicitly stating V4.5.1 is schema preparation only.
- Runtime isolation quality: V4.5.1 does not change `search_aftersale_policy` runtime, ToolRegistry execution,
  ToolCallTrace output, AgentWorkspace writes, AgentRun flow, Skill runtime, or Execution Tree behavior.
- Default test boundary: default validation does not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs,
  API keys, real embedding providers, Spring AI provider calls, or external network.
- Architecture boundary: RAG search contracts stay free of Spring Web, JDBC, `DataSource`, Spring AI, VectorStore,
  PGvector infrastructure, and repository dependencies. Agent, Handler, and Skill layers do not depend on V4.5.1
  search-preparation models.

Known limitations:

- No keyword + vector merge service, HYBRID retrieval runtime, `PolicyVectorRepository.search` call, `EmbeddingClient`
  call, PGvector live search, ToolCallTrace evidence wiring, or Workspace evidence wiring is implemented in V4.5.1.

V4.5 follow-up:

- V4.5.2 implements keyword + vector merge service.
- V4.5.3 wires `search_aftersale_policy` to HYBRID mode while preserving LOW-risk read-only semantics.
- V4.5.4 wires ToolCallTrace / Workspace evidence output.

### V4.5.2 Keyword + Vector Merge Service Quality Summary

Status: completed for pure keyword + vector merge service, merge options, deterministic scoring, dedup, fallback,
offline tests, docs harness, and architecture boundary coverage.

Current V4.5.2 quality status:

- Merge options quality: `RagPolicyEvidenceMergeOptions` validates bounded topK, minScore, non-negative weights,
  non-zero weight sum, tie preference, dedup flags, and include flags.
- Score merge quality: `RagPolicyEvidenceMergeService` uses deterministic weighted average scoring, normalizes scores
  to 0.0-1.0, sorts descending by score, and keeps keywordScore / vectorScore as retrieval evidence scores.
- Dedup quality: merge behavior supports chunkId, policyId, and normalized snippet dedup without fabricating
  documentId, chunkId, or policyId.
- Fallback quality: keyword-only, vector-only, both-empty, and null input cases return clear HYBRID results without
  inventing evidence.
- Runtime isolation quality: V4.5.2 does not change `search_aftersale_policy` runtime, ToolRegistry execution,
  ToolCallTrace output, AgentWorkspace writes, AgentRun flow, Skill runtime, or Execution Tree behavior.
- Default test boundary: default validation does not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs,
  API keys, real embedding providers, Spring AI provider calls, or external network.
- Architecture boundary: merge service stays free of Spring Web, JDBC, `DataSource`, Spring AI, VectorStore,
  PGvector infrastructure, repository dependencies, and `EmbeddingClient`. Agent, Handler, and Skill layers do not
  depend on the merge service.

Known limitations:

- No `search_aftersale_policy` HYBRID runtime wiring, `PolicyVectorRepository.search` call, `EmbeddingClient` call,
  PGvector live search, ToolCallTrace evidence wiring, or Workspace evidence wiring is implemented in V4.5.2.

V4.5 follow-up:

- V4.5.3 wires `search_aftersale_policy` to HYBRID mode while preserving LOW-risk read-only semantics.
- V4.5.4 wires ToolCallTrace / Workspace evidence output.

### V4.5.3 search_aftersale_policy HYBRID Runtime Quality Summary

Status: completed for KEYWORD / VECTOR / HYBRID runtime wiring in `search_aftersale_policy`, input compatibility,
fake-provider vector runtime tests, fallback behavior, docs harness, and architecture boundary coverage.

Current V4.5.3 quality status:

- Input compatibility quality: old input without `retrievalMode` defaults to KEYWORD; invalid mode, topK, and minScore
  return clear LOW-risk tool failures without sensitive details.
- KEYWORD quality: KEYWORD mode keeps existing deterministic keyword policy retrieval behavior and does not call
  `EmbeddingClient` or `PolicyVectorRepository`.
- VECTOR quality: VECTOR mode uses `EmbeddingClient` abstraction plus `PolicyVectorRepository.search` contract and is
  covered by fake embedding + in-memory vector repository tests.
- HYBRID quality: HYBRID mode combines keyword and vector evidence through `RagPolicyEvidenceMergeService`, preserves
  keywordScore / vectorScore, and falls back to keyword evidence when vector dependencies or vector execution fail.
- Tool boundary quality: `search_aftersale_policy` remains LOW-risk read-only, approval-free, and executable through
  ToolRegistry; Handler and Agent paths keep using the tool boundary.
- Evidence-only quality: output evidence and scores are policy retrieval evidence only and do not execute or claim
  completed refund, exchange, coupon compensation, payment, logistics, or dispute-closure actions.
- Default test boundary: default validation uses fake embedding and in-memory vector repository where vector runtime is
  tested and does not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, API keys, real embedding
  providers, Spring AI provider calls, or external network.
- Architecture boundary: search tool runtime and RAG application service depend only on abstractions/contracts, not
  Spring AI provider classes, Spring AI `VectorStore`, JDBC, `DataSource`, PGvector infrastructure, or vector
  repository implementations. Agent, Handler, and Skill layers do not directly depend on vector or embedding
  infrastructure.

Known limitations:

- No ToolCallTrace schema change, AgentWorkspace evidence write change, Execution Tree evidence node, live PGvector
  search, `JdbcPolicyVectorRepository`, real embedding default path, Admin Controller, or ingestion tool is implemented
  in V4.5.3.

V4.5 follow-up:

- V4.5.4 wires ToolCallTrace / Workspace evidence output.

### V4.5.4 ToolCallTrace / Workspace Evidence Quality Summary

Status: completed for RAG evidence observability, ToolCallTrace output JSON stability, AgentWorkspace policy evidence
summary mapping, AgentRun final summary visibility, Execution Tree read-only evidence nodes, docs harness coverage, and
architecture boundary coverage.

Current V4.5.4 quality status:

- ToolCallTrace evidence quality: `search_aftersale_policy` output preserves legacy `results` while exposing stable
  `evidences`, `retrievalMode`, `fallbackUsed`, `totalKeywordMatches`, and `totalVectorMatches` fields in output JSON.
- Workspace evidence quality: `AgentWorkspace.PolicyEvidence` stores single-run evidence summaries with evidenceId,
  policyId, documentId, chunkId, documentTitle, productType, score, retrievalMode, and source when available.
- Final summary quality: AgentRun final summary includes concise policy evidence summaries and avoids full evidence
  JSON, full chunk content, long raw text, and business-action completion claims.
- Execution Tree quality: read-only Execution Tree output can show policy evidence summaries and safely degrades if
  tool output JSON parsing fails.
- Evidence-only quality: RAG evidence and scores remain retrieval evidence only and do not execute or claim completed
  refund, exchange, coupon compensation, payment, logistics, or dispute-closure actions.
- Default test boundary: default validation uses fake / in-memory dependencies where vector evidence is exercised and
  does not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, API keys, real embedding providers, Spring AI
  provider calls, or external network.
- Architecture boundary: Workspace mapping may depend on RAG model shapes but not RAG infrastructure. Execution Tree
  does not depend on `EmbeddingClient`, `PolicyVectorRepository`, PGvector infrastructure, Spring AI, JDBC,
  `DataSource`, or repository implementations. Agent, Handler, and Skill layers do not directly access vector or
  embedding infrastructure.

Known limitations:

- V4.5.4 does not change retrieval algorithms, implement live PGvector search, implement `JdbcPolicyVectorRepository`,
  add Admin Controller, add ingestion tool, add Skill runtime migration, or make real embedding providers a default
  test path.

V4.6 follow-up:

- V4.6 can continue evaluation, demo, Spring Boot completeness, or Skill-layer integration work without treating RAG
  evidence as a business action.

### V4.6.1 RAG Evaluation Cases and Metrics Quality Summary

Status: completed for offline deterministic RAG policy evidence evaluation dataset, metrics, runner, tests, docs, and
architecture boundary coverage.

Current V4.6.1 quality status:

- Dataset quality: `docs/evaluation/rag_policy_cases.jsonl` contains 15 reviewable JSONL cases covering KEYWORD,
  VECTOR, HYBRID, return, exchange, refund-only, logistics, coupon, special goods, repair / quality, unsupported
  query, empty evidence, vector-only evidence, keyword fallback, hybrid dedup, low-score filtering, and evidence-only
  safety.
- Loader quality: `RagEvaluationDatasetLoader` validates malformed JSONL with line numbers and tests dataset uniqueness,
  expected-field completeness, legal retrievalMode values, and secret / local-path safety.
- Fixture quality: `RagEvaluationFixture` builds deterministic fake / in-memory keyword and vector evidence without
  reading raw datasets or using external providers.
- Runner quality: `RagEvaluationApplicationService` calls the RAG search application boundary directly and does not
  create Ticket, AgentRun, ToolCallTrace, AgentWorkspace, or Execution Tree state.
- Metrics quality: report metrics include passRate, evidenceRecallPassRate, evidenceSourcePassRate,
  retrievalModePassRate, fallbackAccuracy, emptyResultAccuracy, citationCompletenessRate, safetyPassRate, and
  averageEvidenceCount.
- Evaluation boundary: V4.6.1 evaluates policy evidence retrieval, while V2.9 evaluation continues to evaluate Agent
  planner behavior and plan validity.
- Default test boundary: default validation uses `FakeEmbeddingClient`, `InMemoryPolicyVectorRepository`, and
  in-memory keyword policy data. It does not require PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, API keys,
  real embedding providers, Spring AI provider calls, raw datasets, or external network.
- Architecture boundary: RAG evaluation stays away from Spring Web, JDBC, `DataSource`, PGvector infrastructure,
  Spring AI `VectorStore`, ToolCallTrace writes, Workspace writes, Execution Tree writes, and Agent / Handler / Skill
  runtime dependencies.

Known limitations:

- V4.6.1 does not implement a V4 demo script, Actuator health indicators, OpenAPI / API docs polish, live PGvector
  evaluation, LLM-as-judge, semantic grading, or new runtime retrieval behavior.

V4.6 follow-up:

- V4.6.2 completed the V4 RAG demo script.
- V4.6.3 handles Actuator health indicators.
- V4.6.4 handles OpenAPI / API docs polish.

### V4.6.2 V4 RAG Demo Script Quality Summary

Status: completed for V4 RAG demo script / expected output / docs harness.

Current V4.6.2 quality status:

- Demo script quality: `docs/demo/V4_RAG_DEMO_SCRIPT.md` documents the local interview / project review demo across
  startup, HYBRID policy evidence, AgentRun, ToolCallTrace, AgentWorkspace, Execution Tree, and RAG evaluation.
- Expected output quality: snippets are short and show `retrievalMode`, `evidences`, `source`, retrieval score,
  keyword/vector scores, fallbackUsed, trace output JSON, workspace evidence, execution tree evidence, and evaluation
  metrics without long raw text.
- Offline boundary: the default V4 RAG demo does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL,
  Redis, real embedding providers, or external network.
- Evidence-only boundary: the demo states that `search_aftersale_policy` remains LOW-risk read-only and RAG evidence
  does not execute refund, exchange, compensation, payment, logistics, inventory, coupon issuance, or dispute closure.
- Evaluation demo boundary: Scenario D links V4.6.1 RAG evaluation, deterministic metrics, fake / in-memory
  dependencies, and the no LLM-as-judge boundary.
- Docs harness quality: `RagDemoDocsHarnessTest` verifies demo content, README and evaluation links, completion record,
  V4.6.2 no-runtime boundary, optional live path references, and secret / local-path safety.

Known limitations:

- V4.6.2 does not add runtime behavior, a direct public tool execution endpoint, Actuator health indicators, OpenAPI
  docs, live PGvector demo automation, or live provider validation.

V4.6 follow-up:

- V4.6.3 completed Actuator health indicators.
- V4.6.4 handles OpenAPI / API docs polish.

### V4.6.3 Actuator Health Indicators Quality Summary

Status: completed for offline RAG readiness diagnostics.

Current V4.6.3 quality status:

- Health indicator quality: `/actuator/health` includes RAG search, vector-store, embedding, and ingestion indicators
  when RAG health is enabled.
- Offline boundary: indicators inspect beans and configuration only. They do not execute RAG search, vector similarity
  search, embedding calls, ingestion work, ToolRegistry runtime, AgentRun, JDBC, PGvector, Spring AI `EmbeddingModel`, or
  Spring AI `VectorStore`.
- Secret safety boundary: details are disabled by default; when enabled, RAG details use sanitized provider/configuration
  signals and do not expose API keys, passwords, tokens, local paths, prompts, raw text, or full provider URLs with
  credentials.
- Actuator exposure boundary: default management exposure remains limited to `health`; sensitive endpoints such as env,
  configprops, and beans are not exposed by default.
- Architecture boundary: ArchitectureTest covers health package isolation from Spring Web controllers, JDBC/DataSource,
  Spring AI concrete clients, VectorStore, ToolRegistry / AgentRun runtime, Agent / Handler / Skill dependencies, and
  business repository implementations.

Known limitations:

- V4.6.3 health is offline readiness only. It does not prove live PostgreSQL / PGvector connectivity, real provider
  reachability, embedding latency, vector index freshness, or production monitoring coverage.

### V4.6.4 OpenAPI / API Docs Polish (completed)

Status: completed for OpenAPI / Swagger UI / API documentation polish.

Current V4.6.4 quality status:

- OpenAPI quality: `/v3/api-docs` exposes the existing Spring Boot HTTP API surface with title
  `AfterSale-Agent API` and V4 metadata.
- Swagger UI quality: `/swagger-ui/index.html` and `/swagger-ui.html` are available for local review without live
  providers.
- API documentation quality: Ticket, AgentRun, Approval, ToolCallTrace, Execution Tree, and platform health APIs have
  OpenAPI annotations and synthetic examples.
- Evidence-only documentation boundary: docs explain that `search_aftersale_policy` remains a LOW-risk read-only
  ToolRegistry tool; RAG evidence is not a business action and retrieval score is not business decision confidence.
- Exposure boundary: `/actuator/health` remains the only default actuator endpoint; OpenAPI docs do not expose env,
  configprops, beans, API keys, passwords, tokens, raw prompts, raw datasets, or local paths.
- Offline boundary: OpenAPI docs and tests do not call real LLMs, real embedding providers, PostgreSQL, PGvector,
  Docker, MySQL, Redis, API keys, or external network.
- Architecture boundary: ArchitectureTest covers OpenAPI config isolation from repositories, embedding/vector
  contracts, PGvector, Spring AI, VectorStore, DataSource, and JdbcTemplate.

Known limitations:

- V4.6.4 does not add authentication, authorization, production deployment guidance, or a public policy-search
  controller.
- OpenAPI docs do not prove live provider or PGvector connectivity.

### V4.7.1 Documentation Consistency / Secret Safety Audit (completed)

Status: completed for documentation consistency, completion-record safety, and docs harness coverage.

Current V4.7.1 quality status:

- Documentation consistency quality: V4 roadmap, active plan, README, quality notes, agent contracts, decision docs,
  demo docs, evaluation docs, and API docs describe V4.0 through V4.6.4 as completed without turning future opt-in
  capabilities into default runtime behavior.
- Completion-record quality: V4 completed plans are checked for completed status, `TASK_COMPLETE`, repository-relative
  path usage, and no overclaim of later-stage runtime features.
- Secret / path safety quality: documentation checks reject likely real API keys, passwords, tokens, local absolute
  paths, raw prompt leaks, raw dataset paths in V4 docs, and completed-action claims.
- Evidence-only quality: docs continue to state that RAG evidence is policy evidence, not a business decision or
  business action; `search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool.
- Runtime non-change quality: V4.7.1 does not change `search_aftersale_policy`, retrieval algorithms, ToolRegistry,
  ToolCallTrace, Workspace, Execution Tree, RAG evaluation, Actuator health, OpenAPI runtime, or AgentRun semantics.
- Default offline boundary: documentation audit tests only read repository files and do not call real LLMs, real
  embedding providers, PostgreSQL, PGvector, Docker, MySQL, Redis, API keys, or external network.

Known limitations:

- V4.7.1 did not perform new architecture boundary closure, demo polish beyond consistency fixes, or the final V4
  completion record. Architecture boundary closure is handled by V4.7.2; demo polish is handled by V4.7.3; the final
  V4 completion record is handled by V4.7.4.

### V4.7.2 Architecture Boundary / Offline Validation Closure (completed)

Status: completed for architecture boundary closure, default offline validation closure, live skip closure, and
validation command documentation.

Current V4.7.2 quality status:

- Architecture boundary quality: ArchitectureTest adds focused rules for Agent / Handler / Skill isolation from RAG
  evaluation, RAG health, OpenAPI config, provider infrastructure, ingestion repositories, embedding clients, vector
  repositories, JDBC, DataSource, Spring AI, and VectorStore dependencies.
- Tool boundary quality: tool executors remain application-service callers and do not bind directly to provider
  infrastructure or low-level clients.
- Default offline quality: `DefaultOfflineValidationTest` verifies the default Spring context does not create
  DataSource, PGvector profile guard, Spring AI ChatModel, Spring AI EmbeddingModel, Spring AI VectorStore, or live
  provider gateway beans.
- Actuator exposure quality: default `/actuator/health` remains available, while env / beans / configprops are not
  broadly exposed by default.
- Live skip quality: `LiveTestSkipClosureTest` checks live LLM, Spring AI chat, Spring AI embedding, and real Agent
  validation tests require explicit opt-in flags plus credential / environment assumptions.
- Validation documentation quality: `docs/quality/VALIDATION_COMMANDS.md` records default offline commands, live
  opt-in examples, and regression handling.
- Runtime non-change quality: V4.7.2 does not change `search_aftersale_policy`, retrieval algorithms, ToolRegistry,
  ToolCallTrace, Workspace, Execution Tree, RAG evaluation, Actuator health behavior, OpenAPI behavior, or AgentRun
  semantics.

Known limitations:

- V4.7.2 does not add broad live PGvector integration validation, production monitoring, production deployment, demo
  polish, or the final V4 completion record. Those remain future scoped work.

### V4.7.3 Interview Demo / README Polish (completed)

Status: completed for interview-facing README polish, demo checklist, project highlights, and docs harness coverage.

Current V4.7.3 quality status:

- README quality: README now gives interview reviewers a short path through project positioning, safety boundaries, V4
  RAG / Tool / Skill capabilities, and default offline validation commands.
- Demo quality: `docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md` records the recommended 5 / 10 / 10 / 5 minute walkthrough,
  pre-demo checks, common questions, suggested answer points, and fallback paths.
- Highlights quality: `docs/demo/V4_PROJECT_HIGHLIGHTS.md` summarizes the V4 technology stack, key capabilities,
  engineering quality gates, and future work without presenting future work as completed.
- Boundary quality: RAG evidence remains evidence-only policy support; `search_aftersale_policy` remains a LOW-risk
  read-only ToolRegistry tool; the default interview path remains offline.
- Runtime non-change quality: V4.7.3 does not change `search_aftersale_policy`, retrieval algorithms, ToolRegistry,
  ToolCallTrace, Workspace, Execution Tree, RAG evaluation, Actuator health behavior, OpenAPI behavior, or AgentRun
  semantics.
- Docs harness quality: interview demo docs tests verify links, default validation wording, secret / path safety,
  evidence-only claims, and no completed-action or production deployment overclaims.

Known limitations:

- V4.7.3 did not create the final V4 completion record. That closure is completed by V4.7.4.

### V4.7.4 V4 Final Completion Record (completed)

Status: completed for V4 final quality closure, release summary, final roadmap status, and docs harness coverage.

Current V4.7.4 quality status:

- Final completion quality: `version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md` summarizes V4.0
  through V4.7.4 scope, preserved architecture boundaries, default offline validation, evidence-only safety, known
  limitations, recommended demo path, and future work.
- Release summary quality: `version-updates/V4_RELEASE_SUMMARY.md` gives reviewers a concise V4 summary, validation path,
  demo path, non-production boundaries, and roadmap.
- Roadmap quality: `EXEC_PLAN_V4.md`, the historical active V4 plan, README, and this quality score now mark V4
  overall and V4.7.4 as completed while keeping V5 / production hardening as future work.
- Validation quality: the final default gate remains `mvn test`, `mvn checkstyle:check`, `mvn spotbugs:check`, and
  `mvn test -Dtest=ArchitectureTest`.
- Offline quality: default validation remains independent from real LLMs, API keys, PostgreSQL, PGvector, Docker,
  MySQL, Redis, real embedding providers, Spring AI VectorStore, and external network.
- Architecture quality: ArchUnit coverage continues to protect Agent / Handler / Skill, ToolRegistry, RAG, ingestion,
  OpenAPI, Actuator, Spring AI, PGvector, JDBC, DataSource, and VectorStore boundaries.
- Docs harness quality: final docs tests verify completion records, release summary links, final status wording,
  default offline validation, ToolRegistry boundary, evidence-only RAG wording, live opt-in wording, and no production
  / real external integration overclaims.
- Spring Boot completeness quality: V4 includes OpenAPI / Swagger UI docs and offline Actuator health indicators for
  current diagnostics; this is not a production monitoring claim.
- RAG evaluation quality: deterministic RAG evaluation remains part of the default offline quality story and does not
  require live providers.

Known limitations:

- V4 final quality closure does not implement production auth, production deployment, production monitoring,
  `JdbcPolicyVectorRepository`, live PGvector validation, production ingestion admin UI, or real payment / logistics /
  refund / exchange / coupon compensation integrations.

### Project Review Correction Stage 0 (completed)

Status: completed for documentation fact wording and project review correction docs harness coverage.

Current Stage 0 quality status:

- Documentation fact quality: README, V4 plans, release summary, validation docs, PGvector setup docs, and policy
  ingestion docs now distinguish completed foundation / demo / interview-grade scope from V5 production hardening.
- PGvector wording quality: docs describe PGvector as profile, schema, compose docs, repository contract, and fake /
  in-memory default store. They do not present `JdbcPolicyVectorRepository`, default live PGvector write/search, or
  Spring AI VectorStore production path as completed.
- API wording quality: docs describe the HTTP surface as demo/backend API surface, not complete production CRUD.
- Observability wording quality: docs separate current MDC / structured logs, ToolCallTrace, Execution Tree, Actuator
  health, and RAG readiness diagnostics from future Prometheus, dashboard, distributed tracing, and cross-service
  trace-id propagation.
- Runtime non-change quality: Stage 0 changes docs and docs harness tests only; runtime behavior, retrieval
  algorithms, ToolRegistry, Actuator health, OpenAPI config, ToolCallTrace, Workspace, and Execution Tree are unchanged.

Known limitations:

- Stage 0 did not implement V5 production hardening, production config templates, metrics, distributed tracing,
  Ticket pagination, async AgentRun, SSE, deeper Spring AI features, RAG reranking, query rewriting, RRF, chunk window
  expansion, `JdbcPolicyVectorRepository`, or live PGvector validation. Ticket pagination is later completed by
  Project Review Correction Stage 3.2.

### Project Review Correction Stage 1 (completed)

Status: completed for production configuration template, secret placeholder documentation, and docs harness coverage.

Current Stage 1 quality status:

- Production config template quality: `src/main/resources/application-prod.example.yml` provides a reviewable template
  for server, logging, datasource / Hikari, LLM provider, Spring AI, RAG / PGvector, Actuator, and OpenAPI settings.
- Secret placeholder quality: sensitive values use environment variable placeholders with empty defaults. The template
  does not contain real API keys, database passwords, tokens, private endpoints, local absolute paths, raw prompts, or
  raw dataset paths.
- Documentation quality: `docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md` explains environment variable groups, the default
  offline boundary, live opt-in boundary, secret safety, and the relationship to future production hardening.
- README and validation quality: README and `docs/quality/VALIDATION_COMMANDS.md` link the production template docs and
  state that the template is not loaded by default and is not a production deployment manifest.
- Docs harness quality: `ProductionConfigTemplateDocsTest` verifies template existence, links, placeholder safety,
  default offline wording, no production overclaims, and the completion record.
- Runtime non-change quality: Stage 1 changes template/docs/docs harness only. Runtime services, controllers, tools,
  RAG retrieval, ingestion, health indicators, OpenAPI config, ToolRegistry, ToolCallTrace, Workspace, Execution Tree,
  and AgentApplicationService are unchanged.

Known limitations:

- Stage 1 does not implement production auth, secret manager integration, production deployment, production monitoring,
  metrics, distributed tracing, CI/CD, Kubernetes, Helm, Dockerfile hardening, async AgentRun,
  `JdbcPolicyVectorRepository`, live PGvector validation, production ingestion admin UI, or real payment / logistics /
  refund / exchange / coupon compensation integrations. Ticket pagination is later completed by Project Review
  Correction Stage 3.2.

### Project Review Correction Stage 2 (completed)

Status: completed for observability hardening decision, metrics / tracing strategy documentation, and docs harness
coverage.

Current Stage 2 quality status:

- Observability decision quality:
  `docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md` records the current baseline, gaps, decision,
  metrics strategy, tracing strategy, logging strategy, Actuator exposure strategy, RAG / Agent observability strategy,
  secret safety, and default offline boundary.
- Current baseline quality: docs now consistently describe MDC / structured logs, `X-Request-Id`, ToolCallTrace,
  ApprovalRequest, Execution Tree, `/actuator/health`, RAG readiness diagnostics, OpenAPI docs, and offline RAG
  evaluation metrics as the current diagnostic surfaces.
- Metrics strategy quality: candidate AgentRun, ToolCall, Approval, RAG search, LLM provider, and embedding provider
  metrics are recorded as future / opt-in work with low-cardinality and no-secret label boundaries.
- Tracing strategy quality: current tracing remains MDC-only; OpenTelemetry, collector, cross-service trace-id
  propagation, spans, and exporters remain future / opt-in.
- Actuator exposure quality: default exposure remains limited to `health`; env, beans, configprops, heapdump,
  threaddump, and prometheus are not default endpoints.
- Secret safety quality: docs state logs, metrics labels, trace attributes, health details, OpenAPI examples, and docs
  must not contain API keys, database passwords, tokens, complete prompt content, raw provider responses, raw dataset paths,
  local absolute paths, or customer private data.
- Docs harness quality: `ObservabilityHardeningDecisionDocsTest` verifies links, baseline wording, future / opt-in
  monitoring boundaries, actuator exposure boundaries, default offline requirements, secret safety, and completion
  record.
- Runtime non-change quality: Stage 2 changes docs and docs harness tests only. Runtime services, controllers, tools,
  RAG retrieval, ingestion, health indicators, OpenAPI config, ToolRegistry, ToolCallTrace, Workspace, Execution Tree,
  AgentApplicationService, `pom.xml`, and `application.yml` are unchanged.

Known limitations:

- Stage 2 does not implement Micrometer instrumentation, Prometheus registry, Grafana dashboard, OpenTelemetry,
  collector configuration, production log aggregation, provider latency / cost metrics, production monitoring,
  live provider connectivity checks, live PGvector connectivity checks, async AgentRun, SSE / WebSocket, or batch
  APIs. Ticket pagination is later completed by Project Review Correction Stage 3.2.

### Project Review Correction Stage 3.1 (completed)

Status: completed for API surface audit, API completeness decision, and docs harness coverage.

Current Stage 3.1 quality status:

- API surface audit quality:
  `docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md` records the current demo/backend API surface:
  Ticket create/get, AgentRun create/start, ToolCallTrace read-only view, Execution Tree read-only view, Approval
  pending/get/approve/reject, health endpoints, OpenAPI JSON, and Swagger UI.
- API limitation quality: docs state that current APIs are not complete production CRUD. Stage 3.1 recorded Ticket
  list/query pagination, AgentRun get/status polling, production-grade async AgentRun, SSE / WebSocket streaming,
  batch APIs, production auth / RBAC, idempotency, rate limiting, and API audit hardening as follow-up candidates.
  Ticket list/query pagination is later completed by Project Review Correction Stage 3.2. AgentRun get/status polling
  is later completed by Project Review Correction Stage 3.3.
- OpenAPI quality: OpenAPI docs continue to describe existing HTTP APIs only. They do not add a public RAG search
  endpoint and do not claim production API hardening.
- ToolRegistry boundary quality: `search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool. LLM output
  may plan tools but must not directly execute tools or expose an HTTP bypass around ToolRegistry, RiskPolicy,
  Approval, or ToolCallTrace.
- Docs harness quality: `ApiCompletenessDecisionDocsTest` verifies decision docs, completion record, README / OpenAPI /
  remediation / validation links, future boundaries, secret safety, no complete CRUD overclaim, and no real external
  integration overclaim.
- Runtime non-change quality: Stage 3.1 changes docs and docs harness tests only. Runtime services, controllers, DTOs,
  tools, RAG retrieval, ingestion, health indicators, OpenAPI config, ToolRegistry, ToolCallTrace, Workspace,
  Execution Tree, AgentApplicationService, `pom.xml`, and application resources are unchanged.

Known limitations:

- Stage 3.1 does not implement Ticket list/query pagination, AgentRun get/status polling, async AgentRun,
  SSE / WebSocket, batch APIs, production auth / RBAC, idempotency, rate limiting, or production API audit hardening.
  Ticket list/query pagination is later completed by Project Review Correction Stage 3.2. AgentRun get/status polling
  is later completed by Project Review Correction Stage 3.3.

### Project Review Correction Stage 3.2 (completed)

Status: completed for Ticket list/query pagination foundation, OpenAPI documentation, and docs harness coverage.

Current Stage 3.2 quality status:

- Ticket pagination quality: `GET /api/tickets` returns a bounded page with `items`, `page`, `size`, `totalElements`,
  `totalPages`, `hasNext`, `hasPrevious`, and applied `sort`.
- Query filter quality: Ticket list supports `page`, `size`, `sort`, `status`, `userId`, `orderId`, `intentType`,
  `createdFrom`, and `createdTo`. Sort fields are whitelisted to `createdAt`, `updatedAt`, and `ticketId`.
- API compatibility quality: existing `POST /api/tickets` and `GET /api/tickets/{ticketId}` paths remain unchanged.
- OpenAPI quality: Ticket list/query parameters are documented in `/v3/api-docs` and `docs/api/OPENAPI.md`.
- ToolRegistry / Agent boundary quality: Ticket listing is read-only. It does not create AgentRun records, call
  ToolRegistry, write ToolCallTrace or Workspace, invoke RAG retrieval, or expose a public RAG HTTP endpoint.
- Default offline quality: Ticket pagination tests use default in-memory dependencies and do not require real LLMs,
  API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live calls, or external
  network.
- Docs harness quality: `TicketPaginationDocsTest` verifies Stage 3.2 documentation links, completion status,
  pagination/filter wording, future API boundaries, secret safety, and no production integration overclaims.

Known limitations:

- Stage 3.2 does not add AgentRun get/status polling, production-grade async AgentRun, SSE / WebSocket streaming,
  batch APIs, production auth / RBAC, idempotency, rate limiting, public RAG HTTP APIs, or production API audit
  hardening.

### Project Review Correction Stage 3.3 (completed)

Status: completed for AgentRun get/status polling read model, OpenAPI documentation, and docs harness coverage.

Current Stage 3.3 quality status:

- AgentRun read quality: `GET /api/agent-runs/{runId}` returns a read-only summary with `runId`, `ticketId`,
  `status`, time fields, final / failure summary, and trace / execution-tree links.
- Status polling boundary quality: the status endpoint does not run Planner, execute ToolRegistry, write
  ToolCallTrace, mutate Ticket, mutate Workspace, or inline Execution Tree details.
- API compatibility quality: existing `POST /api/tickets/{ticketId}/agent-runs`,
  `GET /api/agent-runs/{runId}/traces`, and `GET /api/agent-runs/{runId}/execution-tree` paths remain unchanged.
- OpenAPI quality: AgentRun status read fields are documented in `/v3/api-docs` and `docs/api/OPENAPI.md`.
- Default offline quality: AgentRun status tests use default in-memory dependencies and do not require real LLMs,
  API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live calls, or external
  network.
- Docs harness quality: `AgentRunStatusDocsTest` verifies Stage 3.3 documentation links, completion status,
  AgentRun read/status wording, future API boundaries, secret safety, and no production integration overclaims.

Known limitations:

- Stage 3.3 does not add production-grade async AgentRun, SSE / WebSocket streaming, batch APIs,
  production auth / RBAC, idempotency, rate limiting, public RAG HTTP APIs, cancel/retry APIs, or production API audit
  hardening.

### Project Review Correction Stage 3.4 (completed)

Status: completed for async AgentRun / SSE / WebSocket / batch API evaluation, decision documentation, and docs harness
coverage.

Current Stage 3.4 quality status:

- Async evaluation quality:
  `docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md` records that async AgentRun needs a state
  machine, executor / queue strategy, idempotent run requests, duplicate submission protection, failure recovery,
  trace ordering, and approval state preservation before runtime implementation.
- Status polling quality: current `GET /api/agent-runs/{runId}` remains the safe read path and does not run Planner,
  call ToolRegistry, write ToolCallTrace, mutate Workspace, or modify Ticket / Approval state.
- Streaming evaluation quality: SSE / WebSocket remain future work; future streams must not expose raw prompts, raw LLM
  responses, secrets, complete tool output, complete evidence chunks, local paths, or provider configuration.
- Batch evaluation quality: future batch APIs require idempotency, rate limiting, size limits, partial failure models,
  per-item audit records, approval backlog control, and permission checks.
- Cancel / retry quality: cancel and retry remain future work until state-machine, idempotency, side-effect, and trace
  linkage rules are defined.
- Security quality: production auth / RBAC is explicitly recorded as a prerequisite for streaming, batch,
  cancel / retry, trace, execution-tree, and approval hardening.
- Docs harness quality: `AsyncStreamingBatchApiDecisionDocsTest` verifies Stage 3.4 documentation links, baseline API
  wording, future runtime boundaries, ToolRegistry / Approval / RAG boundaries, default offline requirements, and
  secret / local-path safety.
- Runtime non-change quality: Stage 3.4 changes docs and docs harness tests only. Runtime services, controllers, DTOs,
  tools, RAG retrieval, ingestion, health indicators, OpenAPI config, ToolRegistry, ToolCallTrace, Workspace,
  Execution Tree, AgentApplicationService, `pom.xml`, and application resources are unchanged.

Known limitations:

- Stage 3.4 does not implement async AgentRun runtime, SSE / WebSocket runtime, batch APIs, cancel / retry APIs,
  AgentRun list pagination, production auth / RBAC, idempotency, rate limiting, public RAG HTTP APIs, or production API
  audit hardening.

### Project Review Correction Stage 4 (completed)

Status: completed for Spring AI deepening evaluation, decision documentation, and docs harness coverage.

Current Stage 4 quality status:

- Baseline quality:
  `docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md` records the current Spring AI Chat adapter
  foundation and Spring AI embedding adapter foundation behind `LlmClient` and `EmbeddingClient`.
- ChatMemory evaluation quality: ChatMemory is not implemented in Stage 4 and must not replace `AgentWorkspace`,
  ToolCallTrace, Execution Tree, or business state if evaluated later.
- Advisor evaluation quality: Advisors are not implemented in Stage 4 and must not bypass `search_aftersale_policy`,
  RAG evidence merge logic, ToolRegistry, raw prompt safety, or evidence-only rules.
- Tool Calling API evaluation quality: Spring AI Tool Calling API is not enabled in Stage 4. Spring AI Tool Calling
  API cannot replace ToolRegistry, and LLM must not directly execute tools.
- Bulk embedding evaluation quality: bulk embedding runtime is not implemented in Stage 4. Bulk embedding must stay
  behind `EmbeddingClient` abstraction.
- Agent boundary quality: `AgentPlanParser` and `AgentPlanValidator` must not be bypassed, and high-risk actions still
  require Approval.
- Default offline quality: Stage 4 docs harness tests read files only and do not require real LLMs, API keys,
  PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live calls, Spring AI VectorStore, or
  external network.
- Runtime non-change quality: Stage 4 does not modify Spring AI clients, provider clients, Planner, ToolRegistry, RAG
  runtime, ingestion pipeline, health indicators, OpenAPI config, ToolCallTrace, Workspace, Execution Tree,
  `src/main/java`, `src/main/resources`, or `pom.xml`.

Known limitations:

- Stage 4 does not implement ChatMemory runtime, Advisors runtime, Spring AI Tool Calling API, bulk embedding runtime,
  provider runtime changes, public RAG HTTP endpoints, RAG reranking, query rewriting, RRF, or chunk window expansion.

### Project Review Correction Stage 5 (completed)

Status: completed for RAG retrieval quality improvement evaluation, decision documentation, and docs harness coverage.

Current Stage 5 quality status:

- Current RAG baseline quality:
  `docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md` records current KEYWORD / VECTOR / HYBRID
  policy evidence retrieval, `RagPolicyEvidenceMergeService`, `EmbeddingClient` abstraction,
  `PolicyVectorRepository` contract, `FakeEmbeddingClient`, and `InMemoryPolicyVectorRepository`.
- Evaluation baseline quality: deterministic RAG evaluation exists and remains no LLM-as-judge by default.
- Reranking evaluation quality: reranking is not implemented in Stage 5. Future reranking must be opt-in for real
  providers and must treat reranking score as retrieval score, not business decision confidence.
- Query rewriting evaluation quality: query rewriting is not implemented in Stage 5. Future rewriting must be
  auditable, deterministic by default, and safe against raw prompt leakage.
- RRF / hybrid scoring quality: RRF is not implemented in Stage 5. Current HYBRID scoring remains unchanged and any
  future scoring change must be proven against deterministic evaluation cases.
- Chunk window expansion quality: chunk window expansion is not implemented in Stage 5. Future expansion must define
  max window, max chars, source citation, and dedup rules without returning full source documents.
- Provider / PGvector quality at Stage 5 time: `JdbcPolicyVectorRepository` and live PGvector validation were not
  implemented in that stage, and Spring AI VectorStore production path was not enabled. V5.A.1 later adds the opt-in
  JDBC adapter and V5.A.3 later adds the opt-in live connectivity smoke, while real reranker/embedding providers must
  remain opt-in.
- Evidence-only quality: `search_aftersale_policy` remains LOW-risk read-only ToolRegistry tool. RAG evidence is
  evidence-only, RAG score is not business decision confidence, high-risk actions require Approval, LLM must not
  directly execute tools, and future RAG improvements must not bypass ToolRegistry / RiskPolicy / Approval / Trace /
  Workspace / Execution Tree.
- Default offline quality: Stage 5 docs harness tests read files only and do not require real LLMs, API keys,
  PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, real reranker providers, Spring AI VectorStore,
  or external network.
- Runtime non-change quality: Stage 5 does not modify `search_aftersale_policy`, retrieval algorithms, RAG evaluation
  runner, RAG runtime, ingestion pipeline, health indicators, OpenAPI config, ToolRegistry, ToolCallTrace, Workspace,
  Execution Tree, `src/main/java`, `src/main/resources`, or `pom.xml`.

Known limitations:

- Stage 5 does not improve runtime retrieval quality directly.
- Stage 5 does not implement reranking runtime, query rewriting runtime, RRF, hybrid scoring runtime changes, chunk
  window expansion, `JdbcPolicyVectorRepository`, live PGvector validation, Spring AI VectorStore production path,
  production-scale relevance benchmark, or a public RAG HTTP endpoint. The `JdbcPolicyVectorRepository` adapter is
  later completed in V5.A.1 and the opt-in live PGvector connectivity smoke is later completed in V5.A.3.

### Project Review Correction Stage 6 (completed)

Status: completed for deployment hardening roadmap, decision documentation, and docs harness coverage.

Current Stage 6 quality status:

- Deployment baseline quality:
  `docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md` records current `docker-compose.yml`,
  `docker-compose-rag.yml`, `.env.rag.example`, `application-prod.example.yml`, `application-mysql.yml`,
  `application-rag-postgres.yml`, Actuator health, OpenAPI docs, default validation commands, and default offline
  testing boundary.
- Deployment roadmap quality:
  `docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md` breaks future hardening into Dockerfile, CI quality gate, profile
  matrix, secret management, database migration, PGvector deployment, readiness/liveness, observability,
  security/auth, and release/rollback checklists.
- Future boundary quality: Dockerfile is not implemented, CI/CD is not implemented, Kubernetes / Helm is not
  implemented, secret manager is not implemented, production deployment is not completed, live PGvector validation is
  not completed, `JdbcPolicyVectorRepository` is not implemented, production auth/RBAC is not completed, and
  production monitoring is not completed.
- Default offline quality: Stage 6 docs harness tests read files only and do not require real LLMs, API keys,
  PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live provider calls, secret manager,
  CI runner, Kubernetes / Helm, Prometheus, Grafana, OpenTelemetry collector, or external network.
- Runtime non-change quality: Stage 6 does not modify `src/main/java`, `src/main/resources`, `pom.xml`,
  Docker / Compose files, ToolRegistry, `search_aftersale_policy`, RAG runtime, ingestion pipeline, health indicators,
  OpenAPI config, ToolCallTrace, Workspace, Execution Tree, or AgentApplicationService.

Known limitations:

- Stage 6 does not implement Dockerfile hardening, CI/CD pipeline, Kubernetes / Helm, secret manager, production
  deployment, production auth/RBAC, production monitoring, live PGvector validation, `JdbcPolicyVectorRepository`,
  Flyway / Liquibase, readiness/liveness runtime changes, or production external integrations.

### V5.A.1 JdbcPolicyVectorRepository (completed)

Status: completed for the explicit opt-in JDBC PGvector repository adapter and its offline-safe boundary tests.

Current V5.A.1 quality status:

- Repository adapter quality:
  `JdbcPolicyVectorRepository` implements the existing `PolicyVectorRepository` port through
  `NamedParameterJdbcOperations` and stays inside PGvector infrastructure.
- Profile isolation quality: the adapter is created only for the explicit `rag-postgres` / `pgvector` path and is not
  created by the default profile.
- SQL / row mapping quality: repository tests cover vector literal validation, schema-name validation, SQL parameter
  usage, document/chunk/embedding row mapping, and vector search score mapping.
- Sanitized error quality: PGvector repository failures are wrapped without exposing JDBC credentials, raw SQL, raw
  vectors, local paths, raw prompts, or raw dataset paths.
- Default offline quality: V5.A.1 tests do not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL,
  Redis, real embedding providers, Spring AI `VectorStore`, or external network.
- Runtime non-change quality: V5.A.1 does not modify `search_aftersale_policy` retrieval algorithms, ToolRegistry
  execution semantics, ToolCallTrace schema, Workspace evidence logic, Execution Tree runtime, RAG evaluation runner,
  Actuator health behavior, OpenAPI behavior, or public API surface.

Known limitations:

- V5.A.1 does not add live PGvector validation, database migration baseline, Spring AI `VectorStore` production path,
  Admin ingestion API, public RAG HTTP endpoint, reranking, query rewriting, RRF, or chunk window expansion.

### V5.A.2 Schema Init / Version Baseline (completed)

Status: completed for schema version baseline documentation and docs harness validation.

Current V5.A.2 quality status:

- Schema baseline quality: `schema-rag-postgres.sql` declares schema version baseline `2026-06-01-001` for the opt-in
  `JdbcPolicyVectorRepository` / PGvector policy evidence search path.
- Initialization documentation quality: docs explain docker-compose-rag fresh-volume init mount, manual SQL import,
  and future explicit test setup as the supported initialization paths.
- Migration boundary quality: V5.A.2 does not add Flyway / Liquibase. Migration framework work remains pending V5.B.2.
- Live validation boundary quality: V5.A.2 does not validate live PGvector connectivity. V5.A.3 later completes the
  explicit opt-in PGvector connectivity smoke task.
- Default offline quality: V5.A.2 docs harness tests read files only and do not require real LLMs, API keys,
  PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI `VectorStore`, or external network.
- Runtime non-change quality: V5.A.2 does not modify `src/main/java`, retrieval algorithms, ToolRegistry semantics,
  RAG health, OpenAPI behavior, ingestion runtime, ToolCallTrace, Workspace, Execution Tree, or public API behavior.

Known limitations:

- V5.A.2 does not implement production database migrations, live PGvector validation, Spring AI `VectorStore`
  production path, Admin ingestion API, public RAG HTTP endpoint, RAG quality improvements, or production deployment.

### V5.A.3 PGvector Connectivity Smoke Test (completed)

Status: completed for the explicit opt-in live PGvector connectivity smoke test.

Current V5.A.3 quality status:

- Smoke opt-in quality: `JdbcPolicyVectorRepositorySmokeTest` is tagged `live`, requires
  `-Dlive.rag=true`, and is not part of default `mvn test`.
- Environment boundary quality: the smoke uses existing project variables `AFTERSALE_PGVECTOR_URL`,
  `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`, and optional `AFTERSALE_PGVECTOR_SCHEMA`. Missing
  required configuration skips through JUnit assumptions.
- Connectivity quality: the smoke executes `schema-rag-postgres.sql`, writes temporary `v5a3-smoke-` records, verifies
  document/chunk/embedding lookup, verifies fake / fixed-vector ranking, checks duplicate and invalid-vector failure
  sanitization, and cleans up its temporary rows.
- Schema setup quality: `CREATE EXTENSION IF NOT EXISTS vector` failures from insufficient privileges skip with a
  sanitized setup reason. Fresh `docker-compose-rag.yml` initialization and preinstalled PGvector instances remain the
  recommended live setup paths.
- Default offline quality: default validation still does not require real LLMs, API keys, PostgreSQL, PGvector,
  Docker, MySQL, Redis, real embedding providers, Spring AI `VectorStore`, or external network.
- Runtime non-change quality: V5.A.3 does not modify `src/main/java`, does not change `search_aftersale_policy`
  retrieval algorithms, does not call ToolRegistry, does not write AgentRun / ToolCallTrace / Workspace / Execution
  Tree state, and does not change RAG evaluation, Actuator health, OpenAPI, or ingestion runtime.

Known limitations:

- V5.A.3 does not validate RAG quality, real embedding recall, reranking, query rewriting, RRF, chunk window expansion,
  Spring AI `VectorStore` production path, Flyway / Liquibase migration management, Admin ingestion API, public RAG
  HTTP endpoints, production deployment, or production monitoring.

### V5.A RAG Production Path Foundation (completed)

Status: completed for documentation and validation closure of the V5.A RAG production path foundation.

Current V5.A quality status:

- Dual-path quality: the default path remains fake / in-memory through `InMemoryPolicyVectorRepository`, while the
  JDBC PGvector path is explicit opt-in through `JdbcPolicyVectorRepository`.
- Schema quality: `schema-rag-postgres.sql` carries schema version baseline `2026-06-01-001` for the opt-in PGvector
  path.
- Smoke quality: `JdbcPolicyVectorRepositorySmokeTest` validates opt-in SQL connectivity, persistence, lookup,
  fixed-vector search ranking, cleanup, and sanitized failures only when `-Dlive.rag=true` is supplied.
- Docs harness quality: `RagProductionPathCompletionDocsTest` verifies completion records, V5.A status, default
  offline boundaries, smoke boundaries, ToolRegistry / evidence-only wording, and production overclaim safety.
- Runtime non-change quality: V5.A.4 does not modify `src/main/java`, retrieval algorithms, ToolRegistry semantics,
  RAG health, OpenAPI behavior, RAG evaluation, ingestion runtime, ToolCallTrace, Workspace, Execution Tree, or public
  API behavior.

Known limitations:

- V5.A is a production path foundation only. Production deployment is not completed, production auth / RBAC is not
  completed, production monitoring is not completed, Flyway / Liquibase migration management is not completed, Spring
  AI `VectorStore` production path is not enabled, RAG quality enhancement is not completed, and real embedding quality
  validation is not completed.
- Reranking, query rewriting, RRF, chunk window expansion, production ingestion API / admin UI, and real refund /
  exchange / payment / logistics integrations remain future work.

### V5.B.1 Container + CI Foundation (completed)

Status: completed for container build foundation and CI quality gate.

Current V5.B.1 quality status:

- Dockerfile foundation quality: the image build uses a Java 17 Maven build stage and a Java 17 JRE runtime stage.
- Non-root runtime quality: the runtime image creates and uses an `aftersale` system user.
- Build-context safety quality: `.dockerignore` excludes `.env*`, key material, `target`, Git metadata, IDE files,
  logs, temporary folders, local data folders, and common generated outputs.
- CI quality: `.github/workflows/ci.yml` runs `mvn test`, `mvn checkstyle:check`, `mvn spotbugs:check`, and
  `mvn test -Dtest=ArchitectureTest`.
- Docker build quality: CI validates `docker build -t after-sale-agent-platform:ci .` without image push, registry
  login, Docker Compose, release, or deployment.
- Default offline quality: default validation still does not require real LLMs, API keys, PostgreSQL, PGvector,
  Docker, MySQL, Redis, real embedding providers, Spring AI live calls, registry secrets, or external business
  services.
- Runtime non-change quality: V5.B.1 does not modify `src/main/java`, application runtime semantics, ToolRegistry,
  `search_aftersale_policy`, RAG retrieval, RAG evidence merge, ingestion, health indicators, OpenAPI, ToolCallTrace,
  Workspace, Execution Tree, or AgentApplicationService.

Known limitations:

- V5.B.1 does not add CD / release automation, registry push, Kubernetes / Helm, secret manager, production auth /
  RBAC, production monitoring, Flyway / Liquibase migration management, readiness / liveness runtime changes, or live
  PGvector checks in the default gate.

### V5.B.2.1 Config / Secret Boundary and Profile Matrix Plan (completed)

Status: completed for configuration baseline documentation, secret boundary documentation, and migration follow-up
planning.

Current V5.B.2.1 quality status:

- Configuration baseline quality: docs now identify `application.yml` as the default offline / local baseline,
  `application-prod.example.yml` as a template only, `application-mysql.yml` as explicit MySQL opt-in, and
  `application-rag-postgres.yml` as explicit PostgreSQL / PGvector opt-in.
- Profile matrix quality: default / mysql / rag-postgres / prod-template profiles are documented with purpose,
  external dependency boundary, and default validation boundary.
- Secret boundary quality: docs state that Docker images do not contain secrets, CI default gates do not inject live
  secrets, and real secrets must come from environment variables, deployment platforms, or a future secret manager.
- Migration boundary quality: `schema-rag-postgres.sql` remains the schema version `2026-06-01-001` baseline
  reference. Flyway / Liquibase migration framework remains planned for V5.B.2.2.
- Default offline quality: V5.B.2.1 docs harness reads files only and does not require real LLMs, API keys,
  PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live calls, secret manager, Docker
  Compose, or external network.
- Runtime non-change quality: V5.B.2.1 does not modify `src/main/java`, `pom.xml`, Dockerfile, compose files,
  GitHub Actions workflow, application yml runtime semantics, ToolRegistry, `search_aftersale_policy`, RAG runtime,
  ingestion, health indicators, OpenAPI config, ToolCallTrace, Workspace, Execution Tree, or AgentApplicationService.

Known limitations:

- V5.B.2.1 does not implement Flyway / Liquibase, secret manager, profile matrix validation harness, readiness /
  liveness runtime changes, production auth / RBAC, production monitoring, production deployment, or external business
  integrations.

### V5.B.2.2 Flyway Migration Foundation (completed)

Status: completed for Flyway migration foundation, default-disabled migration configuration, and schema-only baseline
documentation.

Current V5.B.2.2 quality status:

- Flyway selection quality: `pom.xml` adds Flyway dependencies managed by Spring Boot dependency management. Liquibase
  is not introduced.
- Default disabled quality: `application.yml` keeps `spring.flyway.enabled: false`, so default Maven validation does
  not run migrations or create database connections.
- Profile-specific quality: `application-mysql.yml` points to `classpath:db/migration/mysql` behind
  `AFTERSALE_FLYWAY_ENABLED:false`, and `application-rag-postgres.yml` points to
  `classpath:db/migration/pgvector` behind `AFTERSALE_RAG_FLYWAY_ENABLED:false`.
- PGvector baseline quality: the PGvector migration copies `schema-rag-postgres.sql` version `2026-06-01-001`
  semantics for the explicit opt-in `JdbcPolicyVectorRepository` path.
- MySQL baseline quality: the MySQL migration is schema-only and intentionally excludes `data-mysql.sql` demo seed.
- Docs harness quality: `FlywayMigrationFoundationDocsTest` reads files only and does not start Spring, connect to
  MySQL / PostgreSQL / PGvector, run Docker, call real LLMs, call real embedding providers, invoke Spring AI live
  providers, or use external network.
- Runtime non-change quality: V5.B.2.2 does not modify `src/main/java`, Dockerfile, compose files, GitHub Actions
  workflow, ToolRegistry, `search_aftersale_policy`, RAG runtime, ingestion, health indicators, OpenAPI config,
  ToolCallTrace, Workspace, Execution Tree, or AgentApplicationService.

Known limitations:

- V5.B.2.2 does not implement Liquibase, secret manager, profile matrix validation harness, readiness / liveness
  runtime changes, production auth / RBAC, production monitoring, production deployment, or external business
  integrations.
- `CREATE EXTENSION IF NOT EXISTS vector` may require elevated PostgreSQL privileges. Existing docker init, preinstalled
  extension, or opt-in live smoke skip behavior remains the current operational boundary.

### V5.B.2.3 Profile Matrix Validation (completed)

Status: completed for file-based profile matrix validation harness and docs harness closure. V5.B.2 current scope
completed.

Current V5.B.2.3 quality status:

- Profile matrix quality: `ProfileMatrixValidationTest` verifies default, `mysql`, `rag-postgres`, production
  template, Flyway, CI, and live PGvector smoke boundaries by reading repository files only.
- Default profile quality: `application.yml` remains the default offline / local baseline with Flyway disabled,
  DataSource auto-configuration excluded, actuator health-only exposure, and live RAG / PGvector / Spring AI paths
  disabled by default.
- MySQL profile quality: `application-mysql.yml` remains explicit opt-in, uses `AFTERSALE_MYSQL_URL`,
  `AFTERSALE_MYSQL_USERNAME`, `AFTERSALE_MYSQL_PASSWORD`, and guards Flyway with `AFTERSALE_FLYWAY_ENABLED:false`.
- RAG PGvector profile quality: `application-rag-postgres.yml` remains explicit opt-in, keeps the existing
  `AFTERSALE_PGVECTOR_URL`, `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`,
  `AFTERSALE_PGVECTOR_SCHEMA` variable convention, and guards Flyway with `AFTERSALE_RAG_FLYWAY_ENABLED:false`.
- Production template quality: `application-prod.example.yml` remains template only and does not claim production
  deployment, production auth, production monitoring, secret manager, or real external business integrations.
- Live test quality: `JdbcPolicyVectorRepositorySmokeTest` remains `@Tag("live")`, gated by `-Dlive.rag=true`, and can
  skip sanitized setup failures such as `CREATE EXTENSION IF NOT EXISTS vector` permission issues.
- Docs harness quality: `ProfileMatrixValidationDocsTest` checks completion status, validation commands, secret
  safety, default offline guarantees, and future-work boundaries.
- Runtime non-change quality: runtime profile behavior was not changed. V5.B.2.3 does not modify `src/main/java`,
  `pom.xml`, application config files, Dockerfile, compose files, migration SQL, ToolRegistry, `search_aftersale_policy`,
  RAG runtime, ingestion, health indicators, OpenAPI config, ToolCallTrace, Workspace, Execution Tree, or
  AgentApplicationService.

Known limitations:

- V5.B.2.3 does not implement secret manager, production deployment, production auth / RBAC, production monitoring,
  readiness / liveness runtime changes, or external business integrations.
- Real refund / exchange / payment / logistics integrations are not connected.

### V5.B.3.1 Readiness / Liveness Boundary (completed)

Status: completed for readiness / liveness actuator probe boundary. V5.B.3.2 Micrometer metrics foundation completed.
V5.B.3.3 Prometheus opt-in exposure completed. V5.B.3.4 tracing / correlation boundary completed. V5.B.3.5
observability docs + completion record completed. V5.B.4 planned auth / Kubernetes / release hardening remains future
work.

Current V5.B.3.1 quality status:

- Probe configuration quality: `application.yml` enables `management.endpoint.health.probes.enabled=true` and defines
  explicit `liveness` and `readiness` health groups.
- Actuator exposure quality: Actuator web exposure remains health-only. `/actuator/health`,
  `/actuator/health/liveness`, and `/actuator/health/readiness` are available, while `/actuator/env`,
  `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, `/actuator/threaddump`, and
  `/actuator/prometheus` remain unavailable by default.
- Default offline quality: `ReadinessLivenessBoundaryTest` verifies the default context does not create `DataSource`,
  Spring AI live model, Spring AI `VectorStore`, or `JdbcPolicyVectorRepository` beans.
- Docs harness quality: `ReadinessLivenessBoundaryDocsTest` checks status docs, validation commands, secret safety,
  actuator exposure boundaries, live dependency boundaries, and future-work boundaries.
- Runtime non-change quality: V5.B.3.1 does not modify `src/main/java`, ToolRegistry, `search_aftersale_policy`,
  RAG retrieval, ingestion, health indicator internals, OpenAPI config, ToolCallTrace, Workspace, Execution Tree,
  Ticket, AgentRun, Approval, or production application service behavior.

Known limitations:

- V5.B.3.1 does not implement Micrometer business metrics, Prometheus registry, `/actuator/prometheus`, Grafana
  dashboards, OpenTelemetry, collector configuration, production monitoring, live DB / PGvector / LLM / embedding
  readiness checks, production auth / RBAC, Kubernetes / Helm, release / rollback hardening, or external business
  integrations.
- Real refund / exchange / payment / logistics integrations are not connected.

### V5.B.3.2 Micrometer Metrics Foundation (completed)

Status: completed for low-cardinality Micrometer metrics foundation. V5.B.3.3 Prometheus opt-in exposure completed.
V5.B.3.4 tracing / correlation boundary completed. V5.B.3.5 observability docs + completion record completed.
V5.B.4 planned auth / Kubernetes / release hardening remains future work.

Current V5.B.3.2 quality status:

- Metrics boundary quality: `ApplicationMetricsRecorder` records AgentRun, ToolCall, Approval, RAG search, and
  provider-call observations through Micrometer `MeterRegistry`.
- Metric naming quality: project-owned meters use the `aftersale.*` prefix, including AgentRun, ToolCall, Approval,
  RAG search, and provider call count / duration meters.
- Low-cardinality tag quality: allowed tags are bounded to component, operation, outcome, status, tool name, risk
  level, retrieval mode, fallback, provider type, and approval decision.
- Secret safety quality: metric tags sanitize secrets, paths, URLs, JDBC URLs, raw prompts, raw provider responses,
  search queries, snippets, raw dataset paths, and private endpoints to `unknown`.
- Actuator exposure quality: `/actuator/metrics` and `/actuator/prometheus` remain unavailable by default; Actuator web
  exposure remains health-only.
- Default offline quality: `MetricsFoundationBoundaryTest` verifies the default context does not create `DataSource`,
  Spring AI live model, Spring AI `VectorStore`, or `JdbcPolicyVectorRepository` beans.
- Docs harness quality: `MetricsFoundationDocsTest` checks status docs, metric names, tag safety, actuator exposure,
  validation commands, secret safety, and future-work boundaries.

Known limitations:

- V5.B.3.2 itself does not add Prometheus registry, `/actuator/prometheus`, OpenTelemetry, distributed tracing,
  dashboards, production monitoring backend, provider cost dashboards, production auth / RBAC, Kubernetes / Helm,
  release / rollback hardening, or external business integrations.
- Real refund / exchange / payment / logistics integrations are not connected.

### V5.B.3.3 Prometheus Opt-in Exposure (completed)

Status: completed for Prometheus opt-in exposure. V5.B.3.4 tracing / correlation boundary completed. V5.B.3.5
observability docs + completion record completed. V5.B.4 planned auth / Kubernetes / release hardening remains future
work.

Current V5.B.3.3 quality status:

- Dependency quality: `micrometer-registry-prometheus` is added without an explicit version and remains managed by
  Spring Boot dependency management.
- Profile quality: `application-observability-prometheus.yml` activates only under `observability-prometheus`.
- Default exposure quality: Actuator web exposure remains health-only by default. `/actuator/prometheus`,
  `/actuator/metrics`, `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, and
  `/actuator/threaddump` remain unavailable without opt-in.
- Opt-in exposure quality: `PrometheusOptInExposureTest` verifies `/actuator/prometheus` is available only with the
  explicit profile and that `/actuator/metrics` plus sensitive endpoints remain unavailable.
- Default offline quality: the default context still does not create `DataSource`, Spring AI live model, Spring AI
  `VectorStore`, or `JdbcPolicyVectorRepository` beans.
- Docs harness quality: `PrometheusOptInDocsTest` checks status docs, validation commands, secret safety, endpoint
  exposure boundaries, OpenTelemetry boundaries, production monitoring boundaries, and future-work boundaries.

Known limitations:

- V5.B.3.3 does not add OpenTelemetry, distributed tracing, cross-service propagation, Grafana dashboards, scrape jobs,
  alert rules, production monitoring backend, production auth / RBAC, Kubernetes / Helm, release / rollback hardening,
  or external business integrations.
- Real refund / exchange / payment / logistics integrations are not connected.

### V5.B.3.5 Observability Docs + Completion Record (completed)

Status: completed for observability docs and completion record. V5.B.4 planned auth / Kubernetes / release hardening
remains future work. Production monitoring backend, dashboards, alerting, log aggregation, and OpenTelemetry remain
future / opt-in work.

Current V5.B.3.5 quality status:

- Documentation quality: `docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md` maps readiness / liveness, Micrometer metrics,
  Prometheus opt-in exposure, local HTTP correlation, validation commands, and follow-ups.
- Completion record quality:
  `docs/exec-plans/completed/EXEC_PLAN_V5_B3_5_OBSERVABILITY_DOCS_COMPLETION_RECORD.md` records the scope,
  boundaries, validation commands, known limitations, follow-ups, and `TASK_COMPLETE`.
- Docs harness quality: `ObservabilityDocsCompletionDocsTest` checks links, status wording, default offline boundary,
  production monitoring non-goals, OpenTelemetry non-goals, secret safety, and overclaim prevention.
- Runtime non-change quality: V5.B.3.5 is documentation-only and does not modify business runtime semantics.

Known limitations:

- V5.B.3.5 does not implement production monitoring backend, production tracing, Grafana dashboards, Prometheus scrape
  jobs, alert rules, recording rules, external log aggregation, OpenTelemetry, production auth / RBAC, Kubernetes /
  Helm, release / rollback hardening, or external business integrations.
- Real refund / exchange / payment / logistics integrations are not connected.

### V5.B Production Hardening (current planned scope completed)

Status: V5.B Production Hardening current planned scope completed. V5.B.1 Container + CI,
V5.B.2 Config + Secret + Migration, V5.B.3 Observability, and V5.B.4 Auth + K8s +
Release/Rollback all completed. Production deployment, release automation, rollback
automation, production monitoring backend, secret manager, and external IAM remain future work.
See `docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md`.

### V5.B.4.1 Production Auth / RBAC Boundary Decision (completed)

Status: completed for documentation-only auth / RBAC boundary decision. V5.B.4.2 Spring
Security / API Key Auth Foundation completed. V5.B.4.3 K8s / Helm Foundation completed.
V5.B.4.4 Release / Rollback Foundation completed. V5.B.4 current scope completed.

Current V5.B.4.1 quality status:

- Decision quality: `docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md` records the current API surface, current
  auth gap, planned role model, API access matrix, actuator boundary, OpenAPI boundary, Approval boundary,
  ToolRegistry boundary, RAG evidence-only boundary, K8s exposure precondition, and release / rollback security
  precondition.
- Deployment docs quality: `docs/deploy/AUTH_RBAC_BOUNDARY.md` gives the deployment-facing auth / RBAC boundary and
  states production auth runtime remains planned.
- Completion record quality:
  `docs/exec-plans/completed/EXEC_PLAN_V5_B4_1_AUTH_RBAC_BOUNDARY_DECISION.md` records scope, non-goals, validation
  commands, limitations, follow-ups, and `TASK_COMPLETE`.
- Docs harness quality: `AuthRbacBoundaryDocsTest` checks links, status wording, role model, API access matrix,
  ToolRegistry direct access boundary, Approval boundary, RAG evidence-only boundary, default offline validation,
  future-work boundaries, secret safety, and overclaim prevention.
- Runtime non-change quality: V5.B.4.1 does not modify `src/main/java`, `src/main/resources`, `pom.xml`, Dockerfile,
  CI workflow, migration SQL, ToolRegistry, AgentApplicationService, RAG runtime, health indicators, metrics recorder,
  Prometheus config, correlation filter, OpenAPI runtime config, ToolCallTrace, Workspace, or Execution Tree.

### V5.B.4.2 Spring Security / API Key Auth Foundation (completed)

Status: completed for opt-in Spring Security API key auth foundation. Default local/test profile remains permit-all and
offline. Full production IAM remains future work.

Current V5.B.4.2 quality status:

- Runtime boundary quality: Spring Security protects HTTP endpoints only under `security-api-key` or
  `agent.security.enabled=true`; business services and ToolRegistry semantics are unchanged.
- Default profile quality: `SecurityDefaultBoundaryTest` verifies default API access remains permit-all and live
  dependency beans are not created.
- Credential quality: `ApiKeyCredentialValidatorTest` verifies configured key-to-role mapping, blank key rejection,
  invalid key rejection, fail-fast behavior, and no raw key exposure in principal text.
- RBAC quality: `ApiKeyAuthBoundaryTest` verifies protected API `401` / `403` behavior, health public boundary,
  Approval decision role boundary, Trace / ExecutionTree role boundary, and OpenAPI / Swagger role boundary.
- Prometheus quality: `ApiKeyPrometheusBoundaryTest` verifies opt-in Prometheus requires `ADMIN` or `SYSTEM_SERVICE`
  under the security profile.
- Docs harness quality: `ApiKeyAuthFoundationDocsTest` checks deployment docs, completion record, status docs, secret
  safety, default offline boundary, non-goals, and overclaim prevention.
- Architecture quality: `ArchitectureTest` keeps the security package away from business repositories, ToolRegistry,
  RAG runtime, JDBC, Spring AI and PGvector concrete classes.

Known limitations:

- Full production auth / IAM is not completed.
- OAuth2 / OIDC and JWT issuer / JWKS are not implemented.
- Session login, password login, user database and tenant isolation are not implemented.
- Secret manager, rate limiting, Kubernetes / Helm and release / rollback automation are not implemented.
- Real refund / exchange / payment / logistics integrations are not connected.

Known limitations from V5.B.4.1 decision context:

- full production auth/RBAC is not completed.
- full production IAM is not completed.
- OAuth2 / OIDC and session login are not implemented.
- Kubernetes / Helm is not implemented.
- Release / rollback automation is not implemented.
- Production deployment is not completed.
- Real refund / exchange / payment / logistics integrations are not connected.

### V5.B.4.3 K8s / Helm Foundation (completed)

Status: completed for Kubernetes manifest templates and Helm chart skeleton. V5.B.4.4 planned.

Current V5.B.4.3 quality status:

- K8s manifest quality: `deploy/k8s/` contains deployment, service, configmap, and secret example with safe
  placeholders only. Non-root securityContext, readiness/liveness probes wired to Spring Boot Actuator health groups.
- Helm chart quality: `deploy/helm/after-sale-agent-platform/` provides Chart.yaml, values.yaml with placeholders,
  templates for deployment/service/configmap/secret, _helpers.tpl with profile composition, and NOTES.txt with
  security warnings.
- Secret safety quality: K8s Secret uses `stringData` with `REPLACE_WITH_RUNTIME_SECRET` only. Helm chart defaults
  `secrets.create: false` and supports `existingSecret`. No real API keys, database passwords, tokens, or base64-encoded
  secrets.
- SecurityContext quality: `runAsNonRoot: true`, `allowPrivilegeEscalation: false`, `capabilities.drop: [ALL]`.
  `readOnlyRootFilesystem` noted as future hardening.
- Readiness/liveness quality: probes wired to `/actuator/health/readiness` and `/actuator/health/liveness`.
- Profile boundary quality: `security-api-key` and `observability-prometheus` profiles documented but NOT enabled by
  default. ConfigMap contains only non-sensitive values.
- Ingress boundary quality: Ingress disabled by default; no Ingress resource in raw K8s manifests.
- Docs harness quality: `K8sHelmFoundationDocsTest` verifies file existence, manifest content boundaries, Helm chart
  boundaries, docs status, deployment boundaries, and secret safety.
- Default offline quality: default validation does NOT require Kubernetes, Helm, kubectl, Docker, MySQL, PostgreSQL,
  PGvector, Redis, real LLMs, real embedding providers, or external network.
- Runtime non-change quality: V5.B.4.3 does NOT modify `src/main/java`, `src/main/resources`, `pom.xml`, Dockerfile,
  CI workflow, migration SQL, ToolRegistry, AgentApplicationService, RAG runtime, Spring Security runtime, health
  indicators, metrics recorder, Prometheus config, correlation filter, OpenAPI runtime, ToolCallTrace, Workspace, or
  Execution Tree.

Known limitations:

- V5.B.4.3 is deployment manifest foundation only. No `kubectl apply` or Helm release has been executed.
- No image registry push, no GitHub release workflow, no CD automation.
- No external secret manager, no sealed-secrets, no ExternalSecrets.
- No production Ingress with TLS, no cert-manager, no service mesh.
- No HPA, no NetworkPolicy, no production database provisioning.
- No OAuth2 / OIDC, no JWT runtime.
### V5.B.4.4 Release / Rollback Foundation (completed)

Status: completed for release governance and rollback runbook foundation. V5.B.4 current
scope completed.

Current V5.B.4.4 quality status:

- Release checklist quality: pre-flight checks, release info record, post-release
  verification defined. Template uses `REPLACE_WITH_*` placeholders only.
- Rollback runbook quality: 13-trigger matrix with symptom, detection, action, rollback,
  verify. Rollback strategy covers image, config, secret, profile, and migration caution.
- Image tag policy quality: `latest` prohibited; immutable tags (Git SHA, version tag)
  recommended. Rollback must use known-good immutable tag.
- Change record quality: template covers before/after, validation gates, verification,
  rollback, known issues, and sign-off.
- Automation boundary quality: no GitHub release workflow, no registry push, no
  semantic-release, no Helm/kubectl automation, no CD pipeline.
- Production deployment boundary quality: production deployment is not completed; no
  real release or rollback has been executed.
- Default offline quality: default validation does NOT require Docker, Helm, kubectl,
  Kubernetes, registry, real LLMs, or external network.
- Runtime non-change quality: V5.B.4.4 does NOT modify `src/main/java`, runtime config,
  Dockerfile, CI workflow, migration SQL, ToolRegistry, or any business runtime.
- Docs harness quality: `ReleaseRollbackFoundationDocsTest` verifies file existence,
  release policy content, non-goals, and secret safety.

Known limitations:

- No real release has been executed. No rollback has been executed. No image has been
  pushed to a registry.
- No GitHub release workflow, no CD pipeline, no registry credentials.
- No external secret manager, no production monitoring backend, no alerting.
- Flyway migration rollback not implemented. Image signing and SBOM are future work.
- Real refund / exchange / payment / logistics integrations are not connected.

Planned phases:

```text
V4.0 Pre-flight Fixes (completed)
V4.1 Tool / Skill Layer Foundation (completed)
V4.2 Spring AI Adapter (completed)
V4.3.1 PostgreSQL / PGvector Profile Boundary (completed)
V4.3.2 Vector Schema / Repository Contract (completed)
V4.3.3 Fake Vector Store / Default Offline Vector Tests (completed)
V4.3.4 Docker Compose / Opt-in Integration Docs (completed)
V4.4.1 Policy Ingestion Domain Model (completed)
V4.4.2 Chunking and Checksum Dedup (completed)
V4.4.3 Embedding Pipeline with Fake Provider (completed)
V4.4.4 Policy Ingestion Docs / Completion Record (completed)
V4.5.1 RAG Search Contract (completed)
V4.5.2 Keyword + Vector Merge Service (completed)
V4.5.3 search_aftersale_policy HYBRID Mode Wiring (completed)
V4.5.4 ToolCallTrace / Workspace Evidence Wiring (completed)
V4.6.1 RAG Evaluation Cases and Metrics (completed)
V4.6.2 V4 RAG Demo Script (completed)
V4.6.3 Actuator Health Indicators (completed)
V4.6.4 OpenAPI / API Docs Polish (completed)
V4.7 Documentation / Architecture / Final Closure (completed)
V4.7.1 Documentation Consistency / Secret Safety Audit (completed)
V4.7.2 Architecture Boundary / Offline Validation Closure (completed)
V4.7.3 Interview Demo / README Polish (completed)
V4.7.4 V4 Final Completion Record (completed)
V4.8 Future Skill / Execution Tree / Demo Extensions (planned)
V4.9 Future Spring Boot Completeness (planned)
```

### V4 不接受的退化

- 默认测试依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或外部网络；
- LLM 直接执行 Tool 或 Skill；
- Skill 绕过 ToolRegistry；
- Skill 直接访问 Repository、VectorStore、JdbcTemplate、Spring AI ChatClient 或 EmbeddingModel；
- RAG evidence 被当成最终业务动作；
- ToolCallTrace 丢失；
- Workspace 替代 ToolCallTrace；
- Execution Tree 查询修改业务状态；
- HIGH-risk 售后动作自动执行；
- Docker Compose 被写成生产部署方案；
- 真实 API Key、数据库密码、token、个人路径或 raw private data 进入仓库；
- 降低 ArchUnit、Checkstyle、SpotBugs 或 JUnit 约束。
