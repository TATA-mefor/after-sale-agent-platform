# SPEC.md v0.1

# AfterSale-Agent：面向电商售后的企业级工单 Agent 平台

## 1. 项目定位

AfterSale-Agent 是一个基于 Java Spring Boot 的企业级售后工单 Agent 平台。

它不是普通 AI 客服，也不是纯 RAG 问答系统，而是一个将电商售后业务、企业工单流转、Agent 工程能力和 Harness Engineering 工程方法结合起来的项目。

项目目标是：

> 当用户提出售后诉求时，系统能够创建售后工单，由 Agent 读取订单信息、检索售后政策、规划处理步骤、调用业务工具、生成处理建议，并在高风险动作前进入人工确认流程。

## 2. 为什么需要这个系统

传统电商售后系统通常依赖人工客服处理以下问题：

* 用户描述不标准，需要人工理解意图；
* 售后政策复杂，需要人工查询规则；
* 订单、物流、商品、用户历史记录分散在多个系统；
* 退款、换货、补偿等动作存在误操作风险；
* 工单处理过程缺少结构化执行轨迹；
* 新客服培训成本高，规则变更后响应不稳定。

AfterSale-Agent 要解决的问题不是“自动聊天”，而是“在可控边界内自动推进售后工单处理”。

## 3. 目标用户

### 3.1 外部用户

电商平台消费者。

他们通过自然语言表达售后诉求，例如：

* “我买的耳机用了两天左耳没声音，想退货。”
* “快递显示签收了，但我没收到。”
* “衣服尺码不合适，可以换大一码吗？”
* “商品坏了，我想申请维修。”

### 3.2 内部用户

电商平台客服、售后审核员、运营人员。

他们需要：

* 查看 Agent 的处理建议；
* 审核高风险动作；
* 查看工单状态；
* 查看执行轨迹；
* 分析 Agent 处理质量。

## 4. 项目边界

### 4.1 V1 范围内

V1 只实现最小可用闭环：

1. 用户提交售后问题；
2. 系统创建售后工单；
3. Agent 识别售后意图；
4. Agent 查询模拟订单；
5. Agent 检索售后政策；
6. Agent 生成处理计划；
7. Agent 调用只读工具或低风险工具；
8. 系统记录 Agent 执行轨迹；
9. 系统返回处理建议。

### 4.2 V1 暂不实现

以下能力暂不进入 V1：

* 真实支付退款；
* 真实物流 API；
* 复杂多 Agent 协作；
* 完整前端后台；
* 大规模向量数据库部署；
* 真实生产级权限系统；
* 自动发放补偿金；
* 长期用户画像系统。

这些能力可以作为 V2 / V3 扩展。

## 5. 核心业务对象

### 5.1 Order

订单信息。

关键字段：

* orderId
* userId
* productId
* productName
* orderStatus
* paidAmount
* paidAt
* deliveredAt
* aftersaleDeadline

### 5.2 Ticket

售后工单。

关键字段：

* ticketId
* userId
* orderId
* rawUserMessage
* intentType
* priority
* status
* internalNote
* agentSuggestion
* createdAt
* updatedAt

### 5.3 AfterSalePolicy

售后政策。

关键字段：

* policyId
* category
* productType
* policyText
* effectiveFrom
* effectiveTo

### 5.4 AgentRun

Agent 一次执行记录。

关键字段：

* runId
* ticketId
* status
* planJson
* finalAnswer
* errorMessage
* startedAt
* finishedAt

### 5.5 ToolCallTrace

工具调用轨迹。

关键字段：

* traceId
* runId
* toolName
* inputJson
* outputJson
* status
* latencyMs
* errorMessage
* createdAt

## 6. Agent 能力定义

V1 Agent 至少具备以下能力：

### 6.1 Intent Classification

识别售后意图：

* REFUND_ONLY：仅退款；
* RETURN_AND_REFUND：退货退款；
* EXCHANGE：换货；
* REPAIR：维修；
* LOGISTICS_ISSUE：物流问题；
* GENERAL_CONSULTATION：普通咨询；
* UNKNOWN：无法识别。

### 6.2 Planning

根据用户问题、订单信息和政策检索结果生成处理计划。

计划必须是结构化 JSON，而不是纯自然语言。

### 6.3 Tool Calling

Agent 可以调用以下工具：

* get_order_by_id
* get_user_orders
* search_aftersale_policy
* create_aftersale_ticket
* update_ticket_status
* add_ticket_note

V1 中不允许 Agent 直接执行真实退款。

### 6.4 RAG

Agent 能基于售后政策知识库检索相关规则。

V1 可以先使用简单文本检索或轻量向量检索，后续再升级为更完整的 RAG 模块。

### 6.5 Traceability

Agent 的每一步工具调用必须被记录。

面试时必须能展示：

