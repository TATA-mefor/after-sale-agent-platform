# EXEC_PLAN_V2.md

# AfterSale-Agent V2 执行计划

## 1. V2 总目标

AfterSale-Agent V1 已完成一个可本地演示的售后工单 Agent 后端闭环：

```text
创建售后工单
→ 触发规则型 AgentRun
→ 售后政策检索
→ 写入工单备注
→ 记录 ToolCallTrace
→ 查询执行轨迹
```

V2 的目标是在不破坏 V1 工程边界的前提下，将项目从“规则型单任务售后 Agent”升级为：

> 可接入真实 LLM、能基于订单事实和政策依据生成计划、支持复杂售后诉求拆解、具备多任务编排雏形的企业级 Agent 平台。

V2 不追求一次性做成完整生产系统，而是分阶段增强：

```text
V2.1 LLM Planner Adapter
V2.2 Order Query Tools
V2.3 Multi-Intent Planning
V2.4 Specialist Agent Handler
V2.5 Shared Workspace / Memory
V2.6 Approval APIs
V2.7 Execution Tree
V2.8 Evaluation Dataset
V2.9 Robustness
```

MySQL、Docker Compose、完整可观测性和部署能力暂时下沉到 V3。

---

## 2. V2 核心原则

1. LLM 只负责生成结构化计划，不直接执行业务动作；
2. Java 后端负责校验计划、执行工具、维护状态；
3. ToolRegistry 仍然是唯一工具执行入口；
4. 高风险动作仍然必须进入审批边界；
5. 默认测试不得依赖真实 LLM、API Key 或外部网络；
6. RuleBasedAgentPlanner 必须保留，保证本地演示和测试确定性；
7. FakeAgentPlanner 必须用于测试边界；
8. ToolCallTrace 必须持续记录工具调用；
9. 所有阶段必须通过 ArchUnit、Checkstyle、SpotBugs、JUnit 质量门禁；
10. 任何阶段变更范围前，必须先更新 Harness 文档。

---

## 3. V2.1：LLM Planner Adapter

### 3.1 目标

引入 Planner 抽象，使 Agent 的计划生成能力可由规则、Fake 或 LLM 实现。

目标链路：

```text
Ticket
→ AgentApplicationService
→ AgentPlanner
→ AgentPlan
→ ToolRegistry
→ ToolCallTrace
```

### 3.2 已完成能力

- `AgentPlanner` 抽象；
- `RuleBasedAgentPlanner`；
- `FakeAgentPlanner`；
- `LlmAgentPlanner` 边界；
- `agent.planner.mode=rule|fake|llm`；
- 默认 `rule` 模式；
- 测试不依赖真实 LLM；
- 缺少 API Key 时有清晰错误。

### 3.3 不做

- 不让 LLM 直接执行工具；
- 不让 LLM 直接修改 Ticket；
- 不接真实退款；
- 不接真实数据库；
- 不接向量库；
- 不做多 Agent。

### 3.4 状态

```text
COMPLETED
```

---

## 4. V2.1.1：Structured LLM Planner Client

### 4.1 目标

让 `LlmAgentPlanner` 在配置完整时可以真实调用 OpenAI-compatible LLM，并解析结构化 `AgentPlan`。

### 4.2 已完成能力

- `LlmClient`；
- `LlmRequest`；
- `LlmResponse`；
- `OpenAiLlmClient`；
- `AgentPlanParser`；
- `AgentPlanValidator`；
- `AgentPlannerPromptFactory`；
- 结构化 JSON 输出解析；
- 非法 intent / riskLevel / toolName / policyQuery 校验；
- 不安全表述校验，例如“已退款”“已补偿”。

### 4.3 不做

- 不让真实 LLM 成为默认测试依赖；
- 不提交 API Key；
- 不让 LLM 直接调用工具。

### 4.4 状态

```text
COMPLETED
```

---

## 5. V2.1.2：LLM Planner Live Smoke Test

### 5.1 目标

提供显式 opt-in 的 live smoke test，用于本地验证真实 LLM Planner 可调用。

### 5.2 已完成能力

- `LlmPlannerLiveSmokeTest`；
- 默认 `mvn test` 不运行 live test；
- 通过 `-Dlive.llm=true` 显式开启；
- 缺少 API Key 时不污染默认测试；
- live smoke 只验证 Planner，不执行工具、不写业务状态。

