# Decision: Specialist Agent Handler

Date: 2026-05-16
Status: Accepted

## Context

V2.3 已完成 Multi-Intent Planning。系统现在可以把一个复杂售后 Ticket 拆解为多个结构化 `AgentSubtask`，并由
`AgentApplicationService` 按 priority 顺序执行：

```text
Ticket
→ AgentPlan with subtasks
→ AgentPlanValidator
→ AgentApplicationService
→ ToolRegistry
→ ToolCallTrace
```

当前仍由 `AgentApplicationService` 统一处理所有子任务。随着 `RETURN`、`EXCHANGE`、`COUPON_CONSULTATION`、
`LOGISTICS_ISSUE`、`GENERAL_CONSULTATION`、`HUMAN_ESCALATION` 等类型增加，把每类子任务处理细节继续堆在
`AgentApplicationService` 中会导致：

- 编排服务承担过多具体业务策略；
- 子任务类型处理逻辑难以单独测试；
- 后续新增类型容易影响既有流程；
- Specialist 分工无法清晰表达；
- 架构边界不利于后续 Execution Tree、Approval APIs 和评测集扩展。

因此，V2.4 引入 Specialist Agent Handler。

## Decision

V2.4 在 Java 模块化单体内引入策略类形式的 Specialist Agent Handler。

核心接口 / 模型包括：

```text
SpecialistAgentHandler
SpecialistAgentHandlerRegistry
SubtaskExecutionContext
SubtaskExecutionResult
```

职责划分：

```text
Planner / LLM：只生成结构化 subtasks
AgentApplicationService：校验计划并调度 handler
SpecialistAgentHandlerRegistry：按 SubtaskType 查找 handler
SpecialistAgentHandler：处理自己支持的 SubtaskType
ToolRegistry：唯一工具执行入口
ToolCallTrace：记录所有工具调用
```

V2.4 只做策略类分工，不做多 Agent 微服务、不引入消息队列、不做并行执行、不做投票共识。

Handler 仍然必须通过 ToolRegistry 调用工具。Handler 不得直接访问 Repository，不得绕过 RiskPolicy，不得直接执行真实退款、
真实换货、真实优惠券补偿、支付变更、物流变更或争议关闭。

## Consequences

Positive:

- `AgentApplicationService` 更专注于调度和状态维护；
- RETURN、EXCHANGE、COUPON_CONSULTATION、LOGISTICS_ISSUE 等子任务可以拥有独立策略；
- 每个 handler 可单独测试；
- 后续新增子任务类型更可控；
- 保持模块化单体和本地演示简单性；
- ToolRegistry 和 ToolCallTrace 边界保持稳定；
- 为 V2.7 Execution Tree 和 V2.8 Evaluation Dataset 提供更清晰的执行节点。

Negative:

- 类数量会增加；
- registry 需要处理重复类型和未覆盖类型；
- handler 之间共享上下文需要明确边界；
- 如果 handler 设计过重，可能演变成隐式微服务，需要架构规则约束。

Risk controls:

- handler 不访问 Repository；
- handler 不直接调用 LLM；
- handler 工具调用必须经过 ToolRegistry；
- handler 输出必须是 `SubtaskExecutionResult`；
- 高风险动作只能进入人工确认边界；
- 默认测试仍然离线运行；
- 后续增加 ArchUnit 规则防止 handler 依赖 repository。

## Alternatives Considered

### Alternative 1: 继续让 AgentApplicationService 处理所有 SubtaskType

拒绝。

短期最简单，但会让编排服务逐渐承载每类售后策略。随着类型增加，测试、调试和扩展都会变差。

### Alternative 2: 直接做多 Agent 微服务

拒绝。

V2.4 的目标是表达专业处理分工，不是引入部署拓扑复杂度。多 Agent 微服务会带来服务通信、部署、链路追踪、状态一致性和重试问题，
但并不能替代清晰的 handler 边界。

### Alternative 3: 引入消息队列

暂不采用。

当前子任务仍是单进程顺序执行。消息队列会引入异步状态、幂等、重试、死信和可观测性复杂度，不适合 V2.4 的最小目标。

### Alternative 4: 做并行执行

暂不采用。

并行执行会引入顺序、依赖、部分失败和汇总一致性问题。V2.4 先保持顺序执行，保证 trace 和测试确定性。

### Alternative 5: 让 LLM 直接选择并调用 Handler

拒绝。

LLM 仍然只负责生成结构化 subtasks。Java 后端必须校验计划并调度 handler，避免绕过 ToolRegistry、RiskPolicy 和审计边界。

### Alternative 6: Handler 直接调用业务 Service 或 Repository

拒绝。

Handler 是 Agent 执行策略，不是新的业务数据访问层。直接访问 Repository 会破坏既有架构约束和审计链路。业务能力必须通过
ToolRegistry 暴露并记录 trace。
