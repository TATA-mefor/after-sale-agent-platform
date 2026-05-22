
# Decision: V4 Spring AI Adapter

Date: 2026-05-22
Status: Accepted

## Context

AfterSale-Agent 现有 LLM provider 边界已经支持 OpenAI-compatible Responses、DashScope Responses compatible 和 DashScope Chat Completions compatible providers。V4 面试目标要求项目体现 Java / Spring Boot AI 工程能力，因此需要引入 Spring AI。

项目不能为了接入 Spring AI 而破坏既有原则：LLM 只规划，Java 后端校验和执行，ToolRegistry 是唯一工具执行入口，高风险动作进入 Approval，默认测试离线确定性运行。

## Decision

V4 引入 Spring AI adapter，而不是用 Spring AI 替代整个 Agent 架构。

Spring AI 只作为 provider abstraction 的一个实现：

```text
LlmAgentPlanner
→ LlmClient
→ SpringAiLlmClient
→ Spring AI ChatClient
```

Embedding 能力通过独立 embedding client 边界进入 RAG：

```text
PolicyEmbeddingService
→ EmbeddingClient
→ SpringAiEmbeddingClient
→ Spring AI EmbeddingModel
```

## Configuration

推荐配置：

```yaml
agent:
  spring-ai:
    enabled: false
    chat-enabled: false
    embedding-enabled: false
  planner:
    llm:
      provider: spring-ai-chat
```

默认 profile 不创建真实 Spring AI provider。真实 provider 只能在显式 live profile 或 opt-in test 中使用。
Spring AI model auto-configuration is disabled by default with `spring.ai.model.*=none` until a local operator enables
the relevant model type.

## Boundaries

Spring AI ChatClient 不得注入：

- Controller；
- AgentSkill；
- SpecialistAgentHandler；
- ToolExecutor；
- Repository；
- domain model。

Spring AI EmbeddingModel 不得注入：

- AgentApplicationService；
- AgentSkill；
- SpecialistAgentHandler；
- ToolExecutor；
- Controller；
- domain model。

Provider 输出仍必须经过 AgentPlanParser 和 AgentPlanValidator。

## Test Strategy

默认测试使用 fake LLM client 和 fake embedding client。

Live tests 必须显式 opt-in：

```bash
mvn test -Dtest=SpringAiLlmClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.llm=true
mvn test -Dtest=SpringAiEmbeddingClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.embedding=true
```

缺少 API Key 或 provider configuration 时，live tests 必须 skip 或给出清晰 setup error，不得污染默认测试。

## Consequences

Positive:

- 项目具备 Spring AI 生态接入能力；
- 现有 LLM provider 边界可复用；
- RAG embedding 能力有标准 Java / Spring 接入路径；
- 默认测试仍然保持离线确定性。

Costs:

- 增加 Spring AI 依赖和配置复杂度；
- 需要 provider-aware error sanitization；
- 需要确保 ChatClient / EmbeddingModel 不泄露到 Agent 和 Skill 层。

## Non-goals

- 不把 Spring AI Tool Calling 作为默认执行路径；
- 不让 LLM 直接执行工具；
- 不让 Spring AI 替代 ToolRegistry、Approval、Trace 或 Workspace；
- 不删除既有 OpenAI / DashScope provider；
- 不让默认 test gate 依赖真实 provider。