### 5.3 使用方式

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

需要本地环境变量：

```text
OPENAI_API_KEY
```

### 5.4 状态

```text
COMPLETED
```

---

## 6. V2.2：Order Query Tools

### 6.1 目标

让 Agent 的处理建议同时基于订单事实和售后政策。

### 6.2 已完成能力

- `get_order_by_id` 工具；
- `get_user_orders` 工具；
- 订单 demo 数据；
- `OrderApplicationService`；
- `OrderRepository` 内存实现；
- 订单工具通过 ToolRegistry 暴露；
- AgentRun 默认 trace 包含：

```text
get_order_by_id
search_aftersale_policy
add_ticket_note
```

### 6.3 当前链路

```text
Ticket
→ Planner
→ get_order_by_id
→ search_aftersale_policy
→ add_ticket_note
→ ToolCallTrace
→ 基于订单事实和政策依据的处理建议
```

### 6.4 不做

- 不接真实订单中心；
- 不接真实物流；
- 不接真实支付；
- 不修改订单核心数据；
- 不接真实数据库。

### 6.5 状态

```text
COMPLETED
```

---

## 7. V2.3：Multi-Intent Planning

### 7.1 目标

支持复杂售后问题拆解。

当前 AgentRun 主要处理单一售后诉求。V2.3 要支持用户一次表达多个售后意图，并将其拆成多个结构化子任务。

示例输入：

```text
我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？
```

期望拆解：

```text
子任务 1：退货处理
子任务 2：换货处理
子任务 3：优惠券咨询
```

V2.3 的目标不是实现复杂多 Agent 微服务，而是在当前 Java 模块化单体中建立多任务规划模型。

---

### 7.2 核心模型

V2.3 计划新增或扩展以下模型：

```text
AgentSubtask
SubtaskType
SubtaskStatus
SubtaskPlan
MultiIntentAgentPlan
```

#### 7.2.1 AgentSubtask

表示一个可执行的售后子任务。

字段建议：

```text
subtaskId
type
target
userMessageFragment
priority
riskLevel
policyQuery
plannedTools
dependencies
status
```

#### 7.2.2 SubtaskType

建议枚举：

```text
RETURN
EXCHANGE
REFUND_ONLY
REPAIR
LOGISTICS_ISSUE
COUPON_CONSULTATION
GENERAL_CONSULTATION
HUMAN_ESCALATION
UNKNOWN
```

#### 7.2.3 SubtaskStatus

建议枚举：

```text
PENDING
RUNNING
SUCCEEDED
FAILED
SKIPPED
WAITING_APPROVAL
```

#### 7.2.4 SubtaskPlan

表示单个子任务的执行计划。

至少包含：

```text
subtaskId
type
target
priority
riskLevel
policyQuery
plannedTools
dependencies
reason
```

#### 7.2.5 MultiIntentAgentPlan

表示复杂售后问题的整体计划。

至少包含：

```text
intent
riskLevel
subtasks
finalSuggestion
evidenceHints
```

---

### 7.3 示例结构化输出

```json
{
  "intent": "MULTI_INTENT",
  "riskLevel": "MEDIUM",
  "subtasks": [
    {
      "subtaskId": "subtask-1",
      "type": "RETURN",
      "target": "有污渍的衣服",
      "userMessageFragment": "其中一件有污渍要退货",
      "priority": 1,
      "riskLevel": "MEDIUM",
      "policyQuery": "质量问题 退货 污渍",
      "plannedTools": [
        {
          "toolName": "get_order_by_id",
          "reason": "查询订单事实"
        },
        {
          "toolName": "search_aftersale_policy",
          "reason": "检索质量问题退货政策"
        },
        {
          "toolName": "add_ticket_note",
          "reason": "记录退货子任务处理建议"
        }
      ],
      "dependencies": []
    },
    {
      "subtaskId": "subtask-2",
      "type": "EXCHANGE",
      "target": "需要换尺码的衣服",
      "userMessageFragment": "另一件要换尺码",
      "priority": 2,
      "riskLevel": "MEDIUM",
      "policyQuery": "换货 尺码不合适",
      "plannedTools": [
        {
          "toolName": "get_order_by_id",
          "reason": "查询订单事实"
        },
        {
          "toolName": "search_aftersale_policy",
          "reason": "检索尺码换货政策"
        },
        {
          "toolName": "add_ticket_note",
          "reason": "记录换货子任务处理建议"
        }
      ],
      "dependencies": []
    },
    {
      "subtaskId": "subtask-3",
      "type": "COUPON_CONSULTATION",
      "target": "未使用优惠券",
      "userMessageFragment": "还有一张优惠券没用上怎么退",
      "priority": 3,
      "riskLevel": "LOW",
      "policyQuery": "优惠券 未使用 退还",
      "plannedTools": [
        {
          "toolName": "search_aftersale_policy",
          "reason": "检索优惠券未使用规则"
        },
        {
          "toolName": "add_ticket_note",
          "reason": "记录优惠券咨询子任务处理建议"
        }
      ],
      "dependencies": []
    }
  ],
  "finalSuggestion": "该售后问题包含退货、换货和优惠券咨询三个子任务，建议分别处理并记录处理依据。",
  "evidenceHints": [
    "用户一次性提出多个售后诉求",
    "需要分别检索退货、换货、优惠券规则"
  ]
}
```

