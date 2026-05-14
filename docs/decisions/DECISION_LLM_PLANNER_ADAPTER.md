# Decision: LLM Planner Adapter

Date: 2026-05-14
Status: Accepted

## Context

AfterSale-Agent V1 已完成规则型 AgentRun 闭环：

```text
Ticket
→ Rule-based intent detection
→ search_aftersale_policy
→ add_ticket_note
→ ToolCallTrace
```

V2 需要接入真实 LLM，以提升 Agent 对自然语言售后问题的理解能力和计划生成能力。

但是企业级售后系统存在高风险动作，例如退款、补偿、关闭争议工单、修改订单状态。如果让 LLM 直接执行工具或直接修改业务状态，会带来以下问题：

- 行为不可控；
- 测试不确定；
- 高风险动作可能被误执行；
- trace 难以解释责任边界；
- 后端业务规则可能被绕过；
- 项目已有 ToolRegistry / RiskPolicy / Trace 边界会被破坏。

因此，V2.1 选择引入 LLM Planner Adapter，而不是直接引入 LLM Tool Calling。

## Decision

采用 `AgentPlanner` 抽象。

`AgentApplicationService` 只依赖 `AgentPlanner` 接口，不直接依赖具体 LLM SDK。

规划器实现分为：

```text
RuleBasedAgentPlanner
LlmAgentPlanner
FakeAgentPlanner
```

职责划分：

```text
LLM Planner：生成结构化 AgentPlan
Java Backend：校验 AgentPlan
ToolRegistry：执行工具
RiskPolicy：拦截高风险动作
ToolCallTrace：记录执行轨迹
```

LLM 只能输出结构化计划，不得直接执行工具。

AgentPlan 至少包含：

```text
intent
riskLevel
policyQuery
noteToAdd
finalSuggestion
evidenceHints
plannedTools
```

配置项：

```yaml
agent:
  planner:
    mode: rule # rule | llm | fake
```

默认测试环境使用 `rule` 或 `fake`，不得依赖真实 LLM、API Key 或外部网络。

LLM API Key 只能来自环境变量或本地配置，例如：

```text
OPENAI_API_KEY
```

禁止将真实 API Key 写入代码、测试、README 或提交历史。

## Consequences

### Positive

- 保留 V1 的确定性测试能力；
- 可以真实接入 LLM，同时不破坏 ToolRegistry 边界；
- 高风险动作仍然受审批机制约束；
- 便于在面试中解释“LLM 规划、Java 执行”的企业级设计；
- 未来可以替换不同模型供应商；
- FakeAgentPlanner 可以稳定测试 AgentRun 流程；
- RuleBasedAgentPlanner 可以作为降级路径。

### Negative

- V2.1 不会展示“LLM 直接工具调用”的完整能力；
- 需要额外设计 AgentPlan DTO 和解析失败处理；
- LLM 输出需要校验，否则可能产生非法工具名、非法风险等级或缺失字段；
- 真实 LLM 模式仍存在延迟、费用和不稳定性。

### Risks

- LLM 输出结构不符合契约；
- 缺少 API Key 时开发者误以为系统坏了；
- planner mode 配置错误导致测试不稳定；
- 后续开发者可能试图让 LLM 绕过 ToolRegistry。

对应防护：

- 文档约束；
- 配置隔离；
- Fake planner 测试；
- ArchUnit 边界；
- AgentPlan 校验；
- ToolRegistry 统一执行。

## Alternatives Considered

### Alternative 1: LLM 直接执行工具

拒绝。

原因：

- 难以保证高风险动作不被误触发；
- 难以保证测试确定性；
- 会绕过 V1 已建立的 ToolRegistry、RiskPolicy、Trace 边界。

### Alternative 2: 继续只使用规则型 Planner

拒绝作为 V2 主方向。

原因：

- 规则型 Planner 可解释、稳定，但无法体现真实 LLM 接入；
- 对复杂自然语言售后问题适应性有限；
- 简历和面试竞争力不足。

但规则型 Planner 必须保留为本地测试和降级路径。

### Alternative 3: 直接接入 LangChain / Dify / 外部 Agent 平台

暂不采用。

原因：

- 当前项目定位是 Java 后端 Agent 工程项目；
- 需要展示 Java 后端如何控制工具、状态、trace 和风险边界；
- 直接使用外部平台会弱化后端工程含量。

### Alternative 4: LLM 只生成自然语言建议

拒绝。

原因：

- 自然语言建议无法稳定驱动后端工具；
- 不利于测试；
- 不利于 trace 和审计；
- 不符合企业级 Agent 的结构化执行需求。
