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
V2.3 MySQL Persistence
V2.4 Approval APIs
V2.5 Agent Evaluation Dataset
V2.6 Docker Compose and Observability
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
- AgentRun 继续通过 ToolRegistry 执行工具并记录 ToolCallTrace；
- README planner mode 配置说明；
- Planner 相关测试与架构边界测试。

V2.1 未完成且不伪装完成：

- 真实 LLM provider SDK 调用；
- LLM 响应 JSON 解析；
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

---

# 6. V2.3：MySQL Persistence

## 6.1 目标

替换 V1 内存 Repository，使 Ticket、AgentRun、ToolCallTrace 可持久化。

## 6.2 必做

- 引入 MySQL profile；
- 新增表结构脚本；
- 替换 TicketRepository、AgentRunRepository、ToolCallTraceRepository 的实现；
- 保留内存 profile 供测试使用；
- 添加集成测试或 Testcontainers 方案。

## 6.3 不做

- 不做复杂分库分表；
- 不引入微服务；
- 不做生产级运维脚本。

## 6.4 验收标准

- 本地 MySQL profile 可启动；
- 测试环境仍可离线运行；
- 重启后工单和 trace 不丢失；
- V1/V2 demo 可复现。

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
