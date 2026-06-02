# ARCHITECTURE.md v0.1

# AfterSale-Agent 架构规范

## 1. 架构目标

AfterSale-Agent 采用 Java Spring Boot 模块化单体架构。

V1 的目标不是微服务拆分，而是在单体内建立清晰模块边界、分层依赖、Agent 工具边界、风险控制边界和可机械化验证的架构约束。

项目要证明：

> Agent 可以作为企业后端系统中的智能执行层，但它必须被业务边界、工具契约、人工确认、执行轨迹和架构规则约束。

## 2. 总体架构

```text
外部用户 / 客服人员
        │
        ▼
API Layer
        │
        ▼
Application Services
        │
        ├──────────────► Agent Orchestrator
        │                         │
        │                         ▼
        │                  Agent Tools
        │                         │
        ▼                         ▼
Domain Model              Business Services
        │                         │
        ▼                         ▼
Infrastructure Layer ───► Database / Cache / Search / LLM Provider
```

## 3. 推荐包结构

```text
io.github.tatame.aftersale
├── common
│   ├── api
│   ├── exception
│   ├── id
│   ├── time
│   └── validation
├── order
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── ticket
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── policy
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
├── agent
│   ├── api
│   ├── application
│   ├── domain
│   ├── infrastructure
│   └── prompt
├── tool
│   ├── application
│   ├── domain
│   └── infrastructure
├── trace
│   ├── api
│   ├── application
│   ├── domain
│   └── infrastructure
└── approval
    ├── api
    ├── application
    ├── domain
    └── infrastructure
```

## 4. 模块职责

### 4.1 common

通用基础设施。

允许包含：

* 统一响应结构；
* 统一异常；
* ID 生成；
* 时间工具；
* 参数校验；
* 通用枚举；
* 基础审计字段。

禁止包含：

* 售后业务逻辑；
* Agent 编排逻辑；
* 具体数据库访问；
* 具体 Prompt。

### 4.2 order

订单上下文。

负责：

* 查询订单；
* 查询用户订单；
* 暴露订单只读能力给售后和 Agent 工具；
* 维护订单领域模型。

V1 中订单模块可以使用模拟数据或内存数据源。

禁止：

* 在 order 模块中创建售后工单；
* 在 order 模块中调用 LLM；
* 让 Agent 直接修改订单支付状态。

### 4.3 ticket

工单上下文。

负责：

* 创建售后工单；
* 更新工单状态；
* 添加工单备注；
* 管理工单生命周期；
* 对外暴露工单查询接口。

禁止：

* 在 Controller 中写状态流转逻辑；
* 绕过 TicketApplicationService 修改工单状态；
* 让 Agent 直接操作 Repository。

### 4.4 policy

售后政策上下文。

负责：

* 管理售后政策文本；
* 检索相关政策；
* 为 Agent 提供政策依据；
* 支持后续 RAG 升级。

V1 可以使用简单文本检索；后续可替换为向量检索。

禁止：

* 将政策检索结果直接视为最终结论；
* 在 policy 模块中执行售后动作。

### 4.5 agent

Agent 编排上下文。

负责：

* 售后意图识别；
* 任务规划；
* 调用工具；
* 生成处理建议；
* 创建 AgentRun；
* 触发 trace 记录；
* 管理 Agent 执行状态。

禁止：

* 直接访问业务 Repository；
* 直接执行高风险动作；
* 绕过 tool 模块调用业务能力；
* 在 Service 中散落 Prompt 字符串；
* 生成无依据、无 trace 的最终建议。

### 4.6 tool

Agent 工具上下文。

负责：

* 定义工具契约；
* 统一封装业务能力；
* 记录工具输入输出；
* 控制工具风险级别；
* 为 Agent 提供可调用工具列表。

工具必须声明：

* toolName；
* description；
* inputSchema；
* outputSchema；
* riskLevel；
* whetherRequiresApproval。

### 4.7 trace

执行轨迹上下文。

负责：

* 记录 AgentRun；
* 记录 ToolCallTrace；
* 查询执行轨迹；
* 支持面试演示和审计。

禁止：

