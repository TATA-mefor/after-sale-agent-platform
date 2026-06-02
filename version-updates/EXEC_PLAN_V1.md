# EXEC_PLAN_V1.md

# AfterSale-Agent V1 执行计划

## 1. 执行目标

V1 的目标是从空仓库构建一个可本地运行、可接口演示、可测试验证的最小闭环：

> 用户提交售后问题 → 系统创建工单 → Agent 查询订单 → 检索售后政策 → 生成结构化处理计划 → 调用低风险工具 → 记录执行轨迹 → 返回处理建议。

V1 不追求完整商业系统，而是验证：

```text
Java 后端业务系统 + Agent 执行层 + Harness 工程约束
```

是可以组合成一个清晰、可信、可演示的企业级项目的。

## 2. V1 范围

### 2.1 必做

* Spring Boot 项目骨架；
* 基础包结构；
* 统一响应结构；
* 售后工单创建 API；
* 模拟订单查询；
* 售后政策检索；
* 售后意图识别；
* Agent 执行编排；
* AgentRun 记录；
* ToolCallTrace 记录；
* 工单详情查询；
* Agent 执行轨迹查询；
* 至少 5 个核心测试；
* 至少 3 条 ArchUnit 规则；
* Checkstyle / SpotBugs 接入；
* 基础文档齐全。

### 2.2 不做

* 真实退款；
* 真实物流 API；
* 真实支付系统；
* 完整前端后台；
* 复杂多 Agent 协作；
* 生产级权限系统；
* 大规模向量数据库；
* 真实客服 IM 接入。

## 3. 里程碑总览

```text
M0 Harness 骨架
M1 Spring Boot 项目初始化
M2 架构约束与质量门禁
M3 核心领域模型
M4 售后工单基础闭环
M5 Agent 工具系统
M6 售后政策检索
M7 Agent 编排与执行轨迹
M8 V1 演示接口
M9 测试、文档、Review Packet
```

## 4. M0：Harness 骨架

### 4.1 目标

建立仓库的约束系统，让后续开发有明确上下文。

### 4.2 产出

```text
SPEC.md
WORKFLOW.md
AGENTS.md
ARCHITECTURE.md
version-updates/
version-updates/
docs/decisions/
docs/quality/QUALITY_SCORE.md
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
docs/agent/EVALUATION.md
```

### 4.3 验收标准

* 文档说明项目目标、边界和 V1 范围；
* 文档说明任务执行流程；
* 文档说明架构分层和禁止依赖；
* 文档说明 Agent 风险边界；
* 后续智能体能从 AGENTS.md 找到上下文。

### 4.4 状态

```text
COMPLETED
```

## 5. M1：Spring Boot 项目初始化

### 5.1 目标

创建可运行的 Java 项目骨架。

### 5.2 任务

```text
1. 创建 Maven 项目
2. 设置 Java 17+
3. 引入 Spring Boot 3.x
4. 创建主启动类
5. 创建基础包结构
6. 添加 health check API
7. 添加 README 启动说明
```

### 5.3 预期文件

```text
pom.xml
src/main/java/io/github/tatame/aftersale/AfterSaleAgentApplication.java
src/main/java/io/github/tatame/aftersale/common/
src/main/java/io/github/tatame/aftersale/order/
src/main/java/io/github/tatame/aftersale/ticket/
src/main/java/io/github/tatame/aftersale/policy/
src/main/java/io/github/tatame/aftersale/agent/
src/main/java/io/github/tatame/aftersale/tool/
src/main/java/io/github/tatame/aftersale/trace/
src/main/java/io/github/tatame/aftersale/approval/
src/test/java/io/github/tatame/aftersale/
```

### 5.4 验证命令

```bash
mvn test
mvn spring-boot:run
```

### 5.5 验收标准

* 项目可以编译；
* 测试命令通过；
* 应用可以本地启动；
* 包结构符合 ARCHITECTURE.md。

### 5.6 状态

```text
COMPLETED
```

## 6. M2：架构约束与质量门禁

### 6.1 目标

在代码大量生成前先建立机械化约束。

### 6.2 任务

```text
1. 接入 JUnit 5
2. 接入 ArchUnit
3. 接入 Checkstyle
4. 接入 SpotBugs
5. 创建 ArchitectureTest
6. 编写至少 3 条架构规则
7. 配置 Maven 验证命令
```

### 6.3 ArchUnit 初始规则

```text
Rule 1: api 层不得访问 repository
Rule 2: domain 层不得依赖 Spring Web
Rule 3: agent 模块不得直接访问业务 repository
Rule 4: order/ticket/policy 不得依赖 agent
```

### 6.4 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### 6.5 验收标准

* 架构测试存在；
* 至少 3 条规则可运行；
* 质量检查命令可执行；
* 失败信息能指导修复。

### 6.6 状态

```text
COMPLETED
```

## 7. M3：核心领域模型

### 7.1 目标

建立 V1 所需领域对象。

### 7.2 任务

创建以下模型：

```text
Order
Ticket
AfterSalePolicy
AgentRun
ToolCallTrace
ApprovalRequest
```

