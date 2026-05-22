# LLM_PLANNER_CONTRACT.md

# LLM Planner 契约

## 1. 目标

本文件定义 AfterSale-Agent V2.1 中 LLM Planner 的职责、输入、输出、安全边界和测试策略。

核心原则：

> LLM 负责规划，Java 后端负责执行。

LLM 不得直接调用业务工具，不得直接修改业务状态，不得绕过 ToolRegistry。

## 2. AgentPlanner 职责

`AgentPlanner` 是 Agent 规划能力的抽象接口。

推荐接口形态：

```java
public interface AgentPlanner {
    AgentPlan plan(AgentPlanningContext context);
}
```

`AgentApplicationService` 只依赖该接口，不依赖具体实现。

## 3. Planner 实现

### 3.1 RuleBasedAgentPlanner

规则型 Planner。

用途：

- 保留 V1 行为；
- 本地默认运行；
- 无需 API Key；
- 保证测试确定性；
- 作为 LLM 不可用时的降级路径。

### 3.2 LlmAgentPlanner

真实 LLM Planner。

用途：

- 调用外部 LLM；
- 根据用户售后问题生成结构化 AgentPlan；
- 只负责规划，不执行工具；
- 不直接修改 Ticket、AgentRun、ToolCallTrace。

当前 V2.1 实现状态：

- 已提供 `LlmAgentPlanner` adapter 边界；
- 已提供 planner mode 配置读取；
- 已提供缺少 API Key 时的清晰错误；
- 已接入轻量 OpenAI-compatible Responses provider client；
- 已实现结构化 AgentPlan JSON 解析；
- 已实现 AgentPlan 校验。

`LlmAgentPlanner` 不得伪造真实调用成功。默认测试仍不得访问真实网络或依赖 API Key。

### 3.3 FakeAgentPlanner

测试 Planner。

用途：

- 构造固定 AgentPlan；
- 测试 AgentApplicationService 与 ToolRegistry 协作；
- 不依赖真实 LLM、API Key、网络；
- 可模拟合法计划、非法计划、LLM 失败等场景。

## 4. AgentPlanningContext

`AgentPlanningContext` 是 Planner 输入。

至少包含：

```text
ticketId
userId
orderId
rawUserMessage
currentTicketStatus
availableTools
riskPolicySummary
knownPoliciesSummary optional
createdAt
```

说明：

- `availableTools` 用于让 LLM 知道当前可以规划哪些工具；
- `riskPolicySummary` 用于提醒 LLM 高风险动作边界；
- `knownPoliciesSummary` 可选，V2.1 可以不预加载政策全文；
- LLM 不应该收到敏感密钥、数据库连接、内部异常堆栈。

## 5. AgentPlan

`AgentPlan` 是 Planner 输出。

至少包含：

```text
intent
riskLevel
policyQuery
noteToAdd
finalSuggestion
evidenceHints
plannedTools
```

推荐结构：

```json
{
  "intent": "RETURN_AND_REFUND",
  "riskLevel": "MEDIUM",
  "policyQuery": "质量问题 退货 退款",
  "noteToAdd": "用户反馈耳机左耳无声，建议根据质量问题退换货规则进入人工审核。",
  "finalSuggestion": "该问题疑似质量问题，建议用户提供故障凭证后进入退货退款审核流程。",
  "evidenceHints": [
    "用户描述：耳机用了两天左耳没声音",
    "需检索质量问题退换货规则"
  ],
  "plannedTools": [
    {
      "toolName": "search_aftersale_policy",
      "reason": "检索质量问题退换货规则"
    },
    {
      "toolName": "add_ticket_note",
      "reason": "写入 Agent 处理建议"
    }
  ]
}
```

LLM 原始输出必须是一个 JSON object，不得包裹 Markdown 代码块，不得附加自然语言解释。示例：

```json
{
  "intent": "RETURN_AND_REFUND",
  "riskLevel": "MEDIUM",
  "policyQuery": "质量问题 退货 退款",
  "noteToAdd": "用户反馈商品存在质量问题，建议根据退货退款政策进入人工审核。",
  "finalSuggestion": "建议先检索质量问题退货退款政策，并提示用户补充故障凭证。",
  "evidenceHints": [
    "用户描述商品存在质量问题",
    "需要检索退货退款政策"
  ],
  "plannedTools": [
    {
      "toolName": "search_aftersale_policy",
      "reason": "检索售后政策"
    },
    {
      "toolName": "add_ticket_note",
      "reason": "写入处理建议"
    }
  ]
}
```