* 只记录最终结果，不记录中间步骤；
* 隐藏失败工具调用；
* 在 trace 中存储敏感明文数据。

### 4.8 approval

人工确认上下文。

负责：

* 表达高风险动作审批；
* 保存待确认动作；
* 审批通过后触发实际业务动作；
* 拒绝后回写工单备注。

V1 可以只实现数据结构和状态，不需要完整 UI。

## 5. 分层规则

每个业务域内部采用：

```text
api → application → domain → infrastructure
```

### 5.1 api 层

负责：

* HTTP 请求响应；
* DTO 转换；
* 参数校验；
* 调用 application service。

禁止：

* 写业务逻辑；
* 直接访问 Repository；
* 调用 LLM；
* 直接拼接 Prompt；
* 执行状态流转判断。

### 5.2 application 层

负责：

* 编排领域对象；
* 调用领域服务；
* 调用基础设施接口；
* 管理事务边界；
* 处理用例级流程。

禁止：

* 混入 HTTP 细节；
* 直接依赖 Controller；
* 写大量基础设施代码；
* 将 Prompt 散落到多个 service 中。

### 5.3 domain 层

负责：

* 领域实体；
* 值对象；
* 领域枚举；
* 领域规则；
* 领域服务接口。

禁止：

* 依赖 Spring Web；
* 依赖数据库实现；
* 依赖 Redis、MQ、LLM SDK；
* 依赖基础设施细节。

### 5.4 infrastructure 层

负责：

* 数据库访问；
* 外部 API 调用；
* Redis；
* MQ；
* 向量检索；
* LLM Provider 适配；
* 第三方 SDK 封装。

禁止：

* 承载核心业务规则；
* 绕过 application service 被 Controller 直接调用。

## 6. 依赖方向

允许：

```text
api → application
application → domain
application → infrastructure interface / adapter
infrastructure → domain
agent → tool
agent → trace
tool → business application services
```

禁止：

```text
api → infrastructure
api → repository
api → LLM SDK
domain → api
domain → infrastructure
domain → Spring Web
order → agent
ticket → agent
policy → agent
trace → agent
business repository → agent
```

## 7. Agent 工具调用架构

Agent 不直接访问业务模块 Repository。

正确路径：

```text
AgentOrchestrator
  → ToolRegistry
  → ToolExecutor
  → Specific Tool
  → Business Application Service
  → Domain / Infrastructure
```

错误路径：

```text
AgentOrchestrator → OrderRepository
AgentOrchestrator → TicketRepository
AgentOrchestrator → PaymentClient
AgentOrchestrator → SQL
```

## 8. 工具风险等级

### 8.1 LOW

只读或低风险写入。

示例：

* 查询订单；
* 查询政策；
* 添加内部备注。

### 8.2 MEDIUM

会改变工单状态，但不涉及资金、库存或用户权益。

示例：

* 更新工单状态为待人工审核；
* 标记工单需要补充材料。

### 8.3 HIGH

涉及资金、库存、权益、争议关闭等动作。

示例：

* 发起退款；
* 发放补偿；
* 换货占用库存；
* 关闭争议工单。

HIGH 工具必须进入 approval 模块，Agent 不得直接执行。

## 9. 状态机设计

### 9.1 TicketStatus

```text
CREATED
AGENT_RUNNING
WAITING_USER_INFO
WAITING_HUMAN_APPROVAL
PROCESSING
RESOLVED
REJECTED
CLOSED
FAILED
```

### 9.2 AgentRunStatus

```text
PENDING
RUNNING
SUCCEEDED
FAILED
CANCELLED
```

### 9.3 ToolCallStatus

```text
PENDING
RUNNING
SUCCEEDED
FAILED
SKIPPED
REQUIRES_APPROVAL
```

## 10. 数据存储原则

V1 推荐 MySQL 表：

```text
orders
tickets
aftersale_policies
agent_runs
tool_call_traces
approval_requests
```

V1 可先使用内存数据或 H2 启动，后续切换 MySQL。

关键原则：

* Agent 执行结果必须可持久化；
* 工具调用必须可追踪；
* 失败原因必须保存；
* 高风险动作必须有关联审批记录。

