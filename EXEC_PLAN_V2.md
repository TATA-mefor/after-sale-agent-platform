# EXEC_PLAN_V2.md

# AfterSale-Agent V2 执行计划

## 1. V2 总目标

V1 已完成一个可本地演示的售后工单 Agent 后端闭环：

```text
创建售后工单
→ 触发规则型 AgentRun
→ 检索售后政策
→ 写入工单备注
→ 记录 ToolCallTrace
→ 查询执行轨迹
```

V2 的目标是在不破坏 V1 工程边界的前提下，把项目从“规则型 Agent Demo”升级为“可接入真实 LLM、可扩展真实业务系统、可评测、可部署的企业级 Agent 平台雏形”。

V2 的第一优先级是：

> 接入真实 LLM Planner Adapter，但仍然由 Java 后端控制工具执行。

## 2. V2 核心原则

1. LLM 只负责生成结构化计划，不直接执行业务动作；
2. Java 后端仍然通过 ToolRegistry 执行工具；
3. 高风险动作仍然必须进入审批边界；
4. 测试不得依赖真实 LLM、API Key 或外部网络；
5. RuleBasedAgentPlanner 必须保留，保证本地可演示和测试可确定；
6. FakeAgentPlanner 必须用于测试边界；
7. ToolCallTrace 机制必须继续保留；
8. V2 不允许绕过 V1 已建立的 ArchUnit、Checkstyle、SpotBugs、JUnit 质量门禁。

## 3. V2 里程碑

```text
V2.1 LLM Planner Adapter
V2.2 Order Query Tools
V2.3 Multi-Intent Planning
V2.4 Approval APIs / Specialist Handler
V2.5 MySQL Persistence
V2.6 Agent Evaluation Dataset
V2.7 Docker Compose and Observability
```

V2.1 是第一优先级。

---

# 4. V2.1：LLM Planner Adapter

## 4.1 目标

引入真实 LLM 规划能力，使 Agent 可以基于用户售后问题生成结构化计划。

但 LLM 不直接调用工具、不直接修改业务状态、不直接生成不可追踪的最终结果。

目标链路：

```text
Ticket
→ AgentApplicationService
→ AgentPlanner 抽象
→ RuleBasedAgentPlanner / LlmAgentPlanner / FakeAgentPlanner
→ AgentPlan
→ ToolRegistry
→ ToolCallTrace
→ Ticket note / final suggestion
```

## 4.2 必做

- 新增 `AgentPlanner` 抽象接口；
- 新增 `AgentPlanningContext`；
- 新增 `AgentPlan`；
- 将 V1 规则型逻辑迁移到 `RuleBasedAgentPlanner`；
- 新增 `LlmAgentPlanner`；
- 新增 `FakeAgentPlanner` 或测试专用 planner；
- 通过配置选择 planner：
  - `agent.planner.mode=rule`
  - `agent.planner.mode=llm`
  - `agent.planner.mode=fake`
- 默认测试环境必须使用 rule 或 fake；
- LLM API Key 只能来自环境变量或本地配置；
- 新增决策日志；
- 更新 README 的 LLM Planner 配置说明。

## 4.3 AgentPlan 字段

`AgentPlan` 至少包含：

```text
intent
riskLevel
policyQuery
noteToAdd
finalSuggestion
evidenceHints
plannedTools
```

## 4.4 不做

- 不让 LLM 直接执行工具；
- 不让 LLM 直接修改 Ticket；
- 不接真实退款；
- 不接真实数据库；
- 不接向量库；
- 不做多 Agent；
- 不让测试依赖外部网络；
- 不把 API Key 写入代码、测试或 README。

## 4.5 验收标准

- `AgentApplicationService` 依赖 `AgentPlanner` 抽象；
- V1 端到端测试继续通过；
- `RuleBasedAgentPlanner` 能生成 V1 等价计划；
- `FakeAgentPlanner` 能驱动 AgentRun 测试；
- `agent.planner.mode=llm` 时缺少配置有清晰错误；
- 默认 `mvn test` 不需要 API Key；
- ToolRegistry 仍是唯一工具执行入口；
- ToolCallTrace 继续记录工具调用。

## 4.6 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## 4.7 当前状态

```text
COMPLETED
```

V2.1 已完成：

- `AgentPlanner` 抽象；
- `AgentPlanningContext`；
- `AgentPlan`；
- `RuleBasedAgentPlanner`；
- `FakeAgentPlanner`；
- `LlmAgentPlanner` adapter 边界；
- `agent.planner.mode=rule|fake|llm` 配置选择；
- 默认 rule 模式；
- llm 模式缺少 API Key 时的清晰错误；
- V2.1.1 真实 LLM provider client 边界；
- 结构化 AgentPlan JSON 解析；
- AgentPlan 枚举、必填字段、工具名和未执行事实声明校验；
- V2.1.2 显式 opt-in live smoke test；
- AgentRun 继续通过 ToolRegistry 执行工具并记录 ToolCallTrace；
- README planner mode 配置说明；
- Planner 相关测试与架构边界测试。

