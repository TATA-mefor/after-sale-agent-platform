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
V2.5 Policy Retrieval Tool
V2.6 Agent Workspace / Structured Memory
V2.7 Approval APIs
V2.8 Execution Tree
V2.9 Evaluation Dataset
V2.10 Robustness
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

目标是把当前 `AgentApplicationService` 中统一处理所有 `AgentSubtask` 的逻辑，拆成按 `SubtaskType` 分发的专业处理器：

```text
AgentSubtask
→ SpecialistAgentHandlerRegistry
→ SpecialistAgentHandler
→ ToolRegistry
→ ToolCallTrace
```

V2.4 的价值不是增加进程数量，而是让 RETURN、EXCHANGE、COUPON_CONSULTATION、LOGISTICS_ISSUE 等子任务拥有清晰、
可测试、可替换的处理策略。

### 8.2 范围

V2.4 只做：

- 定义 `SpecialistAgentHandler` 接口；
- 定义 `SpecialistAgentHandlerRegistry`；
- 定义 `SubtaskExecutionContext`；
- 定义 `SubtaskExecutionResult`；
- 让 `AgentApplicationService` 通过 registry 查找 handler；
- 每个 handler 只声明并处理自己支持的 `SubtaskType`；
- handler 内部仍通过 ToolRegistry 调用工具；
- ToolCallTrace 继续记录每个工具调用；
- 保持单进程顺序执行；
- 补充 handler 分发、覆盖、工具调用和 trace 测试。

### 8.3 不做

V2.4 不做：

- 不做多 Agent 微服务；
- 不引入消息队列；
- 不做并行执行；
- 不做投票共识；
- 不做独立 Agent 服务部署；
- 不做真实退款；
- 不做真实换货；
- 不做真实优惠券补偿；
- 不接真实物流；
- 不接真实支付；
- 不让 handler 直接访问 Repository；
- 不让 handler 绕过 ToolRegistry；
- 不让 LLM / Planner 直接执行 handler。

### 8.4 核心模型 / 接口

#### 8.4.1 SpecialistAgentHandler

建议接口：

```java
public interface SpecialistAgentHandler {
    boolean supports(SubtaskType type);

    SubtaskExecutionResult handle(SubtaskExecutionContext context);
}
```

职责：

- 只处理自己支持的 `SubtaskType`；
- 基于 `AgentSubtask`、Ticket、AgentRun、工具结果和已有 evidence 生成子任务处理结果；
- 通过 ToolRegistry 执行工具；
- 不直接访问 Repository；
- 不执行真实高风险动作。

#### 8.4.2 SpecialistAgentHandlerRegistry

职责：

- 持有所有 `SpecialistAgentHandler`；
- 根据 `SubtaskType` 返回唯一 handler；
- 对未覆盖类型返回清晰错误或 human escalation handler；
- 拒绝重复 handler 覆盖同一类型；
- 为测试暴露已支持的 `SubtaskType` 集合。

#### 8.4.3 SubtaskExecutionContext

建议字段：

```text
runId
ticket
agentPlan
subtask
availableTools
riskPolicySummary
previousResults
```

说明：

- context 是 handler 的输入；
- 不直接暴露 Repository；
- 不暴露 LLM SDK；
- 不包含 API Key 或敏感基础设施配置。

#### 8.4.4 SubtaskExecutionResult

建议字段：

```text
subtaskId
type
status
summary
evidence
toolCalls
errorMessage
requiresHumanApproval
```

说明：

- result 是 handler 的结构化输出；
- AgentApplicationService 使用 result 汇总最终建议；
- 失败必须可见，不能隐藏工具调用失败；
- 高风险动作只能表达 `requiresHumanApproval`，不能表达已执行完成。

### 8.5 候选 Handler

候选 Handler：

```text
ReturnAgentHandler
ExchangeAgentHandler
CouponAgentHandler
LogisticsAgentHandler
GeneralConsultationHandler
HumanEscalationHandler
```

建议支持关系：

