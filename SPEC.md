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