## 6. 字段要求

### 6.1 intent

必须是系统支持的枚举值：

```text
REFUND_ONLY
RETURN_AND_REFUND
EXCHANGE
REPAIR
LOGISTICS_ISSUE
GENERAL_CONSULTATION
UNKNOWN
```

### 6.2 riskLevel

必须是系统支持的风险等级：

```text
LOW
MEDIUM
HIGH
```

### 6.3 policyQuery

用于调用 `search_aftersale_policy`。

要求：

- 不为空；
- 不包含 API Key；
- 不包含数据库语句；
- 应该是面向售后政策检索的短文本。

### 6.4 noteToAdd

用于调用 `add_ticket_note`。

要求：

- 必须说明建议；
- 不得声称已完成真实退款；
- 不得声称已完成真实补偿；
- 不得绕过人工审核。

### 6.5 finalSuggestion

用于返回给业务系统或客服人员。

要求：

- 必须基于用户问题和工具结果；
- 不得把 LLM 猜测当作事实；
- 不得承诺高风险动作已经完成；
- 应该说明下一步建议。

### 6.6 evidenceHints

用于记录 Planner 的依据提示。

说明：

- 不是最终证据；
- 最终依据必须来自工具调用结果；
- 可以包含用户原始描述、规划理由、待检索方向。

### 6.7 plannedTools

只允许包含 ToolRegistry 中已注册的工具。

V2.2 支持的工具至少包括：

```text
get_order_by_id
get_user_orders
search_aftersale_policy
add_ticket_note
create_aftersale_ticket
update_ticket_status
```

Planner 不得生成未知工具名。

### 6.8 解析失败处理

以下情况必须返回清晰错误，并阻止后续工具执行：

- LLM 返回的内容不是合法 JSON；
- JSON 缺少必填字段；
- 字段类型不符合契约；
- `intent` 或 `riskLevel` 不是系统枚举；
- `policyQuery`、`noteToAdd`、`finalSuggestion` 为空；
- `plannedTools` 为空或包含未知工具；
- `noteToAdd` / `finalSuggestion` 声称已经完成退款、补偿、关闭争议等未执行事实。

不可接受行为：

- 抛出 `NullPointerException`；
- 静默忽略非法字段；
- 将非法 AgentPlan 降级为成功计划；
- 在解析失败后继续执行工具。

## 7. 执行边界

LLM 不得：

- 直接执行工具；
- 直接访问 Repository；
- 直接修改 Ticket；
- 直接修改 Order；
- 直接写 ToolCallTrace；
- 直接执行退款、补偿、关闭争议工单；
- 返回“已退款”“已补偿”等未执行事实；
- 生成未经过 ToolRegistry 的业务结果。

Java 后端必须：

- 校验 AgentPlan；
- 通过 ToolRegistry 执行工具；
- 对高风险动作进行审批拦截；
- 记录 ToolCallTrace；
- 在工具失败时记录错误；
- 保持 AgentRun 状态一致。

## 8. 缺少 API Key 时的行为

当 `agent.planner.mode=llm` 但缺少 API Key 或必要配置时：

可接受行为：

1. 应用启动失败，并给出清晰错误；
2. 或 LlmAgentPlanner 返回清晰失败结果；
3. 或明确降级到 RuleBasedAgentPlanner，并记录 warning。

不可接受行为：

- 静默返回伪造的 LLM 结果；
- 在测试中访问真实网络；
- 抛出难以理解的空指针异常；
- 要求开发者把 API Key 写入代码。

## 9. 测试策略

默认 `mvn test` 必须：

- 不依赖真实 LLM；
- 不依赖 API Key；
- 不依赖外部网络；
- 不产生真实费用；
- 使用 RuleBasedAgentPlanner 或 FakeAgentPlanner。

必须覆盖：

- RuleBasedAgentPlanner 生成 V1 等价计划；
- FakeAgentPlanner 驱动 AgentRun；
- AgentApplicationService 依赖 AgentPlanner 抽象；
- 非法 AgentPlan 被拒绝或返回清晰失败；
- LLM 配置缺失时有清晰行为；
- V1 demo 流程继续通过。

Live smoke test 只能显式 opt-in 运行，例如：

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

要求：

- 默认 `mvn test` 不运行真实 LLM；
- 缺少所选 provider 的 API Key 时跳过或给出清晰提示；
- 只验证 LLM provider 调用、AgentPlan 解析和校验；
- 不执行业务工具；
- 不创建或修改 Ticket、AgentRun、ToolCallTrace。