```text
RETURN → ReturnAgentHandler
EXCHANGE → ExchangeAgentHandler
COUPON_CONSULTATION → CouponAgentHandler
LOGISTICS_ISSUE → LogisticsAgentHandler
GENERAL_CONSULTATION → GeneralConsultationHandler
HUMAN_ESCALATION / UNKNOWN → HumanEscalationHandler
```

### 8.6 执行链路

```text
Ticket
→ AgentPlanner
→ AgentPlan with subtasks
→ AgentPlanValidator
→ AgentApplicationService
→ sort subtasks by priority / dependencies
→ SpecialistAgentHandlerRegistry
→ SpecialistAgentHandler
→ ToolRegistry
→ ToolCallTrace
→ SubtaskExecutionResult
→ final AgentRun summary
```

约束：

- `AgentApplicationService` 负责调度，不直接写每类子任务处理细节；
- handler 不得直接调用 LLM；
- handler 不得访问 Repository；
- handler 不得绕过 ToolRegistry；
- handler 不得执行真实退款、换货、优惠券补偿、支付或物流动作；
- ToolCallTrace 继续记录所有工具调用。

### 8.7 验收标准

V2.4 完成时必须满足：

1. 存在 `SpecialistAgentHandler` 接口；
2. 存在 `SpecialistAgentHandlerRegistry`；
3. 存在 `SubtaskExecutionContext`；
4. 存在 `SubtaskExecutionResult`；
5. `AgentApplicationService` 通过 registry 调度 handler；
6. RETURN / EXCHANGE / COUPON_CONSULTATION / LOGISTICS_ISSUE 至少有明确 handler 或 fallback 策略；
7. handler 只能处理自己支持的 `SubtaskType`；
8. handler 内部工具调用全部通过 ToolRegistry；
9. ToolCallTrace 继续记录 handler 内部工具调用；
10. handler 不访问 Repository；
11. handler 不执行真实高风险动作；
12. V1/V2.2 单意图流程不退化；
13. V2.3 多意图流程不退化；
14. 默认测试不依赖真实 LLM；
15. ArchUnit、Checkstyle、SpotBugs、JUnit 质量门禁继续通过。

### 8.8 测试要求

至少补充：

1. registry 能按 `SubtaskType` 找到正确 handler；
2. registry 拒绝重复 handler 覆盖同一类型；
3. 未覆盖类型有清晰 fallback 或错误；
4. ReturnAgentHandler 只处理 RETURN；
5. ExchangeAgentHandler 只处理 EXCHANGE；
6. CouponAgentHandler 只处理 COUPON_CONSULTATION；
7. LogisticsAgentHandler 只处理 LOGISTICS_ISSUE；
8. handler 工具调用均通过 ToolRegistry；
9. handler 不访问 Repository 的 ArchUnit 规则；
10. handler 结果进入最终 AgentRun summary；
11. ToolCallTrace 能看到 handler 执行产生的工具调用；
12. V2.3 多意图顺序执行继续通过；
13. 默认 `mvn test` 不依赖真实 LLM。

### 8.9 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### 8.10 状态

```text
COMPLETED
```

V2.4 已完成：

- 新增 `SpecialistAgentHandler`、`SpecialistAgentHandlerRegistry`、`SubtaskExecutionContext` 和
  `SubtaskExecutionResult`；
- 新增 `ReturnAgentHandler`、`ExchangeAgentHandler`、`CouponAgentHandler`、`LogisticsAgentHandler`、
  `GeneralConsultationHandler` 和 `HumanEscalationHandler`；
- `AgentApplicationService` 已通过 registry 调度多意图 subtasks；
- handler 内部工具调用仍统一通过 `ToolRegistry`；
- `ToolCallTrace` 继续记录 handler 内部工具调用，trace inputJson 保留 subtask metadata；
- registry 对重复 handler coverage 直接拒绝，对未覆盖 subtask type 返回结构化失败结果；
- ArchUnit 已约束 handler 不得依赖 Repository、LLM infrastructure 或 Spring Web；
- 默认测试仍离线运行，不依赖真实 LLM、API Key 或外部网络。

