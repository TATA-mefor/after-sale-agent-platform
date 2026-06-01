# Decision: Project Review Spring AI Deepening Evaluation

Date: 2026-06-01

Status: Completed

## Context

项目审查指出 Spring AI 当前只使用了较浅的 ChatClient / EmbeddingModel adapter 能力，尚未使用
ChatMemory、Advisors、Spring AI Tool Calling API 或 bulk embedding。本决策用于校准该结论：这些能力确实未实现，
但这是当前 Agent 安全边界下的有意取舍，不是默认路径缺陷。

V4 的 Spring AI 目标是建立 provider adapter foundation，而不是让 Spring AI 替代项目已有的 Agent planning、
ToolRegistry、Approval、ToolCallTrace、Workspace、Execution Tree 和 RAG evidence 边界。

## Current Spring AI Baseline

当前基线是 Spring AI Chat adapter foundation 和 Spring AI embedding adapter foundation：

- `LlmClient` abstraction 隔离 Spring AI chat provider；
- LlmClient abstraction keeps chat provider access behind the project boundary；
- `SpringAiLlmClient` 只作为 `LlmClient` 的可选实现；
- provider 输出仍必须经过 `AgentPlanParser` 和 `AgentPlanValidator`；
- `EmbeddingClient` abstraction 隔离 embedding provider；
- EmbeddingClient abstraction keeps embedding provider access behind the project boundary；
- `SpringAiEmbeddingClient` 只作为 `EmbeddingClient` 的可选实现；
- `FakeEmbeddingClient` 保证默认测试离线、确定性；
- live Spring AI smoke tests are opt-in；
- 默认 profile 不要求真实 ChatModel、EmbeddingModel、VectorStore、API Key 或外部网络。

## Problem Statement

如果直接引入 Spring AI 深层能力，可能产生以下风险：

- ChatMemory 可能绕开单次 `AgentWorkspace` 的边界，形成未设计的长期记忆；
- Advisors 可能把 RAG evidence 注入 prompt，但绕过项目自己的检索、合并、审计和 evidence-only 规则；
- Spring AI Tool Calling API 可能让 provider 直接触发工具，绕过 `ToolRegistry`、RiskPolicy、Approval 和
  ToolCallTrace；
- bulk embedding 如果直接暴露给 ingestion runtime，可能绕过 `EmbeddingClient` abstraction、默认离线 fake path
  和 provider 限流策略。

## Decision

Stage 4 completed the decision/evaluation only. Stage 4 does not implement ChatMemory, Advisors, Spring AI Tool
Calling API, bulk embedding, provider runtime changes, live provider behavior changes, public RAG HTTP endpoints, or
RAG retrieval quality changes.

未来如果引入这些能力，必须保持以下原则：

- Spring AI 深层能力只能位于项目 adapter / application 边界后方；
- LLM must not directly execute tools；
- Spring AI Tool Calling API cannot replace ToolRegistry；
- ToolRegistry boundary must be preserved；
- `AgentPlanParser` and `AgentPlanValidator` must not be bypassed；
- high-risk actions still require Approval；
- ToolCallTrace 仍是工具调用审计记录；
- RAG evidence 仍是 policy evidence，不是业务动作或业务决策。

## ChatMemory Evaluation

ChatMemory is not implemented in Stage 4.

后续只有在明确用途时才考虑 ChatMemory，例如 provider conversation context 的 adapter 内部优化。它不得替代：

- `AgentWorkspace` 的单次 AgentRun 工作记忆；
- `ToolCallTrace` 的审计记录；
- Execution Tree 的只读解释视图；
- 业务数据库或长期用户画像。

如果未来实现，ChatMemory 必须禁用默认路径或显式 opt-in，并提供离线 fake / deterministic tests。

## Advisor Evaluation

Advisors are not implemented in Stage 4.

QuestionAnswerAdvisor、DynamicToolAdvisor 或类似能力只有在不绕过项目 RAG service 和 ToolRegistry 边界时才可评估。
Advisor 可以作为 prompt assembly 的受控辅助，但不得：

- 绕过 `search_aftersale_policy`；
- 绕过 RAG evidence merge service；
- 暴露 raw prompt、raw provider response、完整 evidence chunk、API Key、provider config 或本地路径；
- 把 policy evidence 写成退款、换货、补偿、支付、物流或争议关闭已经完成。

## Tool Calling API Evaluation

Spring AI Tool Calling API is not enabled in Stage 4.

Spring AI Tool Calling API 不能成为项目工具执行入口。未来如果做实验，也只能作为 adapter 层受控输入来源，
由 Java 后端解析、校验并通过 `ToolRegistry` 执行。它不得：