V2.1 不包含且不伪装完成：

- Order Query Tools；
- MySQL Persistence；
- Approval APIs。

---

# 5. V2.2：Order Query Tools

## 5.1 目标

让 Agent 的处理建议同时基于售后政策和订单事实。

## 5.2 必做

- 新增或完善订单 demo 数据；
- 实现 `get_order_by_id` 工具；
- 实现 `get_user_orders` 工具；
- 工具输出包含订单状态、支付时间、签收时间、售后截止时间；
- AgentPlan 中可以声明订单查询工具；
- ToolCallTrace 中能看到订单工具调用。

## 5.3 不做

- 不接真实订单中心；
- 不接真实物流；
- 不接真实支付；
- 不修改订单核心数据。

## 5.4 验收标准

- AgentRun trace 中包含订单查询工具；
- 最终建议包含订单依据和政策依据；
- 订单工具为 LOW 风险；
- Agent 不直接访问 OrderRepository。

## 5.5 当前状态

```text
COMPLETED
```

V2.2 已完成：

- 内存 demo 订单数据；
- `OrderApplicationService`；
- `get_order_by_id` 工具；
- `get_user_orders` 工具；
- 订单工具通过 `ToolRegistry` 暴露；
- `RuleBasedAgentPlanner` 默认规划 `get_order_by_id`；
- AgentRun trace 记录订单工具调用；
- Agent 最终建议同时包含订单依据和政策依据；
- README、工具契约、风险策略和 LLM Planner Contract 已同步。

V2.2 不包含且不伪装完成：

- 真实订单中心；
- 真实数据库；
- 真实物流；
- 真实支付；
- 退款或补偿执行。

---

# 6. V2.3：Multi-Intent Planning

## 6.1 目标

支持复杂售后诉求拆解为多个结构化子任务，让一个 Ticket 可以表达多个售后意图，并在同一个受控
AgentRun 中按顺序规划、校验和执行。

示例复杂售后问题：

```text
我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？
```

期望拆解：

```text
RETURN 子任务
EXCHANGE 子任务
COUPON_CONSULTATION 子任务
```

V2.3 的目标不是引入多 Agent 平台，而是在现有 `AgentPlanner -> AgentPlan -> ToolRegistry -> ToolCallTrace`
链路上增加可校验的多意图计划结构。

## 6.2 必做

- 定义核心模型契约：
  - `AgentSubtask`
  - `SubtaskType`
  - `SubtaskStatus`
  - `SubtaskPlan`
  - `MultiIntentAgentPlan`
- 让 Planner 能输出包含 `subtasks` 的结构化计划；
- 支持至少以下子任务类型：
  - `RETURN`
  - `EXCHANGE`
  - `COUPON_CONSULTATION`
  - `LOGISTICS_ISSUE`
  - `GENERAL_CONSULTATION`
- 每个子任务至少包含目标对象、用户原文片段、优先级、风险等级、政策检索 query、计划工具和依赖；
- Java 后端校验子任务类型、风险等级、工具名、依赖关系和高风险声明；
- `AgentApplicationService` 在单进程内按顺序执行子任务计划；
- ToolRegistry 仍然是唯一工具执行入口；
- ToolCallTrace 继续记录每个工具调用；
- 默认测试仍不依赖真实 LLM。

## 6.3 不做

- 不做多 Agent 微服务；
- 不做消息队列；
- 不做并行执行；
- 不做投票共识；
- 不做完整优惠券系统；
- 不做真实退款；
- 不做真实换货；
- 不接真实物流；
- 不接真实支付；
- 不绕过 ToolRegistry；
- 不让 LLM 直接执行子任务。

## 6.4 验收标准

- 复杂售后问题可以被拆解为多个结构化子任务；
- `MultiIntentAgentPlan` 可以表达多个 `AgentSubtask`；
- 每个子任务的 `plannedTools` 均来自 ToolRegistry 已注册工具；
- 子任务依赖关系可校验，禁止循环依赖和未知依赖；
- 子任务不能声明高风险动作已经完成；
- 单进程顺序执行可以产生清晰 ToolCallTrace；
- AgentRun 最终建议能汇总多个子任务结果；
- V1/V2.1/V2.2 默认测试继续通过；
- 默认测试不依赖真实 LLM、API Key 或外部网络。

## 6.5 示例结构化输出