---

## 9. V2.5：Policy Retrieval Tool

### 9.1 目标

将售后政策检索收敛为受控 `search_aftersale_policy` 工具，使 Specialist Handler 在执行动作工具前通过
`ToolRegistry` 获取结构化政策片段。

核心模型：

```text
PolicySearchQuery
PolicySnippet
PolicySearchResult
PolicyRepository
InMemoryPolicyRepository
SearchAfterSalePolicyToolExecutor
```

### 9.2 已完成能力

- `PolicySearchQuery` 表达受控检索输入；
- `PolicySnippet` 表达政策片段；
- `PolicySearchResult` 表达命中结果或结构化空结果；
- `PolicyRepository` 暴露可替换检索抽象；
- `InMemoryPolicyRepository` 使用本地关键词匹配，默认离线运行；
- `SearchAfterSalePolicyToolExecutor` 注册 LOW-risk `search_aftersale_policy`；
- `ToolRegistry` 可以执行政策检索并产生 ToolCallTrace；
- Specialist Handler 会在 `add_ticket_note` 等动作工具前执行政策检索；
- 不支持的 query 返回空 results 和清晰 message，不编造政策依据。

### 9.3 状态

```text
COMPLETED
```

---

## 10. V2.6：Agent Workspace / Structured Memory

### 10.1 目标

建立单次 `AgentRun` 内的结构化工作记忆，用于统一承载订单事实、政策依据、子任务结果、工具结果摘要和风险标记。

当前执行信息分散在 `AgentPlan`、`ToolCallTrace`、Ticket note、subtask metadata 和 final summary 中。V2.6 已引入
`AgentWorkspace`，让 handler 可以读取前序上下文、写入结构化结果，并让 final summary 基于 workspace 汇总。

V2.6 不改变 ToolRegistry 和 ToolCallTrace 的边界：

- ToolRegistry 仍然是唯一工具执行入口；
- ToolCallTrace 仍然是审计记录；
- AgentWorkspace 是当前 `AgentRun` 内的结构化工作记忆。

### 10.2 范围

V2.6 已完成：

- 定义单次 `AgentRun` 内的 `AgentWorkspace`；
- 定义订单事实、政策依据、子任务记忆、工具结果摘要和风险标记模型；
- `AgentRun` 创建时初始化 workspace；
- handler 从 workspace 读取上下文；
- handler 执行工具后写入 workspace；
- final summary 基于 workspace 汇总；
- workspace 和 trace 保持边界清晰；
- 默认测试保持离线、确定性。

### 10.3 不做

V2.6 不做：

- 不做长期记忆；
- 不做跨会话记忆；
- 不做用户画像；
- 不做向量记忆；
- 不接 Redis；
- 不接 MySQL；
- 不接向量库或 PGvector；
- 不保存 API Key、敏感凭证、完整长 prompt 或 LLM 原始长文本；
- 不让 workspace 替代 ToolCallTrace；
- 不让 workspace 绕过 ToolRegistry；
- 不让 workspace 直接访问 Repository。

### 10.4 核心模型

候选模型：

```text
AgentWorkspace
OrderFact
PolicyEvidence
SubtaskMemory
ToolResultSummary
RiskFlag
```

#### 10.4.1 AgentWorkspace

单次 `AgentRun` 的结构化工作区。

建议字段：

```text
runId
ticketId
orderFacts
policyEvidence
subtaskMemories
toolResultSummaries
riskFlags
createdAt
updatedAt
```

#### 10.4.2 OrderFact

来自 `get_order_by_id` / `get_user_orders` 的订单事实摘要。

建议字段：

```text
orderId
productName
orderStatus
paidAmount
deliveredAt
aftersaleDeadline
whetherInAftersaleWindow
sourceToolName
```

#### 10.4.3 PolicyEvidence

来自 `search_aftersale_policy` 的政策依据。

建议字段：

```text
policyId
category
productType
matchedText
matchReason
query
sourceToolName
```

#### 10.4.4 SubtaskMemory

单个 `AgentSubtask` 的执行记忆。