创建以下枚举：

```text
OrderStatus
TicketStatus
IntentType
AgentRunStatus
ToolCallStatus
ToolRiskLevel
ApprovalStatus
```

### 7.3 设计原则

* 领域模型不依赖 Web；
* 状态流转必须封装方法；
* 不允许外部随意 set 状态；
* 关键字段必须有业务语义；
* 时间字段统一处理。

### 7.4 测试

至少覆盖：

```text
Ticket 创建默认状态
Ticket 状态流转
AgentRun 成功/失败状态
ToolCallTrace 记录成功/失败
高风险工具需要审批判断
```

### 7.5 验收标准

* 核心模型可编译；
* 领域测试通过；
* 状态流转不散落在 Controller；
* domain 层无 Spring Web 依赖。

### 7.6 状态

```text
COMPLETED
```

## 8. M4：售后工单基础闭环

### 8.1 目标

实现用户提交售后问题后创建工单。

### 8.2 API

```http
POST /api/tickets
GET /api/tickets/{ticketId}
```

### 8.3 任务

```text
1. 创建 TicketCreateRequest
2. 创建 TicketResponse
3. 创建 TicketController
4. 创建 TicketApplicationService
5. 创建 TicketRepository 接口
6. V1 使用内存或 H2 实现 TicketRepository
7. 添加工单创建测试
8. 添加工单查询测试
```

### 8.4 验收标准

* 可以创建售后工单；
* 可以查询工单详情；
* 初始状态为 CREATED；
* Controller 不包含业务逻辑；
* api 层不直接访问 repository。

### 8.5 状态

```text
COMPLETED
```

## 9. M5：Agent 工具系统

### 9.1 目标

实现 Agent 调用业务能力的统一工具层。

### 9.2 初始工具

```text
get_order_by_id
get_user_orders
search_aftersale_policy
create_aftersale_ticket
update_ticket_status
add_ticket_note
```

### 9.3 任务

```text
1. 定义 ToolDefinition
2. 定义 ToolInput / ToolOutput
3. 定义 ToolExecutor
4. 定义 ToolRegistry
5. 为每个工具声明 riskLevel
6. 低风险工具直接执行
7. 高风险工具返回 REQUIRES_APPROVAL
```

### 9.4 设计原则

* Agent 只能通过 ToolRegistry 找工具；
* 工具内部调用 application service；
* 工具调用必须产生 trace；
* 工具失败不得被吞掉；
* 工具输入输出必须结构化。

### 9.5 测试

至少覆盖：

```text
能按名称找到工具
未知工具返回失败
低风险工具可执行
高风险工具进入审批
工具调用失败有错误信息
```

### 9.6 状态

```text
COMPLETED
```

## 10. M6：售后政策检索

### 10.1 目标

实现 V1 简单政策检索，为 Agent 提供依据。

### 10.2 任务

```text
1. 创建 AfterSalePolicy 模型
2. 创建 PolicyRepository
3. 初始化几条政策数据
4. 实现关键词检索
5. 创建 search_aftersale_policy 工具
6. 返回政策片段和 policyId
```

### 10.3 V1 政策样例

至少包含：

```text
7 天无理由退货规则
质量问题退换货规则
已签收未收到物流争议规则
换货规则
维修规则
特殊商品不支持退货规则
```

### 10.4 验收标准

* 能根据用户问题检索政策；
* 检索结果包含 policyId；
* Agent 建议能引用政策依据；
* 检索失败时有明确错误。

### 10.5 状态

```text
COMPLETED
```

## 11. M7：Agent 编排与执行轨迹

### 11.1 目标

实现 AgentRun 主流程。

### 11.2 API

```http
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}/traces
```

### 11.3 Agent V1 流程

```text
1. 读取 Ticket
2. 识别 intentType
3. 创建 AgentRun
4. 查询订单
5. 检索政策
6. 生成结构化 plan
7. 判断风险等级
8. 调用允许的工具
9. 记录 ToolCallTrace
10. 写入 Agent 建议
11. 更新 Ticket 状态
12. 返回 AgentRun 结果
```

### 11.4 结构化 Plan 示例

```json
{
  "intent": "RETURN_AND_REFUND",
  "riskLevel": "MEDIUM",
  "steps": [
    {
      "step": 1,
      "action": "get_order_by_id",
      "reason": "确认订单是否存在以及是否在售后期"
    },
    {
      "step": 2,
      "action": "search_aftersale_policy",
      "reason": "检索质量问题退货规则"
    },
    {
      "step": 3,
      "action": "add_ticket_note",
      "reason": "保存 Agent 处理建议"
    }
  ],
  "finalSuggestion": "订单仍在售后期内，若用户能提供质量问题凭证，建议进入退货退款审核。"
}
```

### 11.5 验收标准

* AgentRun 可创建；
* Agent 能识别至少 5 类意图；
* Agent 能调用订单工具和政策工具；
* 每次工具调用都有 trace；
* 最终建议包含依据；
* 失败时 AgentRun 状态为 FAILED。

### 11.6 状态

```text
COMPLETED
```