## 11. RAG 架构原则

V1 不追求复杂 RAG。

推荐演进：

```text
V1：简单关键词 / BM25 / 内存政策检索
V2：向量库 + embedding
V3：混合检索 + rerank + 引用依据评估
```

无论哪种检索方式，Agent 最终建议必须包含政策依据。

## 12. Prompt 管理原则

Prompt 不允许散落在业务 Service 中。

推荐位置：

```text
agent/prompt/
docs/agent/PROMPT_GUIDE.md
```

Prompt 必须版本化。

Agent 输出必须尽量结构化，特别是：

* intent；
* plan；
* requiredTools；
* riskLevel；
* finalSuggestion；
* evidence。

## 13. API 设计原则

API 只暴露用例，不暴露内部实现。

V1 核心 API：

```text
POST /api/tickets
POST /api/tickets/{ticketId}/agent-runs
GET  /api/tickets/{ticketId}
GET  /api/agent-runs/{runId}/traces
```

响应结构统一：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

## 14. 可观测性原则

至少记录：

* requestId；
* ticketId；
* agentRunId；
* toolName；
* latencyMs；
* status；
* errorMessage。

禁止使用裸 `System.out.println` 输出业务日志。

推荐使用结构化日志。

## 15. 机械化架构约束草案

V1 至少实现以下 ArchUnit 规则：

### Rule 1：Controller 不得访问 Repository

```text
..api.. 不得依赖 ..infrastructure..repository..
```

### Rule 2：Domain 不得依赖 Spring Web

```text
..domain.. 不得依赖 org.springframework.web..
```

### Rule 3：Agent 不得直接访问业务 Repository

```text
..agent.. 不得依赖 ..order..infrastructure..repository..
..agent.. 不得依赖 ..ticket..infrastructure..repository..
..agent.. 不得依赖 ..policy..infrastructure..repository..
```

### Rule 4：业务模块不得依赖 Agent 模块

```text
..order.. 不得依赖 ..agent..
..ticket.. 不得依赖 ..agent..
..policy.. 不得依赖 ..agent..
```

### Rule 5：Controller 命名规则

```text
..api.. 中的 Controller 类必须以 Controller 结尾
```

## 16. 技术栈边界

V1 默认：

* Java 17+
* Spring Boot 3.x
* Maven
* JUnit 5
* ArchUnit
* Checkstyle
* SpotBugs
* H2 或 MySQL
* Redis 可选
* Spring AI 或 LangChain4j 二选一

引入新技术必须写入 docs/decisions。

## 17. 不做微服务的原因

V1 不采用微服务。

原因：

* 项目核心是验证 Agent + 售后工单 + Harness 闭环；
* 微服务会增加部署和调试复杂度；
* 模块化单体更适合简历项目和本地演示；
* 清晰包边界 + ArchUnit 约束足以表达架构能力。

## 18. 架构成功标准

V1 架构成功的标志：

* 模块边界清晰；
* Controller 无业务逻辑；
* Agent 只通过工具访问业务能力；
* 高风险动作有审批边界；
* Trace 可查询；
* ArchUnit 能阻止关键违规；
* 项目能一键测试；
* 后续智能体能根据文档继续开发。

## 19. V1 最终实现说明

最终 V1 采用内存仓储和规则式 Agent 编排，不接入真实 LLM、真实数据库、Redis、支付、物流或库存系统。

Demo AgentRun 的实际工具链为：

```text
search_aftersale_policy
add_ticket_note
```

订单上下文在 V1 中保留为工单上的 `orderId`。可执行的 `get_order_by_id` 和 `get_user_orders` 工具未进入最终
V1，作为 V2 扩展方向。该收口不改变架构边界：Agent 仍只能通过工具访问业务能力，工具仍不能直接访问
Repository，高风险动作仍必须进入人工确认边界。

## 20. V2 Planner 架构边界

V2 引入 `AgentPlanner` 抽象。

推荐结构：

