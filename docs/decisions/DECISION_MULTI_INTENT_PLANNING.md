# Decision: Multi-Intent Planning

Date: 2026-05-16
Status: Accepted

## Context

AfterSale-Agent V1 已完成售后工单 Agent 闭环，V2.1 已引入 LLM Planner Adapter，V2.1.1 已完成结构化 LLM Planner Client，V2.1.2 已提供 opt-in live smoke test，V2.2 已补充订单查询工具。

当前受控链路是：

```text
Ticket
→ Planner
→ get_order_by_id
→ search_aftersale_policy
→ add_ticket_note
→ ToolCallTrace
```

现实售后问题经常包含多个诉求，例如：

```text
我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？
```

单一 `intent` 无法准确表达这类问题。系统需要把一个 Ticket 拆解为多个结构化子任务，例如 `RETURN`、`EXCHANGE`、`COUPON_CONSULTATION`，并保留可校验、可审计、可测试的执行边界。

## Decision

V2.3 先做 Multi-Intent Planning，而不是直接做微服务多 Agent。

核心模型包括：

```text
AgentSubtask
SubtaskType
SubtaskStatus
SubtaskPlan
MultiIntentAgentPlan
```

V2.3 的边界是：

- Supervisor Planner 负责生成 `MultiIntentAgentPlan`；
- LLM 可以规划子任务，但不能执行子任务；
- Java 后端必须校验子任务类型、工具名、风险等级和依赖关系；
- `AgentApplicationService` 在单进程内顺序执行子任务计划；
- `ToolRegistry` 仍然是唯一工具执行入口；
- `ToolCallTrace` 继续记录每个工具调用；
- Specialist Handler 放到 V2.4。

子任务必须结构化，因为后端需要机械化校验、确定性测试、trace 归因和后续 Execution Tree 扩展。自然语言拆解不能作为可执行计划。

V2.3 不做多 Agent 微服务、不做消息队列、不做并行执行、不做投票共识、不做完整优惠券系统、不做真实退款或换货、不接真实物流或支付。

## Consequences

Positive:

- 能表达一个 Ticket 下多个售后诉求；
- 为复杂问题拆解建立可验证模型；
- 保持当前模块化单体和默认离线测试路径；
- 避免微服务、消息队列和并行执行带来的状态复杂度；
- 为 V2.4 Specialist Handler 和 V2.7 Execution Tree 提供稳定输入。

Negative:

- `AgentPlan`、parser 和 validator 会更复杂；
- 需要校验 subtask 依赖关系和循环依赖；
- 线性 `ToolCallTrace` 对子任务归属的表达能力有限；
- RuleBased planner 只能覆盖有限的多意图样例；
- LLM 输出结构更复杂，非法输出概率更高。

Risk controls:

- 子任务类型白名单；
- `plannedTools` 必须来自 `ToolRegistry`；
- `riskLevel` 必须来自系统枚举；
- `dependencies` 必须引用同一计划中的合法 `subtaskId`，且不得形成循环；
- 子任务不得声明真实退款、真实换货、优惠券补偿或争议关闭已经完成；
- 默认测试不得依赖真实 LLM、API Key 或外部网络。

## Alternatives Considered

### Alternative 1: 先做微服务多 Agent

拒绝。

当前项目需要先证明复杂售后问题可以被结构化拆解并受控执行。微服务会引入部署、服务通信、状态一致性、重试和观测成本，但不能替代子任务模型本身。模块化单体足以表达 V2.3 所需边界。

### Alternative 2: 先做并行执行

暂不采用。

V2.3 的主要风险是计划结构和校验，而不是吞吐量。并行执行会引入时序、幂等、部分失败和补偿问题，降低默认测试确定性。顺序执行更适合当前 Harness 阶段。

### Alternative 3: 使用自然语言列表表达子任务

拒绝。

自然语言列表无法稳定校验子任务类型、工具名、风险等级、依赖关系和完成声明，也无法为 ToolCallTrace、评测集和 Specialist Handler 提供可靠输入。

### Alternative 4: 让 LLM 直接调用工具

拒绝。

这会绕过 Java 后端、`ToolRegistry`、风险策略和 trace 边界。V2.3 延续 V2.1 决策：LLM 只负责规划，Java 后端负责执行。

### Alternative 5: 先实现 Specialist Handler

暂不采用。

Specialist Handler 需要稳定的 `SubtaskPlan` 输入。V2.3 先定义 Multi-Intent 计划契约和校验边界，V2.4 再引入 `ReturnSubtaskHandler`、`ExchangeSubtaskHandler`、`CouponConsultationHandler` 等专门处理器。
