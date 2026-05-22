
# Decision: V4 Tool / Skill Layer

Date: 2026-05-22
Status: Accepted

## Context

AfterSale-Agent V1/V2/V3 已经建立了 AgentPlanner、ToolRegistry、ToolCallTrace、Approval、AgentWorkspace、SpecialistAgentHandler 和 Execution Tree 边界。当前系统已经有工具调用和 specialist handler，但 Tool 与 Skill 的关系还没有作为一级 Harness 概念固定下来。

V4 将引入 Spring AI、RAG、VectorStore、PGvector 和更复杂的政策证据链。如果不先定义 Tool / Skill 分层，后续实现容易出现以下问题：

- Agent 或 Handler 直接访问 VectorStore；
- Skill 直接注入 Repository、ChatClient 或 EmbeddingModel；
- LLM 输出被当成实际执行结果；
- RAG 检索被当成最终业务结论；
- ToolCallTrace 无法解释复合任务内部调用；
- Execution Tree 缺少 Skill 层节点；
- Codex 为了实现快而绕过 ToolRegistry 或 RiskPolicy。

## Decision

V4 引入 Tool / Skill 分层。

```text
Tool = 原子可执行能力
Skill = 可复用复合任务能力
```

Tool 继续由 ToolRegistry 管理。Skill 由新增 SkillRegistry 管理。Planner 可以规划 Tool 或 Skill，但不得执行 Tool 或 Skill。

V4 目标链路：

```text
LLM / RuleBased Planner
→ AgentPlan / AgentSubtask
→ SkillRegistry
→ AgentSkill
→ ToolRegistry
→ ToolExecutor
→ ToolCallTrace
→ AgentWorkspace
→ Execution Tree
```

## Tool Boundary

Tool 是原子动作，必须声明：

```text
toolName
description
inputSchema
outputSchema
riskLevel
requiresApproval
failureModes
traceFields
```

Tool 必须通过 ToolRegistry 执行。每次实际 Tool 调用必须记录 ToolCallTrace。

## Skill Boundary

Skill 是 Java 后端复合任务策略，必须声明：

```text
skillName
description
supportedSubtaskTypes
inputSchema
outputSchema
requiredTools
optionalTools
riskLevel
requiresApprovalWhen
evidenceRequirements
workspaceReads
workspaceWrites
failureModes
```

Skill 可以读取 AgentWorkspace，可以写入 AgentWorkspace，可以组合多个 Tool，但必须通过 ToolRegistry 调用 Tool。

Skill 不得：

- 直接访问 Repository；
- 直接访问 VectorStore；
- 直接访问 JdbcTemplate / DataSource；
- 直接访问 Spring AI ChatClient；
- 直接访问 Spring AI EmbeddingModel；
- 直接调用外部退款、支付、物流、库存或优惠券系统；
- 直接执行真实高风险动作；
- 隐藏 ToolCallTrace；
- 声称真实退款、换货、补偿、支付或物流已经完成。

## Skill Risk Aggregation

Skill 风险等级由以下因素聚合：

```text
max(subtask riskLevel, required tool riskLevel, intended business outcome riskLevel)
```

如果 Skill 涉及退款、赔偿、优惠券补偿、争议关闭、支付状态变更、物流变更或库存变更，必须进入 HIGH risk / Approval 边界，不得自动执行。

## Relationship with Existing SpecialistAgentHandler

V4 不要求一次性删除 SpecialistAgentHandler。

迁移策略：

```text
SpecialistAgentHandler
→ AgentSkill adapter
→ SkillRegistry
→ gradual replacement / coexistence
```

Return、Exchange、Coupon、Logistics、General Consultation、Human Escalation handler 可逐步映射为 Skill。

## Consequences

Positive:

- 面试时可以清晰解释 Tool 与 Skill 的工程分层；
- 复合任务具备独立测试和可审计节点；
- RAG evidence 可以通过 Skill 编排进入 Workspace 和 Execution Tree；
- ToolRegistry 继续作为唯一原子工具执行入口；
- 高风险边界延伸到 Skill 层。

Costs:

- 需要新增 SkillRegistry、AgentSkill、SkillExecutionContext、SkillExecutionResult；
- 需要更新 AgentPlanValidator 以校验 plannedSkills；
- Execution Tree 需要支持 Skill node；
- 文档和测试需要覆盖 Tool / Skill 非绕行边界。

## Non-goals

- 不做微服务多 Agent；
- 不做队列或并行执行；
- 不让 LLM 直接调用 Skill；
- 不让 Skill 绕过 ToolRegistry；
- 不让 Skill 直接访问 RAG / VectorStore 底层实现；
- 不执行真实退款、换货、补偿、支付、物流或库存动作。