```text
agent
├── api
├── application
│   ├── AgentApplicationService
│   ├── planner
│   │   ├── AgentPlanner
│   │   ├── AgentPlanningContext
│   │   ├── AgentPlan
│   │   ├── RuleBasedAgentPlanner
│   │   └── FakeAgentPlanner
│   ├── validation
│   │   └── AgentPlanValidator
│   └── ...
├── infrastructure
│   └── llm
│       ├── LlmAgentPlanner
│       ├── LlmClientAdapter
│       └── LlmPlannerProperties
└── prompt
```

### 20.1 依赖规则

允许：

```text
AgentApplicationService → AgentPlanner
RuleBasedAgentPlanner → domain / planner DTO
FakeAgentPlanner → planner DTO
LlmAgentPlanner → LlmClientAdapter
LlmClientAdapter → external LLM SDK / Spring AI
AgentApplicationService → ToolRegistry
```

禁止：

```text
AgentApplicationService → concrete LLM SDK
Controller → LlmAgentPlanner
LLM SDK adapter → TicketRepository
LLM SDK adapter → ToolRegistry
LLM → direct tool execution
LLM → direct business state mutation
```

### 20.2 设计原则

- `AgentApplicationService` 依赖 `AgentPlanner` 抽象，不依赖具体 LLM SDK；
- `RuleBasedAgentPlanner` 保留 V1 规则行为；
- `FakeAgentPlanner` 用于测试；
- `LlmAgentPlanner` 只生成 `AgentPlan`；
- `AgentPlan` 必须被 Java 后端校验；
- ToolRegistry 仍然是唯一工具执行入口；
- LLM Provider 归入 infrastructure / adapter 边界；
- 所有工具调用继续记录 ToolCallTrace。

### 20.3 V2 ArchUnit 候选规则

后续可以增加：

```text
AgentApplicationService 不得依赖 ..infrastructure..llm..
..api.. 不得依赖 LlmAgentPlanner
..infrastructure..llm.. 不得依赖 ..repository..
..agent..prompt.. 不得依赖 ..repository..
```

## 21. V2.3 Multi-Intent Planning 架构边界

V2.3 引入 Multi-Intent Planning，用于把一个复杂售后 Ticket 拆解为多个结构化子任务。

目标链路：

```text
Ticket
→ Supervisor Planner
→ MultiIntentAgentPlan
→ Java validation
→ AgentApplicationService
→ ToolRegistry
→ ToolCallTrace
```

### 21.1 Supervisor Planner

Supervisor Planner 负责生成 `MultiIntentAgentPlan`。

它可以：

- 识别复杂售后诉求中的多个意图；
- 生成多个 `AgentSubtask`；
- 为每个子任务声明 `SubtaskType`、目标对象、用户原文片段、优先级、风险等级、政策 query、计划工具和依赖；
- 输出结构化 JSON。

它不得：

- 直接执行子任务；
- 直接调用工具；
- 直接修改 Ticket、Order、AgentRun、ToolCallTrace；
- 声称高风险动作已经完成。

### 21.2 AgentApplicationService

`AgentApplicationService` 负责校验并执行子任务计划。

V2.3 当前阶段采用单进程顺序执行：

```text
validate all subtasks
→ sort by dependencies / priority
→ execute planned tools through ToolRegistry
→ append evidence
→ produce final summary
```

V2.3 不做并行执行，不引入消息队列，不引入投票共识。

### 21.3 Specialist Handler Boundary

Specialist Agent Handler 是 V2.4 之后的扩展内容，不在 V2.3 实现。

V2.3 可以先使用统一的子任务执行路径。后续如需引入 `ReturnSubtaskHandler`、`ExchangeSubtaskHandler`、
`CouponConsultationHandler` 等 specialist handler，必须继续遵守：

- 不绕过 ToolRegistry；
- 不直接访问 Repository；
- 不直接执行真实退款、换货、优惠券补偿；
- 保持 ToolCallTrace 可审计。

### 21.4 Trace Boundary

ToolCallTrace 继续记录每个工具调用。

V2.3 可以先保留 trace list，不强制实现 Execution Tree。后续可以扩展为：

```text
AgentRun
└── AgentSubtask
    └── ToolCallTrace
```