建议字段：

```text
subtaskId
type
target
status
summary
evidenceRefs
toolResultRefs
riskFlagRefs
```

#### 10.4.5 ToolResultSummary

工具调用结果摘要，不替代完整 ToolCallTrace。

建议字段：

```text
toolName
status
summary
traceId optional
subtaskId optional
requiresHumanApproval
```

#### 10.4.6 RiskFlag

执行过程中发现的风险标记。

建议字段：

```text
riskLevel
reason
source
subtaskId optional
requiresHumanApproval
```

### 10.5 工作流

```text
AgentRun 创建 workspace
→ AgentApplicationService 将 workspace 放入执行上下文
→ Handler 从 workspace 读取订单事实、政策依据、前序子任务结果和风险标记
→ Handler 通过 ToolRegistry 执行工具
→ Handler 将工具结果摘要、政策依据、订单事实、子任务结果和风险标记写入 workspace
→ ToolRegistry / ToolCallTrace 继续记录完整工具调用审计
→ final summary 基于 workspace 汇总
```

### 10.6 验收标准

V2.6 完成时必须满足：

1. 存在 `AgentWorkspace` 结构；
2. 存在 `OrderFact`、`PolicyEvidence`、`SubtaskMemory`、`ToolResultSummary`、`RiskFlag`；
3. `AgentRun` 创建时初始化 workspace；
4. Handler 可读取 workspace；
5. Handler 执行工具后写入 workspace；
6. final summary 基于 workspace 汇总；
7. ToolCallTrace 继续记录完整工具调用；
8. workspace 不替代 ToolCallTrace；
9. workspace 不绕过 ToolRegistry；
10. workspace 不直接访问 Repository；
11. workspace 不保存 API Key、敏感凭证、完整长 prompt 或 LLM 原始长文本；
12. 默认测试不依赖真实 LLM、API Key、Redis、MySQL、向量库或网络。

### 10.7 测试要求

至少补充：

1. AgentRun 创建时创建 workspace；
2. order tool 结果写入 `OrderFact`；
3. policy tool 结果写入 `PolicyEvidence`；
4. handler 执行完成后写入 `SubtaskMemory`；
5. 工具成功、失败、审批要求写入 `ToolResultSummary`；
6. HIGH risk 或人工确认场景写入 `RiskFlag`；
7. final summary 使用 workspace 内容；
8. ToolCallTrace 仍然产生且不被 workspace 替代；
9. workspace 不包含 API Key、敏感凭证、完整长 prompt；
10. 默认测试离线运行。

### 10.8 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

### 10.9 状态

```text
COMPLETED
```

V2.6 已完成：

- 新增 `AgentWorkspace`、`OrderFact`、`PolicyEvidence`、`SubtaskMemory`、`ToolResultSummary` 和 `RiskFlag`；
- `AgentRun` 开始时创建单次运行内的 in-memory workspace；
- `SubtaskExecutionContext` 携带 workspace 给 Specialist Handler；
- 单意图直接执行路径和多意图 handler 路径都会写入 workspace；
- `get_order_by_id` 结果写入 `OrderFact`；
- `search_aftersale_policy` 结果写入 `PolicyEvidence`；
- `add_ticket_note` 等工具执行结果写入 `ToolResultSummary`；
- 多子任务执行结果写入 `SubtaskMemory`；
- 高风险子任务写入 `RiskFlag` 并等待人工确认；
- final summary 基于 workspace 汇总订单事实、政策依据和子任务记忆；
- ToolRegistry 仍然是唯一工具执行入口；
- ToolCallTrace 仍然是审计记录，未被 workspace 替代。

---

## 11. V2.7：Approval APIs

### 11.1 目标

把高风险人工确认边界落成 API。

已实现 API：

```text
GET  /api/approval-requests/pending
GET  /api/approval-requests/{id}
POST /api/approval-requests/{id}/approve
POST /api/approval-requests/{id}/reject
```

### 11.2 已完成能力