* Agent 为什么这么判断；
* 调用了哪些工具；
* 输入输出是什么；
* 哪一步失败；
* 最终建议来自哪些信息。

## 7. 风险边界

Agent 不得直接执行以下动作：

* 真实退款；
* 发放补偿；
* 删除订单；
* 修改支付状态；
* 修改用户等级；
* 绕过人工审核关闭高风险工单。

这些动作必须进入人工确认流程。

## 8. Harness 工程要求

本项目必须体现 Harness Engineering 思想。

因此仓库中必须包含：

* SPEC.md：项目目标和边界；
* WORKFLOW.md：任务执行流程；
* AGENTS.md：智能体入口导航；
* ARCHITECTURE.md：架构分层与依赖规则；
* docs/exec-plans/：执行计划；
* docs/decisions/：决策日志；
* docs/quality/QUALITY_SCORE.md：质量评分；
* ArchUnit 测试：机械化架构约束；
* Checkstyle / SpotBugs：代码质量约束；
* JUnit 测试：核心业务验证；
* CI 配置：自动验证背压。

## 9. 技术栈约束

V1 建议技术栈：

* Java 17+
* Spring Boot 3.x
* Maven
* MySQL 8
* Redis
* Spring AI 或 LangChain4j
* JUnit 5
* ArchUnit
* Checkstyle
* SpotBugs
* Docker Compose

V1 暂不追求复杂微服务，优先采用模块化单体。

## 10. 推荐模块结构

```text
after-sale-agent-platform/
├── docs/
│   ├── exec-plans/
│   ├── decisions/
│   └── quality/
├── src/main/java/com/example/aftersale/
│   ├── common/
│   ├── order/
│   ├── ticket/
│   ├── policy/
│   ├── agent/
│   ├── tool/
│   ├── trace/
│   └── approval/
├── src/test/java/com/example/aftersale/
└── pom.xml
```

## 11. 架构原则

### 11.1 分层原则

每个业务域遵循：

```text
api → application → domain → infrastructure
```

禁止：

* api 直接访问 repository；
* domain 依赖 Spring Web；
* agent 直接修改订单核心数据；
* tool 层绕过业务服务访问数据库；
* controller 中写业务逻辑。

### 11.2 Agent 安全原则

* 只读工具默认允许；
* 写操作工具必须分级；
* 高风险工具必须人工确认；
* 所有工具调用必须记录 trace；
* Agent 输出不能直接作为最终事实，必须附带依据。

## 12. V1 核心 API

### 12.1 创建售后工单

```http
POST /api/tickets
```

请求示例：

```json
{
  "userId": "U1001",
  "orderId": "O202605130001",
  "message": "我买的耳机用了两天左耳没声音了，想退货。"
}
```

### 12.2 触发 Agent 处理

```http
POST /api/tickets/{ticketId}/agent-runs
```

### 12.3 查询 Agent 执行轨迹

```http
GET /api/agent-runs/{runId}/traces
```

### 12.4 查询工单详情

```http
GET /api/tickets/{ticketId}
```

## 13. V1 验收标准

V1 完成时，必须满足：

1. 能通过接口创建售后工单；
2. 能模拟查询订单；
3. 能识别至少 5 类售后意图；
4. 能检索售后政策；
5. 能生成结构化处理计划；
6. 能记录 AgentRun 和 ToolCallTrace；
7. 能返回清晰处理建议；
8. 至少有 5 个核心 JUnit 测试；
9. 至少有 3 条 ArchUnit 架构规则；
10. 项目文档包含 SPEC、WORKFLOW、AGENTS、ARCHITECTURE；
11. 项目可通过 Maven 一键测试；
12. 项目可本地启动演示。

## 14. 非目标

本项目 V1 不追求：

* 替代真实客服系统；
* 连接真实支付系统；
* 完整商业化上线；
* 训练大模型；
* 实现通用 Agent 框架；
* 追求复杂 UI。

V1 的目标是：

> 用最小闭环证明“Java 后端业务系统 + Agent 执行层 + Harness 工程约束”是可行的。

## 15. 成功标准

这个项目成功的标志不是代码量大，而是：

* HR 能一眼看懂业务场景；
* Java 面试官能看到后端工程深度；
* Agent 面试官能看到 Tool Calling、RAG、Planning、Trace；
* 架构面试官能看到模块边界、状态流转、风险控制；
* 项目组能看到真实落地价值；
* Harness 思想能作为项目差异化被讲清楚。

## 16. V2 目标：真实 LLM Planner Adapter

V1 使用规则型 Agent 完成售后工单处理闭环。

V2 的第一目标是引入真实 LLM Planner Adapter，使 Agent 能够更好地理解用户自然语言售后诉求，并生成结构化处理计划。

V2.1 采用以下边界：

```text
LLM 负责规划
Java 后端负责执行
ToolRegistry 负责工具调用
RiskPolicy 负责风险边界
ToolCallTrace 负责审计记录
```