但在 V2.3 中，trace list 必须足以看出每个工具调用的输入、输出、状态和失败原因。

### 21.5 V2.3 Current Boundary

V2.3 当前实现 Multi-Intent Planning 的基础链路，后续扩展必须保持：

- `Supervisor Planner` 只生成 `MultiIntentAgentPlan`；
- `AgentApplicationService` 负责校验子任务类型、工具名、风险等级和依赖关系；
- 当前阶段使用单进程顺序执行，不做并行调度；
- Specialist Agent Handler 属于 V2.4，不在 V2.3 实现；
- ToolRegistry 仍然是所有工具调用的唯一入口；
- ToolCallTrace 继续记录每个实际工具调用；
- 后续可以扩展 Execution Tree，但 V2.3 不要求替换现有 trace list。

## 22. V2.4 Specialist Agent Handler 架构边界

V2.4 已在 V2.3 的多子任务执行基础上引入 Specialist Agent Handler。

目标链路：

```text
AgentApplicationService
→ SpecialistAgentHandlerRegistry
→ SpecialistAgentHandler
→ ToolRegistry
→ ToolCallTrace
```

### 22.1 AgentApplicationService Boundary

`AgentApplicationService` 负责：

- 校验 `AgentPlan`；
- 按 priority / dependencies 排序 subtasks；
- 通过 `SpecialistAgentHandlerRegistry` 查找 handler；
- 调度 handler；
- 汇总 `SubtaskExecutionResult`；
- 维护 AgentRun / Ticket 状态。

`AgentApplicationService` 不应继续直接写每类子任务的具体处理细节，例如 RETURN、EXCHANGE、COUPON_CONSULTATION、
LOGISTICS_ISSUE 的差异化工具顺序、提示语和结果汇总。

### 22.2 SpecialistAgentHandlerRegistry Boundary

`SpecialistAgentHandlerRegistry` 负责：

- 注册所有 `SpecialistAgentHandler`；
- 根据 `SubtaskType` 查找唯一 handler；
- 拒绝重复 handler 覆盖同一 `SubtaskType`；
- 对未覆盖类型给出清晰 fallback 或错误；
- 暴露支持类型集合用于测试和诊断。

### 22.3 SpecialistAgentHandler Boundary

每个 `SpecialistAgentHandler` 只处理自己支持的 `SubtaskType`。

示例：

```text
ReturnAgentHandler → RETURN
ExchangeAgentHandler → EXCHANGE
CouponAgentHandler → COUPON_CONSULTATION
LogisticsAgentHandler → LOGISTICS_ISSUE
GeneralConsultationHandler → GENERAL_CONSULTATION
HumanEscalationHandler → HUMAN_ESCALATION / UNKNOWN
```

Handler 可以：

- 读取 `SubtaskExecutionContext`；
- 选择当前子任务需要调用的已注册工具；
- 通过 ToolRegistry 调用工具；
- 生成 `SubtaskExecutionResult`；
- 返回 evidence、summary、toolCalls 和 requiresHumanApproval。

Handler 不得：

- 访问 Repository；
- 绕过 ToolRegistry；
- 绕过 RiskPolicy；
- 直接调用 LLM；
- 直接修改 Ticket、Order、AgentRun 或 ToolCallTrace；
- 直接执行真实退款、真实换货、真实优惠券补偿、支付变更、物流变更或争议关闭。

### 22.4 V2.4 ArchUnit 规则

V2.4 已增加：

```text
..agent..handler.. 不得依赖 ..repository..
..agent..handler.. 不得依赖 ..infrastructure..llm..
..agent..handler.. 不得依赖 org.springframework.web..
SpecialistAgentHandler 实现类不得依赖业务 Repository
```

### 22.5 Trace Boundary

ToolCallTrace 继续记录所有 handler 内部工具调用。

V2.4 不要求实现 Execution Tree。后续 V2.7 可以把 handler 执行结果映射为：

```text
AgentRun
└── AgentSubtask
    └── SpecialistAgentHandler
        └── ToolCallTrace
```

V2.4 阶段仍可保留 trace list，但必须能看出 handler 调用了哪些工具、工具输入输出、状态和失败原因。

