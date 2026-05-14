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

V2.1 支持的工具至少包括：

```text
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

## 11. 配置建议

推荐配置：

```yaml
agent:
  planner:
    mode: rule
    llm:
      provider: openai
      model: ${AFTERSALE_LLM_MODEL:gpt-4o-mini}
      api-key: ${OPENAI_API_KEY:}
      endpoint: ${OPENAI_RESPONSES_ENDPOINT:https://api.openai.com/v1/responses}
      timeout-seconds: 30
```

测试环境建议：

```yaml
agent:
  planner:
    mode: fake
```

默认模式必须是 `rule`，以保证本地启动和 `mvn test` 不依赖真实 LLM、API Key 或外部网络。

## 12. 成功标准

V2.1 成功的标志：

- 可以配置启用真实 LLM Planner；
- 默认测试仍然离线通过；
- LLM 只生成 AgentPlan；
- ToolRegistry 仍然执行所有工具；
- ToolCallTrace 仍然记录所有工具调用；
- 高风险动作仍然不能被 LLM 直接执行；
- V1 demo 没有被破坏。