### 11.7 V1 实际收口说明

最终 V1 AgentRun 已实现可演示的政策检索、低风险工具调用、处理建议写入和 trace 查询。订单上下文在
V1 中通过工单 `orderId` 保留，未新增真实订单查询工具；`get_order_by_id` 和 `get_user_orders` 进入 V2 候选。

## 12. M8：V1 演示闭环

### 12.1 目标

提供一条完整可演示路径。

### 12.2 演示脚本

#### Step 1：创建工单

```http
POST /api/tickets
```

请求：

```json
{
  "userId": "U1001",
  "orderId": "O202605130001",
  "message": "我买的耳机用了两天左耳没声音了，想退货。"
}
```

#### Step 2：触发 Agent

```http
POST /api/tickets/{ticketId}/agent-runs
```

#### Step 3：查询工单

```http
GET /api/tickets/{ticketId}
```

#### Step 4：查询执行轨迹

```http
GET /api/agent-runs/{runId}/traces
```

### 12.3 期望演示效果

* 工单状态从 CREATED 变化为 AGENT_RUNNING，再变为 WAITING_HUMAN_APPROVAL 或 RESOLVED；
* Agent 输出 intent、plan、finalSuggestion；
* trace 中能看到 search_aftersale_policy、add_ticket_note；
* 最终建议引用政策依据。

### 12.4 状态

```text
COMPLETED
```

### 12.5 V1 实际收口说明

最终 V1 Demo trace 包含 `search_aftersale_policy` 和 `add_ticket_note`。订单查询工具未进入最终 V1，
不影响当前 API-only demo 的政策依据、工具边界和 trace 展示。

## 13. M9：测试、文档、Review Packet

### 13.1 目标

完成 V1 收口。

### 13.2 必备测试

至少包含：

```text
TicketCreateTest
TicketStatusTransitionTest
PolicySearchTest
ToolRegistryTest
AgentRunFlowTest
ArchitectureTest
```

### 13.3 文档更新

必须更新：

```text
README.md
SPEC.md
WORKFLOW.md
AGENTS.md
ARCHITECTURE.md
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
docs/quality/QUALITY_SCORE.md
version-updates/EXEC_PLAN_V1_REVIEW.md
```

其中 SPEC.md 和 WORKFLOW.md 作为 V1 目标与流程基线检查；若未发现与当前收口冲突的内容，不做无意义改写。

### 13.4 Review Packet

V1 完成时必须输出：

```text
## Review Packet: V1

### What Was Built

### Core Demo Flow

### Architecture Boundaries

### Agent Capabilities

### Validation Results

### Known Limitations

### V2 Candidates

### Completion Signal
TASK_COMPLETE
```

### 13.5 状态

```text
COMPLETED
```

## 14. 风险清单

### 14.1 技术风险

| 风险          | 应对                |
| ----------- | ----------------- |
| Agent 输出不稳定 | V1 使用结构化输出和规则兜底   |
| RAG 检索质量弱   | V1 简单检索，V2 升级向量检索 |
| 工具调用失败      | 必须记录 trace 和错误    |
| 架构漂移        | ArchUnit 强制约束     |
| 项目范围膨胀      | 严格遵守 V1 不做项       |

### 14.2 面试风险

| 风险           | 应对                        |
| ------------ | ------------------------- |
| 被认为只是 AI 客服  | 强调工单闭环、工具调用、审批、trace      |
| 被问为什么用 Agent | 说明售后问题需要规划、检索、工具执行和风险判断   |
| 被问怎么防误操作     | 说明工具风险分级和人工确认             |
| 被问 RAG 准确性   | 说明引用依据和评测计划               |
| 被问工程价值       | 说明 Harness 文档、架构约束和 CI 背压 |

## 15. V1 完成定义

V1 只有在以下条件全部满足时才算完成：

```text
1. 应用可本地启动
2. mvn test 通过
3. 至少 5 个核心测试通过
4. 至少 3 条 ArchUnit 规则生效
5. 售后工单 API 可用
6. AgentRun API 可用
7. Trace 查询 API 可用
8. Agent 能完成订单查询 + 政策检索 + 处理建议
9. 高风险动作不会被直接执行
10. README 有完整演示步骤
11. Review Packet 已生成
12. 输出 TASK_COMPLETE
```

## 16. V2 候选方向

V1 完成后，V2 可选择：

```text
1. 接入真实 MySQL + Redis
2. 接入向量数据库
3. 引入人工确认后台
4. 增加退款 / 换货 / 维修状态机
5. 增加 Agent 评测数据集
6. 增加多 Agent 协作
7. 接入 MCP 工具协议
8. 增加 Prometheus / Grafana 可观测性
9. 增加 Docker Compose 一键启动
10. 增加简历级演示视频脚本
```

## 17. 当前下一步

当前状态：

```text
M9：测试、文档、Review Packet 已完成，V1 进入完成审查态
```

V1 收口产物：

```text
docs/quality/QUALITY_SCORE.md
version-updates/EXEC_PLAN_V1_REVIEW.md
README.md Demo Walkthrough
最终 Review Packet
```