- `ApprovalApplicationService`；
- `ApprovalRepository`；
- `InMemoryApprovalRepository`；
- `ApprovalController`；
- `ApprovalRequestResponse`；
- `ApprovalApproveRequest`；
- `ApprovalRejectRequest`；
- pending approval 查询；
- 单个 approval request 查询；
- approve / reject 状态流转；
- 重复审批返回清晰冲突错误；
- 审批结果写回 Ticket note；
- high-risk subtask 自动创建 `ApprovalRequest` 并让 Ticket 进入 `WAITING_HUMAN_APPROVAL`；
- 低风险动作不会创建审批请求；
- 审批流不执行真实退款、真实换货或真实优惠券补偿。

### 11.3 触发场景

- `riskLevel = HIGH`；
- LLM plan validation failed；
- 工具连续失败；
- 用户问题涉及投诉、争议、强烈不满；
- 计划中包含退款、补偿、关闭争议等高风险动作。

当前 V2.7 至少覆盖 high-risk subtask 触发审批请求。其他触发场景可在后续 robustness / evaluation 阶段扩展。

### 11.4 状态

```text
COMPLETED
```

---

## 12. V2.8：Execution Tree

### 12.1 目标

将 trace 从线性工具调用列表升级为可解释的只读执行树。

已实现 API：

```text
GET /api/agent-runs/{runId}/execution-tree
```

Execution Tree 聚合：

- `AgentRun` 根信息；
- `AgentPlan` / `AgentSubtask`；
- `ToolCallTrace`；
- `ApprovalRequest`；
- `AgentWorkspace` snapshot 汇总。

返回结构包含：