## 23. V2.5 Policy Retrieval Tool 架构边界

V2.5 将售后政策检索明确为可替换的工具链路：

```text
SpecialistAgentHandler
→ ToolRegistry
→ SearchAfterSalePolicyToolExecutor
→ PolicyApplicationService
→ PolicyRepository
→ InMemoryPolicyRepository
```

### 23.1 Policy Model Boundary

政策检索模型包括：

- `PolicySearchQuery`：受控检索输入；
- `PolicySnippet`：命中的政策片段；
- `PolicySearchResult`：结构化检索结果和空结果消息。

`PolicyRepository` 是检索抽象，当前由 `InMemoryPolicyRepository` 通过本地关键词匹配实现。后续可以替换为
Spring AI VectorStore、PGvector 或混合检索，但不得改变 Handler 只能通过 `ToolRegistry` 调用检索工具的边界。

### 23.2 Handler Boundary

Specialist Handler 不得直接访问 `PolicyRepository` 或 `InMemoryPolicyRepository`。Handler 必须调用
`search_aftersale_policy` 工具，让 ToolRegistry 继续负责工具风险控制和 ToolCallTrace 记录。

Handler 在执行动作工具前必须先获取政策检索结果。当前动作工具主要是低风险的 `add_ticket_note`，不包含真实退款、
真实换货、真实优惠券补偿、支付变更或物流变更。

### 23.3 Empty Result Boundary

无匹配政策时，工具必须返回结构化空结果和清晰 message，不得编造政策片段，不得把空结果包装为成功依据。

## 24. V2.6 Agent Workspace / Structured Memory 架构边界

V2.6 计划引入单次 `AgentRun` 内的结构化工作记忆：

```text
AgentRun
→ AgentWorkspace
→ SpecialistAgentHandler
→ ToolRegistry
→ ToolCallTrace
→ AgentWorkspace
→ final summary
```

### 24.1 Ownership Boundary

`AgentWorkspace` 属于 `agent/application` 或 `agent/domain` 边界。它是 Agent 执行上下文模型，不属于
`tool`、`trace`、`order`、`ticket` 或 `policy` 模块。

候选结构：

```text
AgentWorkspace
├── OrderFact
├── PolicyEvidence
├── SubtaskMemory
├── ToolResultSummary
└── RiskFlag
```

### 24.2 Handler Boundary

Specialist Handler 可以读取和写入 workspace：

- 读取前序 `OrderFact`；
- 读取前序 `PolicyEvidence`；
- 读取前序 `SubtaskMemory`；
- 写入本次工具调用后的 `ToolResultSummary`；
- 写入本次子任务结果；
- 写入风险标记。

Handler 仍不得直接访问 Repository、不得直接调用 LLM、不得绕过 ToolRegistry、不得直接修改 Ticket / Order /
AgentRun / ToolCallTrace。

### 24.3 ToolRegistry Boundary

ToolRegistry 仍然只负责：

- 注册工具定义；
- 执行工具；
- 应用工具风险边界；
- 触发 ToolCallTrace 记录。

ToolRegistry 不负责存储全局上下文，不负责持有 `AgentWorkspace`，也不负责生成 final summary。

### 24.4 ToolCallTrace Boundary

ToolCallTrace 是审计记录，不是主要工作记忆。

允许：

- workspace 中保存 `traceId` 或工具结果摘要；
- ToolCallTrace 继续保存完整工具输入、输出、状态、错误和延迟。

禁止：

- 用 workspace 替代 ToolCallTrace；
- 依赖 ToolCallTrace 作为 handler 间的主上下文传递机制；
- 为了 workspace 简化或删除 trace 记录。

### 24.5 Safety Boundary

Workspace 不得：

- 绕过 ToolRegistry 调用工具；
- 直接访问 Repository；
- 保存 API Key；
- 保存敏感凭证；
- 保存完整长 prompt；
- 保存 LLM 原始长文本；
- 保存长期用户画像；
- 保存跨会话记忆。

Workspace 可以作为后续 Execution Tree、Evaluation Dataset 和 Approval APIs 的上下文来源，但 V2.6 不直接实现这些能力。

