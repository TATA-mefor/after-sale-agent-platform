# Decision: Agent Workspace / Structured Memory

Date: 2026-05-16
Status: Accepted

## Context

AfterSale-Agent 已完成 V1 售后工单 Agent 闭环，以及 V2.1 LLM Planner Adapter、V2.2 Order Query Tools、
V2.3 Multi-Intent Planning、V2.4 Specialist Agent Handler 和 V2.5 Controlled Policy Retrieval。

当前执行链路是：

```text
Ticket
→ AgentPlanner
→ AgentPlan / AgentSubtasks
→ AgentPlanValidator
→ AgentApplicationService
→ SpecialistAgentHandlerRegistry
→ SpecialistAgentHandler
→ ToolRegistry
→ Order / Policy / Ticket Tools
→ ToolCallTrace
→ final summary
```

订单事实、政策依据、子任务结果、工具结果摘要和风险标记目前分散在 `AgentPlan`、`ToolCallTrace`、Ticket note、
subtask metadata 和最终 summary 中。它们都能表达一部分执行信息，但缺少一个面向单次 `AgentRun` 的结构化工作区。

继续只依赖这些分散位置会带来几个问题：

- handler 难以稳定复用前序工具结果；
- final summary 容易从局部变量或自然语言拼接中生成；
- ToolCallTrace 更适合审计和排错，不适合作为主工作记忆；
- 如果把所有上下文塞进 LLM prompt，会增加 token 成本、泄露面和测试不确定性；
- 后续 Execution Tree、Evaluation Dataset 和 Approval APIs 缺少统一上下文来源。

## Decision

V2.6 将引入 Agent Workspace / Structured Memory 的设计边界。

`AgentWorkspace` 是单次 `AgentRun` 内部的结构化工作记忆。它用于承载当前执行所需的订单事实、政策依据、子任务记忆、
工具结果摘要和风险标记。

核心模型候选：

```text
AgentWorkspace
OrderFact
PolicyEvidence
SubtaskMemory
ToolResultSummary
RiskFlag
```

职责划分：

```text
AgentApplicationService：创建 workspace，传递给 handler，基于 workspace 汇总 final summary
SpecialistAgentHandler：读取 workspace 上下文，执行工具后写入结构化结果
ToolRegistry：继续只负责工具执行和风险边界
ToolCallTrace：继续作为审计记录
AgentWorkspace：当前 AgentRun 内的结构化工作记忆
```

V2.6 不做长期记忆、不做用户画像、不做向量记忆、不做跨会话记忆。Workspace 先采用单次 `AgentRun` 内存级结构，避免在模型尚未稳定前引入 Redis、MySQL 或向量库。

后续可以将 workspace 持久化到 MySQL、缓存到 Redis，或把其中的 `PolicyEvidence` / `ToolResultSummary` 用于
Execution Tree、Evaluation Dataset 和 Approval APIs，但这些都不是 V2.6 的初始实现目标。

## Consequences

Positive:

- handler 可以通过结构化 workspace 复用订单事实、政策依据和前序子任务结果；
- final summary 可以从 workspace 汇总，而不是散落依赖局部变量或 Ticket note；
- ToolCallTrace 和工作记忆边界更清晰；
- 降低把完整上下文塞进 LLM prompt 的需求；
- 为 Execution Tree、Evaluation Dataset 和 Approval APIs 提供统一上下文来源；
- 单次 AgentRun 内存结构保持默认测试确定性。

Negative:

- 会增加一组新的 Agent 执行模型；
- 需要定义 handler 写入一致性规则；
- 需要测试 workspace 与 ToolCallTrace 的边界；
- 如果边界不清，workspace 可能被误用为长期记忆或审计替代品。

Risk controls:

- workspace 不得保存 API Key、敏感凭证、完整长 prompt 或 LLM 原始长文本；
- workspace 不得替代 ToolCallTrace；
- workspace 不得绕过 ToolRegistry 执行工具；
- workspace 不得直接访问 Repository；
- V2.6 默认测试不得依赖真实 LLM、API Key、Redis、MySQL、向量库或外部网络；
- V2.6 不实现长期用户画像或跨会话记忆。

## Alternatives Considered

### Alternative 1: 继续只依赖 ToolCallTrace 作为工作记忆

拒绝。

ToolCallTrace 是审计记录，适合保存工具输入、输出、状态、错误和延迟。它不适合作为 handler 之间的主工作记忆：
trace 查询和审计格式不等同于执行上下文模型，直接依赖 trace 会让业务汇总逻辑与审计存储耦合。

### Alternative 2: 把所有上下文都塞进 LLM prompt

拒绝。

LLM 仍然只负责规划，不负责执行。把订单事实、政策依据、工具结果和子任务状态持续塞入 prompt 会增加 token 成本、
泄露风险和测试不确定性，也会弱化 Java 后端对执行上下文的控制。

### Alternative 3: 直接做长期记忆 / 用户画像 / 跨会话记忆

拒绝。

当前目标是单次 AgentRun 内部执行上下文，不是用户长期画像。长期记忆会引入权限、隐私、删除、过期、跨会话一致性和评测复杂度，
会扩大 V2.6 范围。

### Alternative 4: 直接接 Redis / MySQL 持久化 workspace

暂不采用。

模型边界应先稳定。直接接 Redis 或 MySQL 会引入 schema、迁移、序列化、清理和环境依赖。V2.6 先采用内存级结构，后续再决定是否持久化。

### Alternative 5: 直接做向量记忆

拒绝。

V2.6 不是向量记忆，也不需要 embedding 或 PGvector。政策检索已由 V2.5 的受控工具承担；workspace 负责结构化执行上下文，而不是相似度召回。