---

### 7.4 V2.3 范围

V2.3 只做：

- AgentPlan 支持 `subtasks`；
- LLM Planner Contract 支持多意图输出；
- RuleBasedAgentPlanner 支持简单多意图拆解；
- AgentApplicationService 顺序执行多个子任务；
- 每个子任务的工具调用进入 ToolCallTrace；
- 最终建议能汇总多个子任务结果；
- 测试覆盖多意图拆解和执行路径。

---

### 7.5 V2.3 不做

V2.3 不做：

- 不做多 Agent 微服务；
- 不做消息队列；
- 不做并行执行；
- 不做投票共识；
- 不做完整优惠券系统；
- 不做真实退款；
- 不做真实换货；
- 不做真实物流；
- 不接真实支付；
- 不引入前端；
- 不接 MySQL；
- 不改变高风险审批边界。

---

### 7.6 执行方式

V2.3 采用：

```text
Supervisor Planner 生成 MultiIntentAgentPlan
→ Java 后端校验 subtasks
→ AgentApplicationService 按 priority 顺序执行
→ 每个 subtask 通过 ToolRegistry 调用工具
→ ToolCallTrace 记录工具调用
→ 汇总生成最终建议
```

V2.3 不引入真正的 Specialist Handler。Specialist Handler 放到 V2.4。

---

### 7.7 验收标准

V2.3 完成时必须满足：

1. 能识别一个用户输入中的多个售后意图；
2. 能生成多个结构化子任务；
3. 每个子任务包含 type、target、priority、riskLevel、policyQuery、plannedTools；
4. Java 后端能校验子任务类型、风险等级、工具名和依赖关系；
5. 子任务按 priority 顺序执行；
6. 每个子任务的工具调用通过 ToolRegistry；
7. ToolCallTrace 能体现多子任务工具调用；
8. 最终建议能汇总多个子任务结果；
9. 默认测试不依赖真实 LLM；
10. V1/V2.1/V2.2 流程继续通过；
11. ArchUnit、Checkstyle、SpotBugs、JUnit 质量门禁继续通过。

---

### 7.8 测试要求

至少补充：

1. RuleBasedAgentPlanner 能将复杂售后问题拆成多个 subtasks；
2. AgentPlanParser 能解析包含 subtasks 的合法 JSON；
3. AgentPlanValidator 能拒绝未知 subtask type；
4. AgentPlanValidator 能拒绝未知 plannedTools；
5. AgentPlanValidator 能拒绝非法依赖，例如依赖不存在的 subtaskId；
6. AgentApplicationService 能顺序执行多个子任务；
7. AgentRun trace 中能看到多个子任务相关工具调用；
8. 多意图输入的最终建议包含多个子任务摘要；
9. 默认 `mvn test` 不依赖真实 LLM；
10. ArchitectureTest 继续通过。

---

### 7.9 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

---

### 7.10 状态

```text
COMPLETED
```

V2.3 已完成：

- `AgentPlan` 支持 `subtasks`；
- 新增 `AgentSubtask`、`SubtaskType`、`SubtaskStatus`；
- `AgentPlanParser` 可解析包含 subtasks 的结构化 JSON；
- `AgentPlanValidator` 校验子任务 ID、类型、风险等级、工具名、依赖关系、循环依赖和数量上限；
- `RuleBasedAgentPlanner` 可将退货、换货、优惠券咨询组合诉求拆成多个子任务；
- `AgentApplicationService` 可按 priority 顺序执行子任务；
- 每个子任务工具调用继续通过 ToolRegistry；
- ToolCallTrace 的 inputJson 包含 subtask metadata；
- 默认测试继续离线运行。