## 10. Prompt 管理

LLM Planner prompt 必须集中管理。

允许位置：

```text
src/main/java/com/example/aftersale/agent/prompt/
docs/agent/
```

禁止：

- 在 Controller 中拼接 Prompt；
- 在多个 Service 中散落 Prompt；
- 将 API Key 写入 Prompt；
- 将敏感内部信息发送给 LLM。

## 11. V2.3 Multi-Intent Planning 契约

V2.3 允许 `AgentPlan` 增加 `subtasks` 字段，用于表达复杂售后诉求的结构化拆解。没有 `subtasks` 时，计划仍按现有单意图 `AgentPlan` 契约处理；包含 `subtasks` 时，整体计划可视为 `MultiIntentAgentPlan`。

LLM 可以规划子任务，但不得执行子任务。Java 后端必须校验子任务结构，再通过现有执行边界顺序处理。

示例复杂售后问题：

```text
我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？
```

期望子任务类型：

```text
RETURN
EXCHANGE
COUPON_CONSULTATION
```

每个 subtask 至少包含：

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
```

推荐结构：

```json
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
}
```

子任务类型候选：

```text
RETURN
EXCHANGE
REFUND_ONLY
REPAIR
COUPON_CONSULTATION
LOGISTICS_ISSUE
GENERAL_CONSULTATION
HUMAN_ESCALATION
UNKNOWN
```

校验要求：

- `type` 必须是系统支持的 `SubtaskType`；
- `riskLevel` 必须是系统支持的风险等级；
- `priority` 必须可排序；
- `plannedTools` 只能包含 ToolRegistry 已注册工具，且工具名必须按现有 `plannedTools.toolName` 契约校验；
- `dependencies` 只能引用同一计划内已存在的 `subtaskId`；
- 不允许循环依赖；
- 子任务不能声明退款、换货、优惠券补偿、争议关闭等高风险动作已经完成；
- LLM 不得直接调用工具、直接修改 Ticket、AgentRun、ToolCallTrace 或 Repository。

执行边界：

- LLM 只生成 `MultiIntentAgentPlan`；
- Java 后端负责解析和校验；
- `AgentApplicationService` 负责按顺序执行子任务计划；
- ToolRegistry 仍然是唯一工具执行入口；
- ToolCallTrace 继续记录每个工具调用。

V2.3 不实现多 Agent 微服务、消息队列、并行执行、投票共识、完整优惠券系统、真实退款、真实换货、真实物流或真实支付。

当前实现说明：

- `AgentPlan` 已支持 `subtasks`；
- `RuleBasedAgentPlanner` 已支持退货、换货、优惠券咨询组合诉求的确定性拆解；
- `AgentApplicationService` 按 `priority` 顺序执行 subtasks；
- ToolCallTrace 暂不改模型，但工具 inputJson 会带上 `subtaskId`、`subtaskType`、`subtaskTarget`；
- Execution Tree 留到后续阶段；Specialist Handler 已在 V2.4 作为 Java 后端执行分发实现。

## 12. V2.4 Specialist Handler Planner 边界

V2.4 已引入 Specialist Agent Handler，但 LLM / Planner 契约不改变：

- LLM / Planner 仍然只生成结构化 `AgentPlan` 和 `subtasks`；
- LLM / Planner 不得直接选择 Java bean、调用 handler 或执行 handler；
- Java 后端负责校验 subtasks 并通过 `SpecialistAgentHandlerRegistry` 调度 handler；
- handler 内部工具调用仍必须通过 ToolRegistry；
- ToolCallTrace 继续记录 handler 内部工具调用；
- LLM / Planner 不得声明真实退款、真实换货、真实优惠券补偿、支付变更、物流变更或争议关闭已经完成。

## 13. V2.6 Agent Workspace Planner 边界

V2.6 计划引入 Agent Workspace / Structured Memory，但 LLM / Planner 契约不改变：

- LLM / Planner 仍然只生成结构化 `AgentPlan` 和 `subtasks`；
- LLM / Planner 不直接读写 workspace；
- Java 后端负责创建 workspace、传递给 handler、汇总 final summary；
- workspace 不能成为把完整上下文塞入 prompt 的替代借口；
- workspace 不得保存 API Key、敏感凭证、完整长 prompt 或 LLM 原始长文本；
- workspace 不得替代 ToolCallTrace；
- workspace 不得绕过 ToolRegistry。

## 14. 配置建议

推荐配置：

```yaml
agent:
  planner:
    mode: rule
    llm:
      provider: ${AFTERSALE_LLM_PROVIDER:openai-responses}
      model: ${AFTERSALE_LLM_MODEL:gpt-4.1-mini}
      api-key: ${OPENAI_API_KEY:}
      endpoint: ${OPENAI_RESPONSES_ENDPOINT:https://api.openai.com/v1/responses}
      timeout-seconds: 30
      dashscope:
        api-key: ${DASHSCOPE_API_KEY:}
        base-url: ${DASHSCOPE_BASE_URL:https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1}
        responses-endpoint: ${DASHSCOPE_RESPONSES_ENDPOINT:}
        chat-completions-endpoint: ${DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT:}
```

支持的 provider：

- `openai-responses`：OpenAI Responses API，使用 `OPENAI_API_KEY`；
- `dashscope-responses`：DashScope Responses-compatible endpoint，使用 `DASHSCOPE_API_KEY`；
- `dashscope-chat-compatible`：DashScope OpenAI-compatible Chat Completions endpoint，使用 `DASHSCOPE_API_KEY`。

`openai` 作为旧配置值可以继续映射到 `openai-responses`，但新配置应使用显式 provider 名称。Chat
Completions compatible provider 需要把 planner 的 system/user prompt 转换为 `messages`，并只把
`choices[0].message.content` 统一回填为 `LlmResponse` 文本。所有 provider 的输出仍必须经过
`AgentPlanParser` 和 `AgentPlanValidator`。

测试环境建议：

```yaml
agent:
  planner:
    mode: fake
```

默认模式必须是 `rule`，以保证本地启动和 `mvn test` 不依赖真实 LLM、API Key 或外部网络。

## 15. 成功标准

V2.1 成功的标志：

- 可以配置启用真实 LLM Planner；
- 默认测试仍然离线通过；
- LLM 只生成 AgentPlan；
- ToolRegistry 仍然执行所有工具；
- ToolCallTrace 仍然记录所有工具调用；
- 高风险动作仍然不能被 LLM 直接执行；
- V1 demo 没有被破坏。

## V4 plannedSkills Extension

V4 reserves a backward-compatible extension for AgentPlan / AgentSubtask to include `plannedSkills` in addition to
`plannedTools`.

Example:

```json
{
  "intent": "RETURN_AND_REFUND",
  "riskLevel": "MEDIUM",
  "subtasks": [
    {
      "subtaskId": "S1",
      "type": "RETURN",
      "target": "耳机",
      "riskLevel": "MEDIUM",
      "policyQuery": "质量问题 退货退款",
      "plannedSkills": [
        {
          "skillName": "ReturnEligibilityAssessmentSkill",
          "reason": "基于订单事实和政策证据判断退货退款建议"
        }
      ],
      "plannedTools": [
        {
          "toolName": "get_order_by_id",
          "reason": "读取订单事实"
        },
        {
          "toolName": "search_aftersale_policy",
          "reason": "检索 RAG 政策证据"
        }
      ]
    }
  ]
}
```

Rules:

1. LLM may plan Skill names, but must not execute Skills.
2. `plannedSkills` must be validated against SkillRegistry.
3. `plannedTools` must be validated against ToolRegistry.
4. Skill execution remains Java backend responsibility.
5. Tool execution remains ToolRegistry responsibility.
6. RAG retrieval remains evidence acquisition, not business execution.
7. HIGH-risk planned Skill must route to Approval boundary.
8. Default tests must not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, or external network.

V4.1 implementation boundary:

- Skill contracts and `SkillRegistry` exist.
- Current Planner output should continue to use `plannedTools` and `subtasks`.
- `plannedSkills` is not parsed, validated, generated by default, or executed in V4.1.
- Enabling `plannedSkills` requires a later backward-compatible parser/validator/runtime change.

## V4 Spring AI Boundary

Spring AI ChatClient may be used behind LlmClient adapter. Spring AI EmbeddingModel may be used behind EmbeddingClient adapter.

Neither ChatClient nor EmbeddingModel may be injected into Planner business logic, Skill, Handler, ToolExecutor, Controller, Repository, or domain model directly.

V4.2 implementation boundary:

- `provider=spring-ai-chat` selects `SpringAiLlmClient`.
- Spring AI chat output is plain planner text and must still pass through `AgentPlanParser` and `AgentPlanValidator`.
- Spring AI tool/function calling is not registered with project tools.
- `ToolRegistry` remains the only tool execution entry point.
- Default tests keep Spring AI disabled and must not require API keys, provider network access, databases, Docker, Redis,
  or external services.