## 25. V3 Infrastructure 架构边界

V3 引入基础设施收口目标，但不改变模块化单体和 Agent 执行边界。

### 25.1 Persistence Boundary

Repository 允许出现 MySQL / JPA / Jdbc 实现，但实现必须位于各业务模块的 `infrastructure` 层。

当前 V3.1 实现采用 Spring JDBC adapter，不采用 JPA entity。数据库映射代码只存在于 infrastructure
repository，domain 通过纯 Java restore factory 支持持久化重建。

允许：

```text
ticket/application → TicketRepository
ticket/infrastructure → MySQL/JPA/Jdbc adapter
trace/application → ToolCallTraceRepository
approval/application → ApprovalRepository
order/application → OrderRepository
policy/application → PolicyRepository
```

禁止：

```text
domain → JPA entity / Jdbc template / datasource
api → repository
agent → business repository
handler → business repository
controller → infrastructure repository
persistence adapter → bypass ApplicationService for business flow
```

### 25.2 Profile Boundary

V3 必须保留 in-memory repository，用于 `test` 或 `dev-simple` profile。

MySQL repository 只在明确的 MySQL profile 中启用。默认测试不得强制依赖本地 MySQL、Docker、真实 LLM、Redis、
向量库或外部网络。

### 25.3 Domain Boundary

Domain 层不得依赖数据库框架。

领域模型可以表达业务状态、ID、时间和风险边界，但不得出现 JPA annotation、Jdbc API、Spring Data repository、
database migration 细节或 datasource 配置。

### 25.4 Agent Boundary

Agent、Planner、Workspace 和 Specialist Handler 仍不得直接访问 Repository。即使 Repository 已有 MySQL 实现，
Agent 执行路径仍必须是：

```text
AgentApplicationService
→ ToolRegistry
→ ToolExecutor
→ Business Application Service
→ Repository abstraction
→ infrastructure implementation
```

Persistence 不得成为绕过 ToolRegistry、Approval、RiskPolicy、ToolCallTrace 或 ApplicationService 的后门。

### 25.5 Docker Compose Boundary

Docker Compose 只负责本地开发环境。

它可以启动：

- app；
- mysql；
- optional redis。

它不代表生产部署，不承诺高可用、备份、密钥管理、监控告警或云环境配置。真实密钥不得写入 compose 文件、
README、测试、代码或提交历史。

### 25.6 Observability Boundary

V3 structured logging 应包含 requestId、ticketId、agentRunId、subtaskId、toolName 和 approvalRequestId 等字段。

日志不得替代 ToolCallTrace、Execution Tree 或持久化状态；日志也不得输出 API Key、数据库密码、完整长 prompt、
LLM 原始长文本或敏感凭证。

## V4 RAG / Tool / Skill 架构补充

V4 在现有模块化单体基础上新增 Tool / Skill / RAG 分层，但不改变既有核心原则：Planner 只规划，Java 后端校验和执行，
ToolRegistry 是 Agent 原子工具执行入口，Skill 是复合任务能力且 does not replace ToolRegistry，Approval 拦截高风险动作，
ToolCallTrace 保留审计记录。

### V4 推荐包结构

```text
io.github.tatame.aftersale
├── agent
│   ├── application
│   │   ├── planner
│   │   ├── skill
│   │   │   ├── AgentSkill
│   │   │   ├── SkillRegistry
│   │   │   ├── SkillDefinition
│   │   │   ├── SkillExecutionContext
│   │   │   └── SkillExecutionResult
│   │   └── workspace
│   └── infrastructure
│       └── springai
├── policy
│   ├── rag
│   │   ├── application
│   │   ├── domain
│   │   └── infrastructure
│   │       ├── pgvector
│   │       └── springai
│   ├── application
│   ├── domain
│   └── infrastructure
└── tool
    ├── application
    ├── domain
    └── infrastructure
```

### V4 执行链路

```text
Planner
→ AgentPlan / AgentSubtask
→ Specialist Handler or SkillRegistry
→ AgentSkill when selected
→ ToolRegistry
→ ToolExecutor
→ ToolCallTrace
→ AgentWorkspace
→ Execution Tree
```