---

## 8. V2.4：Specialist Agent Handler

### 8.1 目标

在 V2.3 的多子任务计划基础上，引入轻量 Specialist Handler。

V2.4 不做多进程 Agent，而是在 Java 模块化单体中使用策略类表达专业 Agent 分工。

候选 Handler：

```text
ReturnAgentHandler
ExchangeAgentHandler
CouponAgentHandler
LogisticsAgentHandler
HumanEscalationHandler
```

### 8.2 不做

- 不拆微服务；
- 不引入消息队列；
- 不做复杂投票共识；
- 不做独立 Agent 服务部署。

### 8.3 状态

```text
PLANNED
```

---

## 9. V2.5：Shared Workspace / Memory

### 9.1 目标

建立结构化工作记忆，避免将完整对话历史无限塞入 LLM。

候选模型：

```text
AgentWorkspace
ResolvedEntity
SubtaskContext
OrderEvidence
PolicyEvidence
ToolResultMemory
```

### 9.2 目标

- 保存当前订单事实；
- 保存用户诉求拆解结果；
- 保存子任务状态；
- 保存政策依据；
- 保存工具结果摘要；
- 为后续多 Agent 协作提供共享上下文。

### 9.3 状态

```text
PLANNED
```

---

## 10. V2.6：Approval APIs

### 10.1 目标

把高风险人工确认边界落成 API。

候选 API：

```text
GET  /api/approval-requests/pending
POST /api/approval-requests/{id}/approve
POST /api/approval-requests/{id}/reject
```

### 10.2 触发场景

- `riskLevel = HIGH`；
- LLM plan validation failed；
- 工具连续失败；
- 用户问题涉及投诉、争议、强烈不满；
- 计划中包含退款、补偿、关闭争议等高风险动作。

### 10.3 状态

```text
PLANNED
```

---

## 11. V2.7：Execution Tree

### 11.1 目标

将 trace 从线性工具调用列表升级为可解释的执行树。

候选 API：

```text
GET /api/agent-runs/{runId}/execution-tree
```

示例结构：

```text
AgentRun
├── Subtask: RETURN
│   ├── ToolCall: get_order_by_id
│   ├── ToolCall: search_aftersale_policy
│   └── ToolCall: add_ticket_note
├── Subtask: EXCHANGE
│   ├── ToolCall: get_order_by_id
│   └── ToolCall: search_aftersale_policy
└── Subtask: COUPON_CONSULTATION
    └── ToolCall: search_aftersale_policy
```

### 11.2 状态

```text
PLANNED
```

---

## 12. V2.8：Evaluation Dataset

### 12.1 目标

建立最小 Agent 评测集。

评估指标：

```text
Intent Accuracy
Subtask Planning Accuracy
Tool Call Accuracy
Policy Match Accuracy
Risk Classification Accuracy
Plan Validity Rate
```

### 12.2 候选文件

```text
docs/evaluation/aftersale_cases.jsonl
docs/evaluation/EVALUATION.md
```

### 12.3 状态

```text
PLANNED
```

---

## 13. V2.9：Robustness

### 13.1 目标

补充基础容错能力。

候选能力：

```text
Tool timeout
Tool retry
Tool failure trace
AgentRun failed state
Fallback to human
Fallback to RuleBasedPlanner
```

### 13.2 状态

```text
PLANNED
```

---

## 14. V3 候选方向

V3 再考虑：

```text
MySQL Persistence
Docker Compose
Redis
Prometheus / Actuator Metrics
Structured Logging
Vector or Hybrid Retrieval
Deployment
```

V3 不在当前 V2.3 范围内。

---

## 15. V2 当前完成度

```text
V2.1 LLM Planner Adapter ✅
V2.1.1 Structured LLM Planner Client ✅
V2.1.2 LLM Live Smoke Test ✅
V2.2 Order Query Tools ✅
V2.3 Multi-Intent Planning ✅
V2.4 Specialist Agent Handler
V2.5 Shared Workspace / Memory
V2.6 Approval APIs
V2.7 Execution Tree
V2.8 Evaluation Dataset
V2.9 Robustness
```