```text
runId
ticketId
agentRunStatus
finalSuggestion
rootSummary
subtasks
toolCalls
approvalRequests
errors
createdAt / finishedAt
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

### 12.2 已完成能力

- 新增 `ExecutionTreeApplicationService`；
- 新增 `AgentExecutionTreeController`；
- 新增 `ExecutionTreeResponse`、subtask node、tool call node 和 approval node；
- 单意图 AgentRun 的无 subtask tool calls 归入 root-level `toolCalls`；
- 多意图 AgentRun 的 tool calls 基于 trace `inputJson.subtaskId` 归属到对应 subtask；
- high-risk subtask 创建的 `ApprovalRequest` 基于 `runId` / `subtaskId` 归属到对应 subtask；
- 无法精确归属的 approval request 保留在 root-level `approvalRequests`；
- missing `runId` 返回清晰 `AGENT_RUN_NOT_FOUND`；
- API 为只读查询，不修改 Ticket、AgentRun、ToolCallTrace 或 ApprovalRequest；
- 默认测试仍离线运行，不依赖真实 LLM、API Key、数据库、Redis 或网络。

### 12.3 不做

V2.8 不做：

- 不改 Agent 主执行流程；
- 不改 ToolCallTrace 模型；
- 不接真实数据库；
- 不接 Redis；
- 不做前端可视化 UI；
- 不做并行执行；
- 不做消息队列；
- 不执行真实退款、换货、优惠券补偿、支付或物流动作。

### 12.4 状态

```text
COMPLETED
```

---

## 13. V2.9：Evaluation Dataset

### 13.1 目标

建立离线、确定性的 Agent 评测集，用于评估当前售后 Agent 的规划、工具调用、风险判断和执行结果边界。

V2.9 默认使用 `RuleBasedAgentPlanner`，不调用真实 LLM，不需要 API Key，不访问外部网络，不接数据库、Redis 或向量库。

当前评测链路：

```text
docs/evaluation/aftersale_cases.jsonl
→ EvaluationApplicationService
→ RuleBasedAgentPlanner
→ AgentPlanValidator
→ controlled policy search through ToolRegistry
→ EvaluationReport
```

评估指标包括：

```text
totalCases
passedCases
failedCases
intentAccuracy
subtaskTypeAccuracy
toolCallAccuracy
riskLevelAccuracy
policyMatchAccuracy
approvalRequirementAccuracy
planValidityRate
```

### 13.2 已完成能力

- 新增 `docs/evaluation/aftersale_cases.jsonl`，包含 15 条售后评测 case；
- 新增 `docs/evaluation/EVALUATION.md`；
- 新增 `EvaluationCase`、`EvaluationExpected`、`EvaluationResult`、`EvaluationReport`、`EvaluationMetric` 和
  `EvaluationFailure`；
- 新增 `EvaluationApplicationService`；
- 每条 case 生成 `AgentPlan` 后必须通过 `AgentPlanValidator`；
- `expectedTools` 校验 planned tools；
- `expectedSubtaskTypes` 校验多意图 subtask 拆解；
- `expectedPolicyCategories` 通过受控 `search_aftersale_policy` 工具离线校验；
- `expectedRequiresApproval` 校验 HIGH-risk / approval requirement 边界；
- 失败结果包含 `caseId` 和失败字段；
- 默认测试仍离线运行，不依赖真实 LLM、API Key、数据库、Redis、向量库或网络。

### 13.3 不做

V2.9 不做：

- 不把评测写成 LLM-as-judge；
- 不让默认评测调用真实 OpenAI provider；
- 不执行完整 AgentRun 状态流转；
- 不修改 Ticket、AgentRun、ToolCallTrace 或 ApprovalRequest；
- 不新增前端；
- 不引入复杂外部评测框架。

### 13.4 状态

```text
COMPLETED
```

---

## 14. V2.10：Robustness

### 14.1 目标

基于 V2.9 evaluation dataset 暴露的问题，增强 deterministic rule-based fallback 的意图识别、子任务拆解和
审批风险判断。V2.10 不改变 LLM Planner、ToolRegistry、Approval、Trace、Workspace 或外部基础设施边界。

重点改进：

```text
refund-only recognition
coupon-only / coupon consultation recognition
two-intent combination splitting
high-risk keyword detection
expectedRequiresApproval alignment
policy keyword priority for special goods and repair
```

### 14.2 已完成能力

- `RuleBasedAgentPlanner` 扩展仅退款、未发货取消退款、不退货退款等 refund-only 表达；
- `RuleBasedAgentPlanner` 扩展优惠券、退券、补券、优惠没退等 coupon consultation 表达；
- 支持退货 + 换货、退货 + 优惠券、物流 + 退款咨询等两意图拆解；
- 多意图计划在无高风险语言时保持 `MEDIUM` plan risk；
- 直接退款、立刻退款、强制退款、投诉、平台介入、金额较高、多次售后、关闭争议、补偿、赔偿等表达触发
  `HIGH` risk 或高风险 subtask；
- 评测继续通过 `AgentPlanValidator`，不读取或硬编码 `caseId`；
- 默认评测继续使用 `RuleBasedAgentPlanner`，不依赖真实 LLM、API Key、网络、数据库、Redis 或向量库；
- `InMemoryPolicyRepository` 调整 keyword priority，优先匹配特殊商品和维修政策，减少泛化质量/退货命中误差。

### 14.3 不做

V2.10 不做：

- 不调用真实 LLM；
- 不做 LLM-as-judge；
- 不删除或降低 V2.9 evaluation case / assertions；
- 不引入数据库、Redis、向量库或前端；
- 不实现真实退款、真实换货、真实优惠券补偿、支付变更、物流变更或争议关闭；
- 不降低 ArchUnit、Checkstyle、SpotBugs 或 JUnit 约束。

### 14.4 状态

```text
COMPLETED
```

---

## 15. V3 候选方向

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

## 16. V2 当前完成度

```text
V2.1 LLM Planner Adapter ✅
V2.1.1 Structured LLM Planner Client ✅
V2.1.2 LLM Live Smoke Test ✅
V2.2 Order Query Tools ✅
V2.3 Multi-Intent Planning ✅
V2.4 Specialist Agent Handler ✅
V2.5 Policy Retrieval Tool ✅
V2.6 Agent Workspace / Structured Memory ✅
V2.7 Approval APIs ✅
V2.8 Execution Tree ✅
V2.9 Evaluation Dataset ✅
V2.10 Robustness ✅
```