### RAG 政策检索链路

```text
Specialist Handler or AgentSkill
→ ToolRegistry
→ search_aftersale_policy
→ RagPolicySearchApplicationService
→ keyword policy retrieval + EmbeddingClient contract + PolicyVectorRepository contract
→ RagPolicySearchResult
→ ToolCallTrace
→ AgentWorkspace.PolicyEvidence
→ Execution Tree
```

RAG retrieval 是 policy evidence source，不是业务决策或业务动作执行。`search_aftersale_policy` 是 LOW-risk read-only
tool，不执行真实退款、换货、补偿、支付、物流、库存或争议关闭。

### 允许依赖

```text
AgentApplicationService → AgentPlanner
AgentApplicationService → SkillRegistry
AgentSkill → ToolRegistry
AgentSkill → AgentWorkspace
RagPolicySearchApplicationService → EmbeddingClient
RagPolicySearchApplicationService → PolicyVectorRepository
PolicyEmbeddingPipelineService → EmbeddingClient
PolicyEmbeddingPipelineService → PolicyVectorRepository
PolicyEmbeddingService → EmbeddingClient
SpringAiChatClientAdapter → Spring AI ChatClient
SpringAiEmbeddingClientAdapter → Spring AI EmbeddingModel
```

### 禁止依赖

```text
Controller → Repository
Controller → VectorStore
AgentSkill → Repository
AgentSkill → VectorStore
AgentSkill → JdbcTemplate / DataSource
AgentSkill → Spring AI ChatClient / EmbeddingModel
SpecialistAgentHandler → Repository
SpecialistAgentHandler → VectorStore
LLM Planner → ToolRegistry execution
LLM Planner → VectorStore
ToolExecutor → direct real refund/payment/logistics APIs
Agent / Handler / Skill → EmbeddingClient
Agent / Handler / Skill → PolicyVectorRepository
Agent / Handler / Skill → PolicyIngestionRepository
Agent / Handler / Skill → OpenAPI config
Agent / Handler / Skill → Actuator health indicators
```

EmbeddingClient / PolicyVectorRepository 是 application / infrastructure contract，不直接暴露给 Agent、Handler 或 Skill。
Policy Ingestion 是 admin/offline pipeline，不是 Agent runtime tool。ToolCallTrace 是审计 source of truth；Workspace 是单次
AgentRun 工作记忆，不是长期记忆；Execution Tree 是只读解释视图，不修改业务状态。

### V4 架构测试目标

ArchitectureTest 应新增或保持以下约束：

1. AgentSkill 不得依赖 Repository；
2. AgentSkill 不得依赖 VectorStore / pgvector infrastructure；
3. AgentSkill 不得依赖 Spring AI clients；
4. Specialist Handler 不得依赖 VectorStore；
5. Controller 不得直接访问 PolicyVectorRepository；
6. RAG infrastructure 不得依赖 Agent application；
7. default profile 不创建真实 vector datasource；
8. ToolRegistry 仍是 ToolExecutor 唯一入口。
9. RAG evaluation 不写 ToolCallTrace / Workspace / Execution Tree；
10. RAG health 不调用 ToolRegistry / AgentRun / EmbeddingClient.embed / PolicyVectorRepository.search；
11. OpenAPI config 不访问 Repository、EmbeddingClient、PolicyVectorRepository、PGvector、DataSource 或 JdbcTemplate；
12. Policy Ingestion 保持 admin/offline pipeline，不成为 Agent runtime tool。

### V4 默认离线验证边界

默认测试和默认 Spring context 必须保持 offline、deterministic：

- 不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或 external network；
- 不创建真实 DataSource、PGvector live connection、Spring AI ChatModel、Spring AI EmbeddingModel 或 Spring AI VectorStore；
- RAG Actuator health 是 offline readiness signal，不是 live PGvector / provider connectivity proof；
- live provider、live embedding、live MySQL、live PGvector 验证必须显式 opt-in，并在缺少配置时 skip 或给出清晰 setup
  message。