```json
{
  "intent": "MULTI_INTENT",
  "riskLevel": "MEDIUM",
  "policyQuery": "服装退货 换尺码 优惠券退回",
  "noteToAdd": "用户包含退货、换尺码和优惠券咨询三个诉求，需按子任务处理。",
  "finalSuggestion": "建议分别核对污渍退货政策、尺码换货政策和优惠券使用规则。",
  "evidenceHints": [
    "一件衣服有污渍",
    "另一件需要换尺码",
    "用户咨询优惠券未使用如何退回"
  ],
  "plannedTools": [
    {
      "toolName": "get_order_by_id",
      "reason": "查询订单事实"
    }
  ],
  "subtasks": [
    {
      "subtaskId": "SUB-1",
      "type": "RETURN",
      "target": "有污渍的衣服",
      "userMessageFragment": "其中一件有污渍要退货",
      "priority": 1,
      "riskLevel": "MEDIUM",
      "policyQuery": "服装 污渍 退货",
      "plannedTools": [
        {
          "toolName": "search_aftersale_policy",
          "reason": "检索质量问题退货政策"
        }
      ],
      "dependencies": []
    },
    {
      "subtaskId": "SUB-2",
      "type": "EXCHANGE",
      "target": "需要换尺码的衣服",
      "userMessageFragment": "另一件要换尺码",
      "priority": 2,
      "riskLevel": "MEDIUM",
      "policyQuery": "服装 尺码 换货",
      "plannedTools": [
        {
          "toolName": "search_aftersale_policy",
          "reason": "检索尺码换货政策"
        }
      ],
      "dependencies": []
    },
    {
      "subtaskId": "SUB-3",
      "type": "COUPON_CONSULTATION",
      "target": "未使用优惠券",
      "userMessageFragment": "还有一张优惠券没用上怎么退",
      "priority": 3,
      "riskLevel": "LOW",
      "policyQuery": "优惠券 未使用 退回",
      "plannedTools": [
        {
          "toolName": "search_aftersale_policy",
          "reason": "检索优惠券规则"
        }
      ],
      "dependencies": []
    }
  ]
}
```

## 6.6 测试要求

- `SubtaskType` 支持 RETURN / EXCHANGE / COUPON_CONSULTATION / LOGISTICS_ISSUE；
- 合法 `MultiIntentAgentPlan` 能通过校验；
- 未知子任务类型被拒绝；
- 未知工具名被拒绝；
- 子任务循环依赖被拒绝；
- 子任务高风险完成声明被拒绝；
- 复杂售后文本能生成多个子任务；
- AgentRun trace 能区分或关联子任务工具调用；
- 默认 `mvn test` 离线通过。

## 6.7 验证命令

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## 6.8 当前状态

```text
PLANNED
```

V2.3 当前仅完成 Harness 文档设计，尚未实现 Java 模型、校验器或执行流程。

---

# 7. V2.4：Approval APIs

## 7.1 目标

把 V1 中的高风险审批边界落成 API。

## 7.2 必做

- 创建审批请求；
- 查询待审批动作；
- 审批通过；
- 审批拒绝；
- 审批结果写回工单；
- trace 中记录审批相关动作。

## 7.3 不做

- 不做完整前端后台；
- 不接真实退款；
- 不接真实补偿。

## 7.4 验收标准

- HIGH 工具不会直接执行；
- HIGH 工具会创建 ApprovalRequest；
- 审批通过后才允许进入后续动作；
- 审批拒绝后工单有清晰备注。

---

# 8. V2.5：Agent Evaluation Dataset

## 8.1 目标

为 Agent 输出质量建立最小评测集。

## 8.2 必做

- 新增 `docs/evaluation/`；
- 定义 20 条售后测试样例；
- 每条包含用户问题、期望 intent、期望 policy category、期望风险等级；
- 新增离线评测命令或测试；
- 记录准确率、工具调用命中率、失败案例。

## 8.3 不做

- 不做复杂 LLM-as-judge；
- 不做线上 A/B；
- 不做大规模标注系统。

## 8.4 验收标准

- 可以离线运行评测；
- 评测不依赖真实 LLM；
- LLM 模式下可手动运行评测；
- 失败案例可被记录到质量文档。

---

# 9. V2.6：Docker Compose and Observability

## 9.1 目标

提升项目可部署性和可观察性。

## 9.2 必做

- Docker Compose 启动应用和 MySQL；
- 结构化日志；
- requestId / ticketId / runId 贯穿日志；
- 可选接入 Prometheus actuator metrics；
- README 增加一键启动说明。

## 9.3 不做

- 不做 Kubernetes；
- 不做完整生产监控；
- 不做复杂告警系统。

## 9.4 验收标准

- 本地可一键启动；
- Demo 可通过 Docker Compose 复现；
- 日志能关联 ticketId 和 agentRunId。

---

# 10. V2 成功标准

V2 成功不是“加了一个 LLM 调用”，而是：

- LLM 被纳入可控 Planner 边界；
- 业务工具执行仍由 Java 后端控制；
- 测试仍然确定性；
- 高风险动作仍然受审批边界约束；
- trace 能解释 Agent 行为；
- V1 demo 没有被破坏；
- 后续可平滑扩展真实数据库、订单工具、评测和部署。
