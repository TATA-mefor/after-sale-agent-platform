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
com.example.aftersale
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