V2.1 不要求 LLM 直接调用工具。

V2.1 不改变高风险动作边界。

V2.1 不要求接入真实退款、真实补偿、真实物流或真实订单中心。

### 16.1 V2.1 必须保持的能力

- V1 demo 继续可运行；
- RuleBasedAgentPlanner 继续可用；
- 默认测试不依赖真实 LLM；
- LLM API Key 不得进入代码仓库；
- ToolRegistry 仍然是唯一工具执行入口；
- 所有工具调用继续记录 trace；
- 高风险动作仍然进入人工确认边界。

### 16.2 V2 后续方向

V2 后续可以逐步扩展：

```text
Order Query Tools
MySQL Persistence
Approval APIs
Agent Evaluation Dataset
Vector or Hybrid Policy Retrieval
Docker Compose and Observability
```

但 V2.1 不同时扩大到这些方向。

## 17. V2.3 目标：Multi-Intent Planning

V2.3 的目标是支持复杂售后诉求拆分。

示例：

```text
我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？
```

系统应能把一个 Ticket 拆解为多个结构化子任务，例如：

```text
RETURN
EXCHANGE
COUPON_CONSULTATION
```

V2.3 计划支持：

- 一个 Ticket 对应多个 `AgentSubtask`；
- 子任务类型包括 `RETURN`、`EXCHANGE`、`COUPON_CONSULTATION`、`LOGISTICS_ISSUE`、`GENERAL_CONSULTATION`；
- 每个子任务有独立目标对象、用户原文片段、优先级、风险等级、政策检索 query、计划工具和依赖；
- Java 后端校验子任务类型、工具名、风险等级和依赖关系；
- ToolRegistry 继续作为唯一工具执行入口；
- ToolCallTrace 继续记录工具调用。

V2.3 仍然不做：

- 真实退款；
- 真实换货；
- 真实优惠券补偿；
- 完整优惠券系统；
- 真实物流或支付系统接入；
- 多 Agent 微服务；
- 并行执行或消息队列。

V2.3 已在模块化单体内实现结构化多意图规划、顺序子任务执行和 trace 可观测性。V2.4 已在此基础上引入
Specialist Handler 分发。Execution Tree、真实退款、真实换货、真实优惠券补偿、真实物流和真实支付仍属于后续阶段或非目标。

## 18. V2.4 目标：Specialist Agent Handler

V2.4 的目标是在 V2.3 `AgentSubtask` 基础上，引入 Specialist Agent Handler，用 Java 模块化单体内的策略类表达多 Agent 分工。

V2.4 已支持：

- 不同 `SubtaskType` 由不同 `SpecialistAgentHandler` 处理；
- `RETURN`、`EXCHANGE`、`COUPON_CONSULTATION`、`LOGISTICS_ISSUE` 可有不同处理策略；
- `GENERAL_CONSULTATION` 和 `HUMAN_ESCALATION` 可有兜底处理策略；
- `AgentApplicationService` 负责调度 handler，而不是直接写每类子任务处理细节；
- handler 仍然通过 ToolRegistry 调用工具；
- ToolCallTrace 继续记录所有工具调用。

V2.4 仍然不做：

- 真实退款；
- 真实换货；
- 真实优惠券补偿；
- 真实物流或支付系统接入；
- 多 Agent 微服务；
- 消息队列；
- 并行执行；
- 投票共识。

V2.4 当前已完成模块化单体内的 Specialist Handler 分发，不代表已实现多 Agent 微服务、真实退款、真实换货、
真实优惠券补偿、真实物流或真实支付。

## 19. V2.5 目标：Policy Retrieval Tool

V2.5 的目标是把售后政策检索收敛为受控工具能力，让 Specialist Handler 在执行工单备注、状态建议等动作前，
先通过 `ToolRegistry` 调用 `search_aftersale_policy` 获取结构化政策片段。

V2.5 已支持：

- `PolicySearchQuery` 表达检索输入；
- `PolicySnippet` 表达命中的政策片段；
- `PolicySearchResult` 表达结构化检索结果；
- `PolicyRepository` 暴露可替换的检索抽象；
- `InMemoryPolicyRepository` 提供默认离线关键词检索；
- `SearchAfterSalePolicyToolExecutor` 注册 LOW-risk `search_aftersale_policy` 工具；
- Specialist Handler 通过 `ToolRegistry` 检索政策，并在动作工具前执行政策检索；
- 无匹配政策时返回结构化空结果，不编造政策依据；
- `ToolCallTrace` 继续记录政策检索调用。

V2.5 仍然不做：

- 真实 VectorStore；
- PGvector；
- 外部 embedding 服务；
- 网络检索；
- 真实 LLM 依赖；
- 真实退款、换货、优惠券补偿、支付或物流动作。