- 直接调用项目 `ToolExecutor`；
- 跳过 RiskPolicy；
- 跳过 Approval；
- 跳过 ToolCallTrace；
- 让 LLM 直接修改 Ticket、AgentRun、Workspace 或 Execution Tree；
- 执行真实退款、换货、优惠券补偿、支付、物流或争议关闭。

## Bulk Embedding Evaluation

Bulk embedding runtime is not implemented in Stage 4.

bulk embedding must stay behind EmbeddingClient abstraction。后续如果 ingestion 规模需要批量 embedding，应优先扩展
`EmbeddingClient` contract 或增加批量 adapter 方法，而不是让 ingestion pipeline 直接依赖 Spring AI
`EmbeddingModel`。默认测试仍必须使用 fake provider 或 in-memory dependencies。

未来 bulk embedding 设计还需要：

- batch size limit；
- provider rate limit / retry strategy；
- partial failure handling；
- idempotent ingestion run state；
- sanitized provider errors；
- opt-in live validation。

## Provider Governance Relationship

Spring AI 深化不能替代 provider governance。OpenAI / DashScope / Spring AI provider 仍必须遵守：

- provider configuration 不进入默认测试；
- API Key 只通过环境变量或本地未提交配置提供；
- provider errors 需要脱敏；
- live tests 必须显式 opt-in；
- 默认验证不依赖真实 provider。

## Agent Boundary

Agent 只接收结构化 plan 或检索结果。Planner 可以规划 Tool / Skill，但不得执行 Tool / Skill。Spring AI 输出仍必须
通过项目拥有的解析和校验边界，不能直接成为 runtime action。

## ToolRegistry / Approval Boundary

ToolRegistry remains tool execution boundary. 所有项目工具调用仍必须通过 `ToolRegistry`，高风险动作仍必须进入
Approval。`search_aftersale_policy` 继续是 LOW-risk read-only ToolRegistry tool，只返回 policy evidence。

## RAG / Ingestion Boundary

RAG retrieval 是 policy evidence source，不是业务动作执行。Policy ingestion 仍是 admin/offline pipeline，不是
Agent runtime tool。Spring AI Advisors 或 bulk embedding 后续只能增强受控 pipeline，不得新增 public RAG HTTP endpoint
或默认 live provider path。

## Default Offline Boundary

本阶段只修改文档和 docs harness tests。默认验证仍不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、
Redis、real embedding provider、Spring AI live provider calls、Spring AI VectorStore 或 external network。

## Security / Secret Safety

文档和测试不得包含真实 API key、数据库密码、token、本地绝对路径、raw prompt、raw dataset path 或 provider 私有配置。
示例只能使用 placeholder。

## Non-goals

- 不实现 ChatMemory runtime；
- 不实现 Advisors runtime；
- 不启用 Spring AI Tool Calling API；
- 不实现 bulk embedding runtime；
- 不修改 `SpringAiLlmClient`；
- 不修改 `SpringAiEmbeddingClient`；
- 不修改 `LlmClient` 或 `EmbeddingClient` contract；
- 不修改 `AgentPlanParser` 或 `AgentPlanValidator`；
- 不修改 ToolRegistry、Approval、ToolCallTrace、Workspace、Execution Tree、RAG runtime 或 ingestion pipeline；
- 不新增 public RAG HTTP endpoint；
- 不调用真实 provider。

## Alternatives Considered

1. 立即启用 Spring AI Tool Calling API。
   放弃原因：会削弱 ToolRegistry / Approval / Trace 的强边界。
2. 立即使用 ChatMemory 管理 Agent 上下文。
   放弃原因：当前 `AgentWorkspace` 已定义单次 AgentRun 工作记忆，长期记忆需要单独设计。
3. 直接在 ingestion pipeline 中调用 Spring AI batch embedding。
   放弃原因：会绕过 `EmbeddingClient` abstraction 和默认离线 fake path。

## Consequences

Positive:

- Spring AI 深化方向被明确记录；
- 当前 adapter foundation 口径更准确；
- ToolRegistry、Approval、Trace 和默认离线边界继续稳定；
- 后续实现可以按风险拆分，不把实验能力写成已完成 runtime。

Costs:

- 本阶段不提升运行时 Spring AI 能力；
- ChatMemory、Advisors、Tool Calling API 和 bulk embedding 仍需要后续独立设计、实现和测试。

## Follow-ups

- Stage 5: RAG retrieval quality evaluation and possible runtime improvements；
- Future Spring AI implementation task: ChatMemory adapter experiment, if a concrete use case exists；
- Future Spring AI implementation task: Advisor experiment, only behind project RAG and ToolRegistry boundaries；
- Future ingestion task: bulk embedding contract extension behind `EmbeddingClient`；
- Future architecture task: keep Spring AI classes out of Agent / Handler / Skill runtime layers。

## Completion Signal

TASK_COMPLETE
